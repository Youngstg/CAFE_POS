@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.coffeeos.erp.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.repo.PoJson
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.components.StockBadge
import com.coffeeos.erp.ui.theme.status

/**
 * Gudang (design.md §8.5): Stock Row (nama + progress bar + angka + badge),
 * WARNING/STOP di atas, mode lihat/ubah terpisah, terima PO dengan pratinjau
 * detail sebelum konfirmasi.
 */
@Composable
fun InventoryScreen(
    outletId: String,
    actorId: String,
    vm: InventoryViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val ingredients by remember(outletId) { vm.ingredients(outletId) }.collectAsState()
    val poPreview by vm.poPreview.collectAsState()
    var poId by remember { mutableStateOf("") }
    var editMode by remember { mutableStateOf(false) }
    var criticalFirst by remember { mutableStateOf(true) }

    val shown = remember(ingredients, criticalFirst) {
        if (!criticalFirst) ingredients
        else ingredients.sortedWith(
            compareByDescending<com.coffeeos.erp.core.data.local.IngredientEntity> { it.isStopped }
                .thenByDescending { it.isLow }
        )
    }
    val ingNames = ingredients.associateBy({ it.id }, { it.name })

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Gudang — Stok & Opname", style = MaterialTheme.typography.titleLarge)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = criticalFirst,
                onClick = { criticalFirst = !criticalFirst },
                label = { Text("Kritis dulu") }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mode ubah", style = MaterialTheme.typography.bodySmall)
                Switch(checked = editMode, onCheckedChange = { editMode = it })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = poId,
                onValueChange = { poId = it; vm.previewPo(it) },
                label = { Text("ID PO diterima") }, modifier = Modifier.weight(1f), singleLine = true
            )
            Button(onClick = { vm.receivePo(poId.trim(), actorId) }) { Text("Terima") }
        }
        poPreview?.let { po ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${po.id} • ${po.status} • Rp${po.total}", fontWeight = FontWeight.Medium)
                    PoJson.parse(po.itemsJson).forEach { (id, qty) ->
                        Text("${ingNames[id] ?: id} — $qty", style = MaterialTheme.typography.bodySmall)
                    }
                    if (po.status != "APPROVED") {
                        Text(
                            "Hanya PO APPROVED yang bisa diterima.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.status.warning
                        )
                    }
                }
            }
        }
        ui.message?.let { Text(it) }
        if (shown.isEmpty()) {
            EmptyState(glyph = "📦", title = "Belum ada bahan", hint = "Tambah bahan via layar Katalog.")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.id }) { ing ->
                var fisik by remember(ing.id, ing.currentStock) {
                    mutableStateOf(ing.currentStock.toString())
                }
                var kapasitas by remember(ing.id, ing.maxCapacity) {
                    mutableStateOf(ing.maxCapacity?.toString() ?: "")
                }
                val pct = ing.maxCapacity?.takeIf { it > 0 }?.let { ing.currentStock / it }
                Card(
                    modifier = Modifier.fillMaxWidth().animateItemPlacement(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            ing.isStopped -> MaterialTheme.status.stopContainer
                            ing.isLow -> MaterialTheme.status.warningContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ing.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            StockBadge(ing.isStopped, ing.isLow)
                        }
                        if (pct != null) {
                            LinearProgressIndicator(
                                progress = { pct.toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = when {
                                    ing.isStopped -> MaterialTheme.status.stop
                                    ing.isLow -> MaterialTheme.status.warning
                                    else -> MaterialTheme.status.safe
                                }
                            )
                        }
                        Text(
                            "${ing.currentStock} ${ing.unit} / kap ${ing.maxCapacity ?: "-"} ${ing.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (editMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = fisik, onValueChange = { fisik = it },
                                    label = { Text("Stok fisik") }, modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                Button(onClick = {
                                    vm.opname(ing.id, fisik.toDoubleOrNull() ?: ing.currentStock, actorId)
                                }) { Text("Opname") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = kapasitas, onValueChange = { kapasitas = it },
                                    label = { Text("Kapasitas max") }, modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                Button(onClick = {
                                    vm.updateCapacity(
                                        ing.id,
                                        kapasitas.toDoubleOrNull() ?: ing.maxCapacity ?: 0.0,
                                        ing.unit
                                    )
                                }) { Text("Simpan") }
                            }
                        }
                    }
                }
            }
        }
    }
}

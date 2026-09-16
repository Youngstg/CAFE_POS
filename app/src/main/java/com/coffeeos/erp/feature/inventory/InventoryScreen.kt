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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Gudang: daftar stok (kuning ≤10%, merah ≤2%/STOP) + opname + terima PO. */
@Composable
fun InventoryScreen(
    outletId: String,
    actorId: String,
    vm: InventoryViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val ingredients by vm.ingredients(outletId).collectAsState()
    var poId by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Gudang — Stok & Opname", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = poId, onValueChange = { poId = it },
                label = { Text("ID PO diterima") }, modifier = Modifier.weight(1f), singleLine = true
            )
            Button(onClick = { vm.receivePo(poId.trim(), actorId) }) { Text("Terima") }
        }
        ui.message?.let { Text(it) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ingredients) { ing ->
                var fisik by remember(ing.id, ing.currentStock) {
                    mutableStateOf(ing.currentStock.toString())
                }
                var kapasitas by remember(ing.id, ing.maxCapacity) {
                    mutableStateOf(ing.maxCapacity?.toString() ?: "")
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            ing.isStopped -> MaterialTheme.colorScheme.errorContainer
                            ing.isLow -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${ing.name} — ${ing.currentStock} ${ing.unit}")
                        Text(
                            when {
                                ing.isStopped -> "STOP ≤2% — menu terkait mati"
                                ing.isLow -> "WARNING ≤10% — segera bikin PO"
                                else -> "Aman"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Acuan 10%/2% dari kapasitas: ${ing.maxCapacity ?: "-"} ${ing.unit}",
                            style = MaterialTheme.typography.bodySmall
                        )
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

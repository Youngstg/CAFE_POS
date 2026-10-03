@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.coffeeos.erp.feature.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.coffeeos.erp.core.data.local.IngredientEntity
import com.coffeeos.erp.core.data.local.PurchaseOrderEntity
import com.coffeeos.erp.core.util.toRupiah
import com.coffeeos.erp.feature.supply.SupplyViewModel
import com.coffeeos.erp.ui.components.BadgeKind
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.components.StatusBadge
import com.coffeeos.erp.ui.components.animateItem
import com.coffeeos.erp.ui.theme.EnergyOrange
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme
import com.coffeeos.erp.ui.theme.status

/**
 * Inventory/Gudang: Stok bahan + terima PO.
 * Perbaikan:
 * - Terima PO via dropdown (bukan input ID manual)
 * - Edit kapasitas via dialog konfirmasi terpisah (bukan toggle switch rawan mis-tap)
 * - Progress bar stok visual dengan warna semantik
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    outletId: String,
    uid: String,
    vm: InventoryViewModel = hiltViewModel(),
    supplyVm: SupplyViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val ingredients by remember(outletId) { vm.ingredients(outletId) }.collectAsState()
    val pos by remember(outletId) { supplyVm.pos(outletId) }.collectAsState()
    val poPreview by vm.poPreview.collectAsState()

    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Stok Bahan", "Terima PO", "Tambah Bahan")

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(label) })
            }
        }

        ui.message?.let { msg ->
            StatusBadge(
                kind = if (msg.contains("tersimpan") || msg.contains("bertambah") || msg.contains("diperbarui")) BadgeKind.SAFE else BadgeKind.WARNING,
                text = msg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        when (tabIndex) {
            0 -> StockListTab(
                ingredients = ingredients,
                onUpdateCapacity = { ingId, cap, unit -> vm.updateCapacity(ingId, cap, unit) },
                onOpname = { ingId, physical -> vm.opname(ingId, physical, uid) }
            )
            1 -> ReceivePoTab(
                pos = pos.filter { it.status == "APPROVED" },
                poPreview = poPreview,
                onPreview = { vm.previewPo(it) },
                onReceive = { vm.receivePo(it, uid) }
            )
            else -> AddIngredientTab(
                onAdd = { name, stock, max, unit -> vm.addIngredient(outletId, name, stock, max, unit) }
            )
        }
    }
}

@Composable
private fun StockListTab(
    ingredients: List<IngredientEntity>,
    onUpdateCapacity: (String, Double, String) -> Unit,
    onOpname: (String, Double) -> Unit,
) {
    val lowStockCount = ingredients.count { it.isLow || it.isStopped }

    Column(Modifier.fillMaxSize()) {
        if (lowStockCount > 0) {
            Surface(
                color = SukopiTheme.colors.warningContainer,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SukopiTheme.colors.warning.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = SukopiTheme.colors.warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Peringatan: Ada $lowStockCount bahan baku dengan stok menipis / kritis!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SukopiTheme.colors.warning
                    )
                }
            }
        }

        if (ingredients.isEmpty()) {
            EmptyState(
                glyph = "📦",
                title = "Belum ada bahan",
                hint = "Tambah bahan di tab 'Tambah Bahan'."
            )
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ingredients, key = { it.id }) { ing ->
                IngredientCard(
                    ing = ing,
                    onUpdateCapacity = { cap, unit -> onUpdateCapacity(ing.id, cap, unit) },
                    onOpname = { phys -> onOpname(ing.id, phys) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun IngredientCard(
    ing: IngredientEntity,
    onUpdateCapacity: (Double, String) -> Unit,
    onOpname: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pct = if ((ing.maxCapacity ?: 0.0) > 0) (ing.currentStock / ing.maxCapacity!! * 100).toFloat().coerceIn(0f, 100f) else -1f
    val stockKind = when {
        ing.isStopped -> BadgeKind.STOP
        ing.isLow -> BadgeKind.WARNING
        else -> BadgeKind.SAFE
    }
    var showOpname by remember { mutableStateOf(false) }
    var showCapacity by remember { mutableStateOf(false) }
    var opnameText by remember { mutableStateOf("") }
    var capText by remember { mutableStateOf(ing.maxCapacity?.toString() ?: "") }
    var unitText by remember { mutableStateOf(ing.unit) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ing.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${ing.currentStock} / ${ing.maxCapacity ?: "?"} ${ing.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    kind = stockKind,
                    text = when {
                        ing.isStopped -> "STOP"
                        ing.isLow -> "WARNING"
                        else -> "OK"
                    }
                )
            }

            // Progress bar stok visual
            if (pct >= 0) {
                LinearProgressIndicator(
                    progress = { pct / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = when (stockKind) {
                        BadgeKind.STOP -> MaterialTheme.status.stop
                        BadgeKind.WARNING -> MaterialTheme.status.warning
                        else -> MaterialTheme.status.safe
                    }
                )
            }

            // Tombol aksi
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showOpname = !showOpname }) { Text(if (showOpname) "Batal" else "Stock Opname") }
                TextButton(onClick = { showCapacity = !showCapacity }) { Text(if (showCapacity) "Batal" else "Edit Kapasitas") }
            }

            // Form opname — expanded on demand
            AnimatedVisibility(showOpname) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = opnameText,
                        onValueChange = { opnameText = it },
                        label = { Text("Stok fisik sekarang (${ing.unit})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Button(
                        onClick = {
                            val v = opnameText.toDoubleOrNull() ?: return@Button
                            onOpname(v); opnameText = ""; showOpname = false
                        },
                        enabled = opnameText.toDoubleOrNull() != null,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) { Text("Simpan Opname", fontWeight = FontWeight.SemiBold) }
                }
            }

            // Form kapasitas — expanded on demand
            AnimatedVisibility(showCapacity) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = capText, onValueChange = { capText = it },
                            label = { Text("Kapasitas max") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = unitText, onValueChange = { unitText = it },
                            label = { Text("Satuan") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val cap = capText.toDoubleOrNull() ?: return@Button
                            onUpdateCapacity(cap, unitText); showCapacity = false
                        },
                        enabled = capText.toDoubleOrNull() != null && unitText.isNotBlank(),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) { Text("Simpan Kapasitas", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceivePoTab(
    pos: List<PurchaseOrderEntity>,
    poPreview: PurchaseOrderEntity?,
    onPreview: (String) -> Unit,
    onReceive: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedPo by remember { mutableStateOf<PurchaseOrderEntity?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Terima Purchase Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        if (pos.isEmpty()) {
            Text(
                "Belum ada PO yang disetujui (APPROVED).\nOwner perlu approve PO di menu Supply.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        // Dropdown PO — bukan input ID manual
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedPo?.id ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Pilih PO yang akan diterima") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                pos.forEach { po ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(po.id, fontWeight = FontWeight.Medium)
                                Text(po.total.toRupiah(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            selectedPo = po
                            onPreview(po.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Preview PO yang dipilih
        poPreview?.let { po ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Detail PO: ${po.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Total: ${po.total.toRupiah()}", style = MaterialTheme.typography.bodyMedium)
                    Text("Status: ${po.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = { onReceive(po.id); selectedPo = null },
                shape = PillShape,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange)
            ) {
                Text("✓ Terima & Tambah Stok", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AddIngredientTab(
    onAdd: (String, Double, Double?, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("0") }
    var capText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Tambah Bahan Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Bahan") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = stockText, onValueChange = { stockText = it },
                label = { Text("Stok Awal") }, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = unit, onValueChange = { unit = it },
                label = { Text("Satuan") }, modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = capText, onValueChange = { capText = it },
            label = { Text("Kapasitas Maks (opsional)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = { Text("Dipakai untuk kalkulasi ambang stok 10%/2%") }
        )
        Button(
            onClick = {
                val stock = stockText.toDoubleOrNull() ?: 0.0
                val cap = capText.toDoubleOrNull()
                onAdd(name.trim(), stock, cap, unit.trim().ifBlank { "pcs" })
                name = ""; stockText = "0"; capText = ""; unit = "kg"
            },
            enabled = name.isNotBlank() && unit.isNotBlank(),
            shape = PillShape,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange)
        ) { Text("Tambah Bahan", fontWeight = FontWeight.Bold) }
    }
}

@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.coffeeos.erp.feature.supply

import com.coffeeos.erp.ui.components.animateItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.coffeeos.erp.core.data.local.SupplierEntity
import com.coffeeos.erp.core.domain.supply.PoCalculation
import com.coffeeos.erp.core.util.toDateTimeString
import com.coffeeos.erp.core.util.toRupiah
import com.coffeeos.erp.feature.inventory.InventoryViewModel
import com.coffeeos.erp.ui.components.BadgeKind
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.components.StatusBadge
import com.coffeeos.erp.ui.theme.EnergyOrange
import com.coffeeos.erp.ui.theme.status

/**
 * Supply: Supplier + Purchase Order (DRAFT→APPROVED→RECEIVED).
 * Perbaikan:
 * - Hapus semua kode demo hardcode ("ing-susu", "Susu 10L")
 * - Form buat PO real: pilih supplier (dropdown) + tambah item bahan (dropdown + qty + harga)
 * - Daftar PO dengan status + total Rupiah
 * - Tab terpisah: Daftar PO | Buat PO | Supplier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyScreen(
    outletId: String,
    uid: String,
    isOwner: Boolean = false,
    vm: SupplyViewModel = hiltViewModel(),
    inventoryVm: InventoryViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val suppliers by remember(outletId) { vm.suppliers(outletId) }.collectAsState()
    val pos by remember(outletId) { vm.pos(outletId) }.collectAsState()
    val ingredients by remember(outletId) { inventoryVm.ingredients(outletId) }.collectAsState()

    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Daftar PO", "Buat PO", "Supplier")

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(label) })
            }
        }

        ui.message?.let { msg ->
            StatusBadge(
                kind = if (msg.contains("dibuat") || msg.contains("diterima") || msg.contains("APPROVED")) BadgeKind.SAFE else BadgeKind.WARNING,
                text = msg,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        when (tabIndex) {
            0 -> PoListTab(
                pos = pos,
                isOwner = isOwner,
                onApprove = { vm.approve(it) }
            )
            1 -> CreatePoTab(
                suppliers = suppliers,
                ingredients = ingredients,
                onSubmit = { suppId, items -> vm.createDraft(outletId, suppId, items, uid) }
            )
            else -> SupplierTab(
                suppliers = suppliers,
                onAdd = { name, phone, addr -> vm.addSupplier(outletId, name, phone, addr) }
            )
        }
    }
}

@Composable
private fun PoListTab(
    pos: List<PurchaseOrderEntity>,
    isOwner: Boolean,
    onApprove: (String) -> Unit,
) {
    if (pos.isEmpty()) {
        EmptyState(glyph = "📦", title = "Belum ada PO", hint = "Buat Purchase Order di tab 'Buat PO'.")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pos, key = { it.id }) { po ->
            PoCard(po = po, isOwner = isOwner, onApprove = { onApprove(po.id) })
        }
    }
}

@Composable
private fun PoCard(po: PurchaseOrderEntity, isOwner: Boolean, onApprove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().animateItem()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(po.id, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(po.createdAt.toDateTimeString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(
                    kind = when (po.status) {
                        "APPROVED" -> BadgeKind.SAFE
                        "RECEIVED" -> BadgeKind.INFO
                        else -> BadgeKind.WARNING
                    },
                    text = po.status
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(po.total.toRupiah(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            if (isOwner && po.status == "DRAFT") {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.status.safe)
                ) {
                    Text("✓ Approve PO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CreatePoTab(
    suppliers: List<SupplierEntity>,
    ingredients: List<IngredientEntity>,
    onSubmit: (supplierId: String, items: List<PoCalculation.PoItem>) -> Unit,
) {
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierDropdown by remember { mutableStateOf(false) }
    val poItems = remember { mutableStateListOf<PoItemDraft>() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Buat Purchase Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (suppliers.isEmpty()) {
            Text("Belum ada supplier. Tambahkan di tab 'Supplier' dulu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Pilih supplier via dropdown
            ExposedDropdownMenuBox(
                expanded = supplierDropdown,
                onExpandedChange = { supplierDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedSupplier?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Supplier") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(supplierDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = supplierDropdown, onDismissRequest = { supplierDropdown = false }) {
                    suppliers.forEach { sup ->
                        DropdownMenuItem(
                            text = { Text(sup.name) },
                            onClick = { selectedSupplier = sup; supplierDropdown = false }
                        )
                    }
                }
            }
        }

        HorizontalDivider()
        Text("Item Pesanan:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        // Daftar item yang sudah ditambahkan
        poItems.forEachIndexed { idx, item ->
            val ing = ingredients.firstOrNull { it.id == item.ingredientId }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ing?.name ?: item.ingredientId, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        Text("${item.qty} ${ing?.unit ?: ""} × ${item.unitCost.toRupiah()} = ${(item.qty * item.unitCost).toRupiah()}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { poItems.removeAt(idx) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus item", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Form tambah item
        AddPoItemForm(
            ingredients = ingredients,
            onAdd = { ingId, qty, price ->
                poItems.add(PoItemDraft(ingId, qty, price))
            }
        )

        val total = poItems.sumOf { it.qty.toLong() * it.unitCost }
        if (poItems.isNotEmpty()) {
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total PO:", fontWeight = FontWeight.Bold)
                Text(total.toRupiah(), fontWeight = FontWeight.ExtraBold, color = EnergyOrange)
            }
        }

        Button(
            onClick = {
                val suppId = selectedSupplier?.id ?: return@Button
                val items = poItems.map { PoCalculation.PoItem(it.ingredientId, it.qty, it.unitCost) }
                onSubmit(suppId, items)
                poItems.clear()
                selectedSupplier = null
            },
            enabled = selectedSupplier != null && poItems.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange)
        ) {
            Text("Buat Draft PO", fontWeight = FontWeight.Bold)
        }
    }
}

data class PoItemDraft(val ingredientId: String, val qty: Double, val unitCost: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPoItemForm(
    ingredients: List<IngredientEntity>,
    onAdd: (String, Double, Long) -> Unit,
) {
    var ingDropdown by remember { mutableStateOf(false) }
    var selectedIng by remember { mutableStateOf<IngredientEntity?>(null) }
    var qty by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tambah Bahan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            ExposedDropdownMenuBox(expanded = ingDropdown, onExpandedChange = { ingDropdown = it }) {
                OutlinedTextField(
                    value = selectedIng?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bahan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ingDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = ingDropdown, onDismissRequest = { ingDropdown = false }) {
                    ingredients.forEach { ing ->
                        DropdownMenuItem(
                            text = { Text("${ing.name} (${ing.unit})") },
                            onClick = { selectedIng = ing; ingDropdown = false }
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Qty (${selectedIng?.unit ?: "unit"})") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.all(Char::isDigit)) price = it },
                    label = { Text("Harga Satuan") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Button(
                onClick = {
                    val ingId = selectedIng?.id ?: return@Button
                    val qtyVal = qty.toDoubleOrNull()?.takeIf { it > 0 } ?: return@Button
                    val priceVal = price.toLongOrNull()?.takeIf { it > 0 } ?: return@Button
                    onAdd(ingId, qtyVal, priceVal)
                    qty = ""; price = ""
                },
                enabled = selectedIng != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Tambah ke PO")
            }
        }
    }
}

@Composable
private fun SupplierTab(
    suppliers: List<SupplierEntity>,
    onAdd: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tambah Supplier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Supplier") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. Telepon") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { if (name.isNotBlank()) { onAdd(name.trim(), phone.trim(), address.trim()); name = ""; phone = ""; address = "" } },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange)
        ) { Text("Tambah Supplier", fontWeight = FontWeight.Bold) }

        HorizontalDivider()
        Text("Daftar Supplier (${suppliers.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (suppliers.isEmpty()) {
            Text("Belum ada supplier.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(suppliers, key = { it.id }) { sup ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(sup.name, fontWeight = FontWeight.SemiBold)
                            if (sup.phone.isNotBlank()) Text(sup.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (sup.address.isNotBlank()) Text(sup.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

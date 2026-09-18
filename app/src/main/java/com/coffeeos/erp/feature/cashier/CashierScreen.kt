package com.coffeeos.erp.feature.cashier

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.data.repo.CartLine

/**
 * Terminal kasir sesuai design.md 8.2:
 * - Landscape/tablet: 2 kolom — kiri grid Menu Card, kanan panel Keranjang persisten.
 * - Portrait/ponsel: stack vertikal (menu di atas, keranjang di bawah).
 * Menu STOP tetap tampil non-aktif (kasir tahu kenapa tak bisa jual).
 */
@Composable
fun CashierScreen(
    outletId: String,
    cashierName: String,
    onOpenShift: () -> Unit = {},
    vm: CashierViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val menus by vm.menus(outletId).collectAsState()
    val hasShift by vm.hasShift.collectAsState()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var category by remember { mutableStateOf("Semua") }
    LaunchedEffect(outletId) { vm.refreshPending(); vm.checkShift(outletId) }

    // Kunci lembut bila shift belum dibuka (design.md §8.2): CTA, bukan error.
    if (hasShift == false) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("☕", style = MaterialTheme.typography.displayLarge)
            Text("Shift belum dibuka", style = MaterialTheme.typography.titleLarge)
            Text("Buka shift dulu sebelum jualan (catat modal awal).")
            Button(onClick = onOpenShift, modifier = Modifier.fillMaxWidth()) {
                Text("Buka Shift")
            }
        }
        return
    }

    val categories = remember(menus) { listOf("Semua") + menus.map { it.category }.distinct() }
    val visible = if (category == "Semua") menus else menus.filter { it.category == category }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryChips(categories = categories, selected = category, onSelect = { category = it })
        if (ui.pendingSync > 0) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Offline • ${ui.pendingSync} menunggu sync",
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { vm.syncNow() }) { Text("Sync sekarang") }
            }
        }
        if (landscape) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuGrid(
                    menus = visible,
                    onAdd = { vm.addToCart(it) },
                    modifier = Modifier.weight(2f).fillMaxHeight()
                )
                CartPanel(
                    outletId = outletId,
                    cashierName = cashierName,
                    vm = vm,
                    modifier = Modifier.width(360.dp).fillMaxHeight()
                )
            }
        } else {
            MenuGrid(
                menus = visible,
                onAdd = { vm.addToCart(it) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            CartPanel(
                outletId = outletId,
                cashierName = cashierName,
                vm = vm,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Filter kategori sebagai chip horizontal (design.md §8.2). */
@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat) }
            )
        }
    }
}

/** Grid Menu Card: tap besar, state habis overlay non-aktif. */
@Composable
private fun MenuGrid(
    menus: List<MenuEntity>,
    onAdd: (MenuEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(menus, key = { it.id }) { menu ->
            Card(
                enabled = menu.isAvailable,
                onClick = { onAdd(menu) },
                colors = CardDefaults.cardColors(
                    containerColor = if (menu.isAvailable) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(menu.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (menu.isAvailable) "Rp${menu.price}" else "HABIS (stok ≤2%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (menu.isAvailable) "＋ Tambah" else "✕",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/** Panel keranjang persisten: stepper besar, slot promo, tombol Bayar terjangkau. */
@Composable
private fun CartPanel(
    outletId: String,
    cashierName: String,
    vm: CashierViewModel,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Keranjang (${ui.cart.sumOf { it.qty }} item)",
            style = MaterialTheme.typography.titleMedium
        )
        LazyColumn(
            Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(ui.cart, key = { it.menuId }) { line ->
                CartLineRow(line = line, onMinus = { vm.decreaseFromCart(line.menuId) })
            }
        }
        PromoPicker(outletId = outletId, vm = vm)
        Text(
            "Total Rp${ui.cart.sumOf { it.qty * it.unitPrice }}",
            style = MaterialTheme.typography.titleLarge
        )
        ui.message?.let { Text(it) }
        ui.lastReceiptPath?.let { Text("Struk fake: $it", style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.clearCart() }) { Text("Bersihkan") }
            Button(
                onClick = { vm.pay(outletId, cashierName, "TUNAI") },
                enabled = ui.cart.isNotEmpty() && !ui.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (ui.busy) "Proses..." else "Bayar Tunai") }
        }
    }
}

/** Baris keranjang dengan stepper ＋/－ besar untuk tap cepat. */
@Composable
private fun CartLineRow(line: CartLine, onMinus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.name, style = MaterialTheme.typography.bodyLarge)
            Text("Rp${line.unitPrice} x${line.qty}", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onMinus) { Text("－") }
    }
}

/** Pilih 1 promo aktif (atau tanpa promo) sebelum bayar. */
@Composable
private fun PromoPicker(outletId: String, vm: CashierViewModel) {
    val promos by vm.promos(outletId).collectAsState()
    val selected by vm.selectedPromo.collectAsState()
    val active = promos.filter { it.active }
    if (active.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Promo:", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { vm.selectPromo(null) }) {
                Text(if (selected == null) "• Tanpa promo" else "Tanpa promo")
            }
            active.forEach { promo ->
                TextButton(onClick = { vm.selectPromo(promo) }) {
                    Text(if (selected?.id == promo.id) "• ${promo.name}" else promo.name)
                }
            }
        }
    }
}

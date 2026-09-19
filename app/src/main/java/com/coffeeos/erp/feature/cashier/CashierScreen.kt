package com.coffeeos.erp.feature.cashier

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.data.repo.CartLine
import com.coffeeos.erp.ui.components.MenuTile
import com.coffeeos.erp.ui.components.MiniKpi
import com.coffeeos.erp.ui.theme.EnergyOrange

/**
 * Terminal kasir Fase 1 Croizan (design.md §8.2):
 * - Landscape: 3 zona — rel kategori | grid + strip KPI | panel Bill.
 * - Portrait: stack (search + KPI geser + chips + grid + keranjang).
 * Foto produk menyusul Fase 2 (tile gradien + inisial untuk sekarang).
 */
@Composable
fun CashierScreen(
    outletId: String,
    cashierName: String,
    onOpenShift: () -> Unit = {},
    vm: CashierViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val menus by remember(outletId) { vm.menus(outletId) }.collectAsState()
    val hasShift by vm.hasShift.collectAsState()
    val kpi by vm.kpi.collectAsState()
    val query by vm.query.collectAsState()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var category by remember { mutableStateOf("Semua") }
    LaunchedEffect(outletId) { vm.refreshPending(); vm.checkShift(outletId); vm.loadKpi(outletId) }

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
    val visible = remember(menus, category, query) {
        menus.filter {
            (category == "Semua" || it.category == category) &&
                (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CashierHeader(
            cashierName = cashierName,
            query = query,
            onQuery = { vm.setQuery(it) },
            pendingSync = ui.pendingSync,
            onSync = { vm.syncNow() }
        )
        if (landscape) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryRail(
                    categories = categories,
                    selected = category,
                    onSelect = { category = it },
                    modifier = Modifier.width(120.dp).fillMaxHeight()
                )
                Column(Modifier.weight(2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    kpi?.let { KpiStrip(it, Modifier.fillMaxWidth()) }
                    MenuGrid(
                        menus = visible,
                        onAdd = { vm.addToCart(it) },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
                CartPanel(
                    outletId = outletId,
                    cashierName = cashierName,
                    vm = vm,
                    modifier = Modifier.width(380.dp).fillMaxHeight()
                )
            }
        } else {
            kpi?.let {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniKpi("Rp${it.revenue}", "Omzet shift", Modifier.width(140.dp))
                    MiniKpi("${it.openTickets}", "Tiket terbuka", Modifier.width(140.dp))
                    MiniKpi("${it.critical}", "Bahan STOP", Modifier.width(140.dp))
                }
            }
            CategoryChips(categories = categories, selected = category, onSelect = { category = it })
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

/** Header: nama kasir + search + badge sync (AppBar ringkas kasir). */
@Composable
private fun CashierHeader(
    cashierName: String,
    query: String,
    onQuery: (String) -> Unit,
    pendingSync: Int,
    onSync: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            cashierName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (pendingSync > 0) {
            TextButton(onClick = onSync) { Text("Offline • $pendingSync") }
        }
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        label = { Text("Cari menu...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Rel kategori vertikal (landscape): rapat, satu kolom teks. */
@Composable
private fun CategoryRail(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Kategori", style = MaterialTheme.typography.titleSmall)
        categories.forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Strip KPI: omzet shift + tiket terbuka + bahan STOP. */
@Composable
private fun KpiStrip(kpi: CashierViewModel.CashierKpi, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiniKpi("Rp${kpi.revenue}", "Omzet shift", Modifier.weight(1f))
        MiniKpi("${kpi.openTickets}", "Tiket terbuka", Modifier.weight(1f))
        MiniKpi("${kpi.critical}", "Bahan STOP", Modifier.weight(1f))
    }
}

/** Grid MenuTile besar berenergi. */
@Composable
private fun MenuGrid(
    menus: List<MenuEntity>,
    onAdd: (MenuEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(menus, key = { it.id }) { menu ->
            MenuTile(menu = menu, onAdd = { onAdd(menu) }, modifier = Modifier.animateItemPlacement())
        }
    }
}

/** Panel Bill persisten: stepper besar, slot promo, total + Checkout raksasa. */
@Composable
private fun CartPanel(
    outletId: String,
    cashierName: String,
    vm: CashierViewModel,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = EnergyOrange
        )
        ui.message?.let { Text(it) }
        ui.lastReceiptPath?.let { Text("Struk fake: $it", style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.clearCart() }) { Text("Bersihkan") }
            Button(
                onClick = { vm.pay(outletId, cashierName, "TUNAI") },
                enabled = ui.cart.isNotEmpty() && !ui.busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnergyOrange,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (ui.busy) "Proses..." else "Checkout • Bayar Tunai",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/** Baris bill dengan stepper ＋/－ besar untuk tap cepat. */
@Composable
private fun CartLineRow(line: CartLine, onMinus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("Rp${line.unitPrice} x${line.qty}", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onMinus) { Text("－", style = MaterialTheme.typography.titleMedium) }
    }
}

/** Pilih 1 promo aktif (atau tanpa promo) sebelum bayar. */
@Composable
private fun PromoPicker(outletId: String, vm: CashierViewModel) {
    val promos by remember(outletId) { vm.promos(outletId) }.collectAsState()
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

/** Filter kategori horizontal (portrait). */
@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { cat ->
            FilterChip(
                selected = selected == cat,
                onClick = { onSelect(cat) },
                label = { Text(cat) }
            )
        }
    }
}

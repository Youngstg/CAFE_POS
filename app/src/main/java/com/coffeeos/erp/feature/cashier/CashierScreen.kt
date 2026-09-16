package com.coffeeos.erp.feature.cashier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Terminal kasir: grid menu (mati saat stok STOP) + keranjang + bayar + struk fake. */
@Composable
fun CashierScreen(
    outletId: String,
    cashierName: String,
    vm: CashierViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val menus by vm.menus(outletId).collectAsState()
    LaunchedEffect(outletId) { vm.refreshPending() }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ui.pendingSync > 0) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Offline • ${ui.pendingSync} menunggu sync",
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { vm.syncNow() }) { Text("Sync sekarang") }
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(menus) { menu ->
                Card(
                    enabled = menu.isAvailable,
                    onClick = { vm.addToCart(menu) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (menu.isAvailable) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(menu.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (menu.isAvailable) "Rp${menu.price}" else "HABIS (stok ≤2%)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(if (menu.isAvailable) "＋ Tambah" else "✕")
                    }
                }
            }
        }
        Text("Keranjang (${ui.cart.sumOf { it.qty }} item)", style = MaterialTheme.typography.titleMedium)
        Text("Total Rp${ui.cart.sumOf { it.qty * it.unitPrice }}")
        ui.message?.let { Text(it) }
        ui.lastReceiptPath?.let { Text("Struk fake: $it", style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.clearCart() }) { Text("Bersihkan") }
            Button(
                onClick = { vm.pay(outletId, cashierName, "TUNAI") },
                enabled = ui.cart.isNotEmpty() && !ui.busy, modifier = Modifier.fillMaxWidth()
            ) { Text(if (ui.busy) "Proses..." else "Bayar Tunai") }
        }
    }
}

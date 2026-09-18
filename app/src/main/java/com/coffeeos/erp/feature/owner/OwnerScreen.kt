package com.coffeeos.erp.feature.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.ui.components.ConfirmDialog
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.components.StatCard
import com.coffeeos.erp.ui.theme.status

/**
 * Owner (design.md §8.8): grid Stat Card 2 kolom + konflik dengan tombol
 * berdampingan — Refund outline (netral) vs Paksa Lunas solid (tegas),
 * paksa lunas selalu konfirmasi (§11).
 */
@Composable
fun OwnerScreen(outletId: String, vm: OwnerViewModel = hiltViewModel()) {
    val dash by vm.dashboard.collectAsState()
    val msg by vm.message.collectAsState()
    val conflicts by vm.conflicts(outletId).collectAsState()
    var confirmForce by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(outletId) { vm.load(outletId) }

    confirmForce?.let { orderId ->
        ConfirmDialog(
            title = "Paksa lunas?",
            body = "Order $orderId dianggap lunas walau stok kalah rebutan. " +
                "Gunakan hanya jika selisih sudah dibereskan manual.",
            confirmLabel = "Paksa Lunas",
            onConfirm = { vm.resolve(orderId, refund = false); confirmForce = null },
            onDismiss = { confirmForce = null }
        )
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Owner Dashboard", style = MaterialTheme.typography.titleLarge)
        dash?.let { d ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { StatCard("Rp${d.revenuePaid}", "Omzet lunas (${d.ordersPaid})", MaterialTheme.status.safe) }
                item {
                    StatCard(
                        "${d.stoppedCount}", "Bahan STOP",
                        if (d.stoppedCount > 0) MaterialTheme.status.stop else MaterialTheme.status.safe
                    )
                }
                item {
                    StatCard(
                        "${d.lowCount}", "Bahan warning",
                        if (d.lowCount > 0) MaterialTheme.status.warning else MaterialTheme.status.safe
                    )
                }
                item {
                    StatCard(
                        "${d.conflictCount}", "Konflik",
                        if (d.conflictCount > 0) MaterialTheme.status.warning else MaterialTheme.status.safe
                    )
                }
                item { StatCard("${d.pendingSync}", "Pending sync", MaterialTheme.status.info) }
                item { StatCard(d.activeShiftId ?: "-", "Shift aktif", MaterialTheme.colorScheme.onSurface) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.syncNow(outletId) }) { Text("Sync sekarang") }
                Button(onClick = { vm.load(outletId) }) { Text("Refresh") }
            }
        }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Konflik butuh keputusan (${conflicts.size})", style = MaterialTheme.typography.titleMedium)
        if (conflicts.isEmpty()) {
            EmptyState(glyph = "☕", title = "Tidak ada konflik", hint = "Semua order sinkron rapi.")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(conflicts, key = { it.id }) { order ->
                Card(Modifier.fillMaxWidth().animateItemPlacement()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${order.id} • Rp${order.total} • stok kalah rebutan")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.resolve(order.id, refund = true) }) {
                                Text("Refund")
                            }
                            Button(onClick = { confirmForce = order.id }) { Text("Paksa Lunas") }
                        }
                    }
                }
            }
        }
    }
}

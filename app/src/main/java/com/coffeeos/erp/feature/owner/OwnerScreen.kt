package com.coffeeos.erp.feature.owner

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Owner: ringkasan + layar Konflik opsi B (refund / paksa lunas). */
@Composable
fun OwnerScreen(outletId: String, vm: OwnerViewModel = hiltViewModel()) {
    val dash by vm.dashboard.collectAsState()
    val msg by vm.message.collectAsState()
    val conflicts by vm.conflicts(outletId).collectAsState()
    LaunchedEffect(outletId) { vm.load(outletId) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Owner Dashboard", style = MaterialTheme.typography.titleLarge)
        dash?.let { d ->
            Text("Omzet LUNAS: Rp${d.revenuePaid} (${d.ordersPaid} order)")
            Text("Bahan warning: ${d.lowCount} • STOP: ${d.stoppedCount}")
            Text("Konflik: ${d.conflictCount} • Pending sync: ${d.pendingSync}")
            Text("Shift aktif: ${d.activeShiftId ?: "-"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.syncNow(outletId) }) { Text("Sync sekarang") }
                Button(onClick = { vm.load(outletId) }) { Text("Refresh") }
            }
        }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Konflik butuh keputusan (${conflicts.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(conflicts) { order ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${order.id} • Rp${order.total} • stok kalah rebutan")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.resolve(order.id, refund = true) }) { Text("Refund") }
                            Button(onClick = { vm.resolve(order.id, refund = false) }) { Text("Paksa Lunas") }
                        }
                    }
                }
            }
        }
    }
}

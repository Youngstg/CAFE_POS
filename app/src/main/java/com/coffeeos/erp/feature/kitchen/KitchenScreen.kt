package com.coffeeos.erp.feature.kitchen

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

/** KDS: antrean QUEUED/COOKING/READY + tombol status. Update masuk antrean sync. */
@Composable
fun KitchenScreen(outletId: String, vm: KitchenViewModel = hiltViewModel()) {
    val queue by vm.queue.collectAsState(initial = emptyList())
    val msg by vm.message.collectAsState()
    LaunchedEffect(outletId) { vm.track(outletId) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Kitchen Display — ${queue.size} antrean", style = MaterialTheme.typography.titleLarge)
        msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(queue) { order ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${order.id} • Rp${order.total} • ${order.status}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (order.status == "QUEUED") {
                                Button(onClick = { vm.setStatus(order.id, "COOKING") }) { Text("Masak") }
                            }
                            if (order.status == "COOKING") {
                                Button(onClick = { vm.setStatus(order.id, "READY") }) { Text("Siap") }
                            }
                        }
                    }
                }
            }
        }
    }
}

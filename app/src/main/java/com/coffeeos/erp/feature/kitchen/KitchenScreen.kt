@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.coffeeos.erp.feature.kitchen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.components.OrderBadge
import com.coffeeos.erp.ui.theme.status

/**
 * KDS kanban 3 kolom (design.md §8.4): QUEUED / COOKING / READY, fullscreen,
 * kolom scroll independen, tombol aksi besar, waktu tunggu relatif.
 * Update realtime antar HP via RealtimeSync (tanpa reload penuh).
 */
@Composable
fun KitchenScreen(outletId: String, vm: KitchenViewModel = hiltViewModel()) {
    val queue by vm.queue.collectAsState(initial = emptyList())
    val msg by vm.message.collectAsState()
    LaunchedEffect(outletId) { vm.track(outletId) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Kitchen Display — ${queue.size} antrean", style = MaterialTheme.typography.titleLarge)
        msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (queue.isEmpty()) {
            EmptyState(glyph = "☕", title = "Dapur bersih", hint = "Belum ada order masuk.")
        } else {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KanbanColumn(
                    title = "QUEUED",
                    orders = queue.filter { it.status == "QUEUED" },
                    accent = MaterialTheme.status.info,
                    onAction = { vm.setStatus(it, "COOKING") },
                    actionLabel = "Masak",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                KanbanColumn(
                    title = "COOKING",
                    orders = queue.filter { it.status == "COOKING" },
                    accent = MaterialTheme.status.warning,
                    onAction = { vm.setStatus(it, "READY") },
                    actionLabel = "Siap",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                KanbanColumn(
                    title = "READY",
                    orders = queue.filter { it.status == "READY" },
                    accent = MaterialTheme.status.safe,
                    onAction = null,
                    actionLabel = "",
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    title: String,
    orders: List<OrderEntity>,
    accent: androidx.compose.ui.graphics.Color,
    onAction: ((String) -> Unit)?,
    actionLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$title (${orders.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orders, key = { it.id }) { order ->
                Card(
                    border = BorderStroke(3.dp, accent),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().animateItemPlacement()
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.id, fontWeight = FontWeight.Bold)
                            OrderBadge(order.status)
                        }
                        Text("Rp${order.total}", style = MaterialTheme.typography.titleLarge)
                        Text(
                            relativeTime(order.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onAction != null) {
                            Button(
                                onClick = { onAction(order.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(actionLabel, style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                }
            }
        }
    }
}

/** Waktu tunggu relatif untuk urgensi visual ("5 mnt lalu"). */
fun relativeTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val mins = ((now - createdAt).coerceAtLeast(0) / 60_000).toInt()
    return when {
        mins < 1 -> "baru saja"
        mins < 60 -> "$mins mnt lalu"
        else -> "${mins / 60} jam ${mins % 60} mnt lalu"
    }
}

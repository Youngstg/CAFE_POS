@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.coffeeos.erp.feature.kitchen

import android.content.res.Configuration
import android.media.RingtoneManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.core.data.local.OrderItemEntity
import com.coffeeos.erp.core.util.toRupiah
import com.coffeeos.erp.ui.components.CustomerNotesBlock
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.components.OptionChip
import com.coffeeos.erp.ui.components.OrderBadge
import com.coffeeos.erp.ui.components.OrderTypeTag
import com.coffeeos.erp.ui.theme.Dimens
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme
import com.coffeeos.erp.ui.theme.status
import kotlinx.coroutines.delay

/**
 * Layar Order List & Barista KDS SuKopi POS (DESIGN.md §7.3 & §7.7).
 * Mendukung 2 mode tampilan:
 * 1. Master–Detail Order List (DESIGN.md §7.3): Kiri (~320dp) daftar order + Kanan detail pesanan.
 * 2. Barista Kanban Board (DESIGN.md §7.7): 3 kolom (New · Preparing · Ready) dengan timer urgensi.
 */
@Composable
fun KitchenScreen(outletId: String, vm: KitchenViewModel = hiltViewModel()) {
    val queue by vm.queue.collectAsState(initial = emptyList())
    val msg by vm.message.collectAsState()
    val itemsMap by vm.orderItemsMap.collectAsState()
    val soundEnabled by vm.soundEnabled.collectAsState()
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    // Mode tampilan: 0 = Master-Detail Order List, 1 = Barista Kanban 3-Kolom
    var viewMode by remember { mutableIntStateOf(0) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }

    // Audio alert saat ada pesanan baru masuk
    LaunchedEffect(Unit) {
        vm.newOrderAlert.collect {
            if (soundEnabled) {
                try {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, alertUri)
                    ringtone?.play()
                } catch (_: Exception) {}
            }
        }
    }

    // Ticker refresh waktu relatif setiap 30 detik
    var tickMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            tickMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(outletId) { vm.track(outletId) }

    // Sinkronisasi seleksi pertama saat queue terisi
    LaunchedEffect(queue) {
        if (selectedOrderId == null && queue.isNotEmpty()) {
            selectedOrderId = queue.first().id
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Baris Header: Judul + Switcher View Mode + Toggle Suara
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (viewMode == 0) "Order List" else "Barista Board",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kelola antrean dan progres pesanan secara realtime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SukopiTheme.colors.textSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Switcher View Mode (DESIGN.md Segmented Control)
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(Modifier.padding(2.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (viewMode == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { viewMode = 0 }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "📋 Order List",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (viewMode == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (viewMode == 0) MaterialTheme.colorScheme.primary else SukopiTheme.colors.textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (viewMode == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { viewMode = 1 }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🍱 Barista (3 Kolom)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (viewMode == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (viewMode == 1) MaterialTheme.colorScheme.primary else SukopiTheme.colors.textSecondary
                            )
                        }
                    }
                }

                // Toggle Suara Notifikasi
                FilterChip(
                    selected = soundEnabled,
                    onClick = { vm.toggleSound() },
                    shape = PillShape,
                    label = { Text(if (soundEnabled) "🔔 ON" else "🔕 Mute", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        msg?.let {
            Text(it, color = SukopiTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
        }

        if (queue.isEmpty()) {
            EmptyState(glyph = "☕", title = "Dapur bersih", hint = "Belum ada pesanan aktif saat ini.")
        } else if (viewMode == 0) {
            // Mode 1: Master-Detail Order List (DESIGN.md §7.3)
            OrderListMasterDetail(
                orders = queue,
                itemsMap = itemsMap,
                selectedOrderId = selectedOrderId,
                onSelectOrder = { selectedOrderId = it },
                onUpdateStatus = { id, status -> vm.setStatus(id, status) },
                now = tickMs,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            // Mode 2: Barista Kanban 3-Kolom (DESIGN.md §7.7)
            BaristaKanbanBoard(
                orders = queue,
                itemsMap = itemsMap,
                now = tickMs,
                onUpdateStatus = { id, status -> vm.setStatus(id, status) },
                isLandscape = isLandscape,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}

/**
 * Master-Detail Order List (DESIGN.md §7.3):
 * Kiri (~320dp): Tabs Active | Completed, lalu daftar kartu order.
 * Kanan (fleksibel): Satu kartu detail dengan status, tipe order, item, customer notes, ringkasan harga & tombol aksi.
 */
@Composable
private fun OrderListMasterDetail(
    orders: List<OrderEntity>,
    itemsMap: Map<String, List<OrderItemEntity>>,
    selectedOrderId: String?,
    onSelectOrder: (String) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    now: Long,
    modifier: Modifier = Modifier,
) {
    var tabIndex by remember { mutableIntStateOf(0) } // 0: Active, 1: Completed
    val activeOrders = orders.filter { it.status in listOf("QUEUED", "COOKING", "READY") }
    val completedOrders = orders.filter { it.status in listOf("PAID", "SERVED", "COMPLETED", "CANCELLED") }
    val displayOrders = if (tabIndex == 0) activeOrders else completedOrders

    val selectedOrder = orders.firstOrNull { it.id == selectedOrderId }
    val selectedItems = selectedOrder?.let { itemsMap[it.id] } ?: emptyList()

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Sisi Kiri (~320dp): Tabs + Daftar Kartu Order
        Column(
            Modifier
                .width(320.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tabs Pill (DESIGN.md §6 Tabs)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OptionChip(
                    label = "Active (${activeOrders.size})",
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    shape = PillShape,
                    modifier = Modifier.weight(1f)
                )
                OptionChip(
                    label = "Completed (${completedOrders.size})",
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    shape = PillShape,
                    modifier = Modifier.weight(1f)
                )
            }

            // Daftar Kartu Order
            if (displayOrders.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Tidak ada order",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SukopiTheme.colors.textTertiary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayOrders, key = { it.id }) { order ->
                        val isSelected = order.id == selectedOrderId
                        val orderItems = itemsMap[order.id] ?: emptyList()
                        val total = orderItems.sumOf { it.qty.toLong() * it.unitPrice }

                        Surface(
                            onClick = { onSelectOrder(order.id) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Baris 1: Nama kiri, Jam kanan
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (order.customerName.isNotBlank()) order.customerName else "#${order.id.takeLast(6)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = SukopiTheme.colors.textTertiary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = relativeTime(order.createdAt, now),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SukopiTheme.colors.textTertiary
                                        )
                                    }
                                }

                                // Baris 2: Tag Tipe Order
                                OrderTypeTag(type = order.orderType)

                                // Baris 3: Status Badge kiri, Total Harga kanan
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OrderBadge(status = order.status)
                                    Text(
                                        text = if (total > 0) total.toRupiah() else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sisi Kanan (Fleksibel): Satu Kartu Detail Pesanan
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            if (selectedOrder == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.ShoppingBag,
                        title = "Select an order to see details",
                        hint = "Pilih salah satu pesanan dari daftar di sebelah kiri."
                    )
                }
            } else {
                val total = selectedItems.sumOf { it.qty.toLong() * it.unitPrice }
                val waitMins = ((now - selectedOrder.createdAt).coerceAtLeast(0) / 60_000).toInt()
                val isUrgent = waitMins >= 15

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Detail: ID + Status Badge
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${selectedOrder.id}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        OrderBadge(status = selectedOrder.status)
                    }

                    // Baris Meta: Waktu + Tipe Order + Chip Sumber
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = if (isUrgent) SukopiTheme.colors.danger else SukopiTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${relativeTime(selectedOrder.createdAt, now)} (${waitMins}m)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUrgent) SukopiTheme.colors.danger else SukopiTheme.colors.textSecondary,
                                fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        OrderTypeTag(type = selectedOrder.orderType)

                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "via Kasir POS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SukopiTheme.colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Blok Pelanggan
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = if (selectedOrder.customerName.isNotBlank()) selectedOrder.customerName else "Tamu / Walk-in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Customer • Tipe: ${selectedOrder.orderType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SukopiTheme.colors.textSecondary
                            )
                        }
                    }

                    // Daftar Item Pesanan ("Order Items (X)")
                    Text(
                        text = "Order Items (${selectedItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedItems) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Thumbnail 40dp
                                    Box(
                                        Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.menuName.firstOrNull()?.uppercase() ?: "☕",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = item.menuName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (item.variant?.isNotBlank() == true) {
                                            Text(
                                                text = item.variant,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SukopiTheme.colors.textSecondary
                                            )
                                        }
                                        Text(
                                            text = (item.unitPrice * item.qty).toRupiah(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Qty x1 merah (danger, SemiBold) sesuai DESIGN.md §7.3
                                    Text(
                                        text = "×${item.qty}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SukopiTheme.colors.danger
                                    )
                                }
                            }
                        }
                    }

                    // Blok Catatan Pelanggan
                    val notes = selectedItems.mapNotNull { it.notes.ifBlank { null } }.joinToString(", ")
                    if (notes.isNotBlank()) {
                        CustomerNotesBlock(notes = notes)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // Footer Ringkasan Harga + Tombol Aksi
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Pembayaran", style = MaterialTheme.typography.bodySmall, color = SukopiTheme.colors.textSecondary)
                            Text(total.toRupiah(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        // Tombol Aksi Status
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (selectedOrder.status) {
                                "QUEUED" -> {
                                    Button(
                                        onClick = { onUpdateStatus(selectedOrder.id, "COOKING") },
                                        shape = PillShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("▶ Mulai Masak")
                                    }
                                }
                                "COOKING" -> {
                                    Button(
                                        onClick = { onUpdateStatus(selectedOrder.id, "READY") },
                                        shape = PillShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = SukopiTheme.colors.success)
                                    ) {
                                        Text("✓ Siap Disajikan")
                                    }
                                }
                                "READY" -> {
                                    Button(
                                        onClick = { onUpdateStatus(selectedOrder.id, "SERVED") },
                                        shape = PillShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("🍽 Selesaikan")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Barista Kanban Board 3-Kolom (DESIGN.md §7.7):
 * Tiga kolom (New · Preparing · Ready), header kolom dengan badge jumlah,
 * kartu tiket dengan nomor order besar, nama pelanggan, daftar item (+2sp), timer, catatan.
 */
@Composable
private fun BaristaKanbanBoard(
    orders: List<OrderEntity>,
    itemsMap: Map<String, List<OrderItemEntity>>,
    now: Long,
    onUpdateStatus: (String, String) -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    val queued = orders.filter { it.status == "QUEUED" }
    val cooking = orders.filter { it.status == "COOKING" }
    val ready = orders.filter { it.status == "READY" }

    if (isLandscape) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KanbanColumnView("New Order", queued, itemsMap, SukopiTheme.colors.info, now, { onUpdateStatus(it, "COOKING") }, "▶ Mulai Masak", MaterialTheme.colorScheme.primary, Modifier.weight(1f).fillMaxHeight())
            KanbanColumnView("Preparing", cooking, itemsMap, SukopiTheme.colors.warning, now, { onUpdateStatus(it, "READY") }, "✓ Siap Disajikan", SukopiTheme.colors.success, Modifier.weight(1f).fillMaxHeight())
            KanbanColumnView("Ready", ready, itemsMap, SukopiTheme.colors.success, now, { onUpdateStatus(it, "SERVED") }, "🍽 Diambil", MaterialTheme.colorScheme.primary, Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Row(
            modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KanbanColumnView("New Order", queued, itemsMap, SukopiTheme.colors.info, now, { onUpdateStatus(it, "COOKING") }, "▶ Mulai Masak", MaterialTheme.colorScheme.primary, Modifier.width(300.dp).fillMaxHeight())
            KanbanColumnView("Preparing", cooking, itemsMap, SukopiTheme.colors.warning, now, { onUpdateStatus(it, "READY") }, "✓ Siap Disajikan", SukopiTheme.colors.success, Modifier.width(300.dp).fillMaxHeight())
            KanbanColumnView("Ready", ready, itemsMap, SukopiTheme.colors.success, now, { onUpdateStatus(it, "SERVED") }, "🍽 Diambil", MaterialTheme.colorScheme.primary, Modifier.width(300.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun KanbanColumnView(
    title: String,
    orders: List<OrderEntity>,
    itemsMap: Map<String, List<OrderItemEntity>>,
    accent: Color,
    now: Long,
    onAction: (String) -> Unit,
    actionLabel: String,
    actionColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header kolom + badge jumlah
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = PillShape,
                    color = accent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${orders.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    val waitMins = ((now - order.createdAt).coerceAtLeast(0) / 60_000).toInt()
                    val isUrgent = waitMins >= 15
                    val items = itemsMap[order.id] ?: emptyList()

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isUrgent) SukopiTheme.colors.danger else MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Header tiket: nomor order besar + timer di pojok
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${order.id.takeLast(6)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${waitMins}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUrgent) SukopiTheme.colors.danger else SukopiTheme.colors.textSecondary
                                )
                            }

                            if (order.customerName.isNotBlank()) {
                                Text(
                                    text = "👤 ${order.customerName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OrderTypeTag(type = order.orderType)

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            // Daftar item (+2sp font per DESIGN.md §7.7)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items.forEach { item ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = item.menuName,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (!item.variant.isNullOrBlank()) {
                                                Text(
                                                    text = item.variant,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SukopiTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "×${item.qty}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SukopiTheme.colors.danger
                                        )
                                    }
                                }
                            }

                            // Blok catatan jika ada
                            val notes = items.mapNotNull { it.notes.ifBlank { null } }.joinToString(", ")
                            if (notes.isNotBlank()) {
                                CustomerNotesBlock(notes = notes)
                            }

                            // Tombol aksi status
                            Button(
                                onClick = { onAction(order.id) },
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Text(actionLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Waktu tunggu relatif ("5 mnt lalu"). */
fun relativeTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val mins = ((now - createdAt).coerceAtLeast(0) / 60_000).toInt()
    return when {
        mins < 1 -> "baru saja"
        mins < 60 -> "$mins mnt lalu"
        else -> "${mins / 60} jam ${mins % 60} mnt lalu"
    }
}

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.coffeeos.erp.feature.owner

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.TopSellingItem
import com.coffeeos.erp.core.data.repo.OwnerRepository
import com.coffeeos.erp.core.util.toRupiah
import androidx.compose.material3.Surface
import com.coffeeos.erp.ui.components.BadgeKind
import com.coffeeos.erp.ui.components.ConfirmDialog
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.components.FirebaseConnectionDialog
import com.coffeeos.erp.ui.components.StatusBadge
import com.coffeeos.erp.ui.components.animateItem
import com.coffeeos.erp.ui.theme.EnergyOrange
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme
import com.coffeeos.erp.ui.theme.status

private data class StatData(val value: String, val label: String, val accent: Color)

@Composable
private fun StatRow(a: StatData, b: StatData) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(value = a.value, label = a.label, accent = a.accent, modifier = Modifier.weight(1f))
        StatCard(value = b.value, label = b.label, accent = b.accent, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = accent)
        }
    }
}

/**
 * Owner Dashboard:
 * - Filter periode (Hari ini, 7 hari, 30 hari, Semua) terhubung langsung ke Room
 * - Diagram batang visual (Bar Chart) 7 hari terakhir
 * - Top 5 Menu Terlaris (Best Seller)
 * - Bagikan Rekap Laporan Penjualan via WhatsApp / Intent
 * - Penanganan konflik order semi-manual
 */
@Composable
fun OwnerScreen(outletId: String, vm: OwnerViewModel = hiltViewModel()) {
    val dash by vm.dashboard.collectAsState()
    val msg by vm.message.collectAsState()
    val period by vm.period.collectAsState()
    val conflicts by remember(outletId) { vm.conflicts(outletId) }.collectAsState()
    var confirmForce by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val healthState by vm.healthState.collectAsState()
    var showFirebaseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(outletId) { vm.load(outletId) }

    if (showFirebaseDialog) {
        FirebaseConnectionDialog(
            state = healthState,
            onTestClick = { vm.checkFirebaseHealth() },
            onSyncClick = { vm.syncNow(outletId) },
            onDismiss = { showFirebaseDialog = false }
        )
    }

    confirmForce?.let { orderId ->
        ConfirmDialog(
            title = "Paksa Lunas?",
            body = "Order $orderId akan ditandai LUNAS meskipun ada konflik stok.\n\n" +
                "Pastikan selisih stok sudah dibereskan manual sebelum konfirmasi.",
            confirmLabel = "Paksa Lunas",
            onConfirm = { vm.resolve(orderId, refund = false); confirmForce = null },
            onDismiss = { confirmForce = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Header Dashboard + Action buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dashboard Owner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Analitik & Performa Cafe", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            vm.checkFirebaseHealth()
                            showFirebaseDialog = true
                        },
                        shape = PillShape
                    ) {
                        Text("🔥 Firebase")
                    }
                    OutlinedButton(
                        onClick = {
                            val text = vm.getShareReportText()
                            if (text != null) shareReport(context, text)
                        },
                        shape = PillShape
                    ) {
                        Text("📱 Share")
                    }
                    Button(
                        onClick = { vm.syncNow(outletId) },
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange)
                    ) { Text("Sync", fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        item {
            // Filter Periode
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PeriodFilter.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { vm.setPeriod(outletId, p) },
                        label = { Text(p.label) }
                    )
                }
            }
        }

        // Kartu Statistik KPI
        dash?.let { d ->
            item {
                StatRow(
                    StatData(d.revenuePaid.toRupiah(), "Omzet (${d.ordersPaid} transaksi)", MaterialTheme.status.safe),
                    StatData(d.averageOrderValue.toRupiah(), "Rata-rata / Order", MaterialTheme.colorScheme.primary)
                )
            }
            item {
                StatRow(
                    StatData(
                        "${d.stoppedCount}", "Bahan Habis (Stop)",
                        if (d.stoppedCount > 0) MaterialTheme.status.stop else MaterialTheme.status.safe
                    ),
                    StatData(
                        "${d.lowCount}", "Bahan Menipis (<=10%)",
                        if (d.lowCount > 0) MaterialTheme.status.warning else MaterialTheme.status.safe
                    )
                )
            }
            item {
                StatRow(
                    StatData("${d.pendingSync}", "Belum Tersinkron", MaterialTheme.status.info),
                    StatData(d.activeShiftId ?: "—", "Shift Aktif", MaterialTheme.colorScheme.onSurface)
                )
            }

            // Diagram Batang 7 Hari Terakhir
            if (d.dailyTrend.isNotEmpty()) {
                item {
                    DailySalesBarChart(trend = d.dailyTrend)
                }
            }

            // Top 5 Menu Terlaris
            if (d.topSellingItems.isNotEmpty()) {
                item {
                    TopSellingSection(items = d.topSellingItems)
                }
            }
        }

        msg?.let {
            item {
                StatusBadge(
                    kind = if (it.contains("Error") || it.contains("Gagal")) BadgeKind.STOP else BadgeKind.INFO,
                    text = it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            HorizontalDivider()
            val conflictLabel = if (conflicts.isEmpty()) "Tidak ada order bermasalah ✓"
            else "Order Bermasalah (${conflicts.size}) — perlu keputusan manual"
            Text(conflictLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        if (conflicts.isEmpty()) {
            item {
                EmptyState(glyph = "☕", title = "Semua order aman", hint = "Tidak ada konflik stok yang perlu diselesaikan.")
            }
        } else {
            items(conflicts, key = { it.id }) { order ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().animateItem()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(order.id, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Total: ${order.total.toRupiah()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(kind = BadgeKind.STOP, text = "KONFLIK")
                        }
                        Text(
                            "⚠️ Order ini tidak dapat diselesaikan otomatis karena stok " +
                                "tidak mencukupi saat sinkronisasi. Pilih tindakan di bawah:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { vm.resolve(order.id, refund = true) },
                                shape = PillShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Refund ke Pelanggan", style = MaterialTheme.typography.labelMedium)
                            }
                            Button(
                                onClick = { confirmForce = order.id },
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Paksa Lunas", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Diagram batang visual penjualan harian (7 hari terakhir). */
@Composable
private fun DailySalesBarChart(trend: List<OwnerRepository.DailySales>) {
    val maxRev = trend.maxOfOrNull { it.revenue }?.coerceAtLeast(1L) ?: 1L

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📈 Trend Penjualan 7 Hari",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Maks: ${maxRev.toRupiah()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bar chart row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEach { day ->
                    val fraction = (day.revenue.toFloat() / maxRev).coerceIn(0.05f, 1f)
                    val animatedHeight by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(600),
                        label = "barHeight"
                    )
                    val isHighest = day.revenue == maxRev && day.revenue > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Label nominal ringkas di atas bar (misal: 120k)
                        if (day.revenue > 0) {
                            Text(
                                text = "${day.revenue / 1000}k",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = if (isHighest) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHighest) EnergyOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                        }

                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(animatedHeight)
                                .width(22.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (isHighest) EnergyOrange
                                    else if (day.revenue > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        // Hari
                        Text(
                            day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isHighest) FontWeight.Bold else FontWeight.Normal,
                            color = if (isHighest) EnergyOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/** Tampilan 5 Menu Terlaris dengan progress bar proporsional. */
@Composable
private fun TopSellingSection(items: List<TopSellingItem>) {
    val maxQty = items.maxOfOrNull { it.totalQty }?.coerceAtLeast(1) ?: 1

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "🏆 5 Menu Terlaris",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            items.forEachIndexed { index, item ->
                val medal = when (index) {
                    0 -> "🥇"
                    1 -> "🥈"
                    2 -> "🥉"
                    else -> "${index + 1}."
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(medal, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(item.menuName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "${item.totalQty} porsi (${item.totalRevenue.toRupiah()})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { item.totalQty.toFloat() / maxQty },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (index == 0) EnergyOrange else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                if (index < items.lastIndex) {
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

private fun shareReport(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan CoffeeOS POS")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Rekap Penjualan"))
}

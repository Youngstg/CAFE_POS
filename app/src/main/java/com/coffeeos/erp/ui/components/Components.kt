package com.coffeeos.erp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeeos.erp.ui.theme.status

/**
 * Komponen bersama (design.md §5). Bahasa status SELALU: warna + glif + teks,
 * tidak pernah warna saja (§10 aksesibilitas).
 */

enum class BadgeKind { SAFE, WARNING, STOP, INFO, NEUTRAL }

/** Pill badge status: background 20%-ish + teks penuh + glif (●/▲/✕/ℹ/○). */
@Composable
fun StatusBadge(kind: BadgeKind, text: String, modifier: Modifier = Modifier) {
    val st = MaterialTheme.status
    val (bg, fg, glyph) = when (kind) {
        BadgeKind.SAFE -> Triple(st.safeContainer, st.safe, "●")
        BadgeKind.WARNING -> Triple(st.warningContainer, st.warning, "▲")
        BadgeKind.STOP -> Triple(st.stopContainer, st.stop, "✕")
        BadgeKind.INFO -> Triple(st.infoContainer, st.info, "ℹ")
        BadgeKind.NEUTRAL -> Triple(st.neutralContainer, st.neutral, "○")
    }
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            "$glyph $text",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/** Status stok bahan sebagai badge (sumber kebenaran visual §9). */
@Composable
fun StockBadge(isStopped: Boolean, isLow: Boolean, modifier: Modifier = Modifier) {
    when {
        isStopped -> StatusBadge(BadgeKind.STOP, "STOP ≤2%", modifier)
        isLow -> StatusBadge(BadgeKind.WARNING, "WARNING ≤10%", modifier)
        else -> StatusBadge(BadgeKind.SAFE, "Aman", modifier)
    }
}

/** Status order dapur sebagai badge. */
@Composable
fun OrderBadge(status: String, modifier: Modifier = Modifier) {
    when (status) {
        "READY" -> StatusBadge(BadgeKind.SAFE, "READY", modifier)
        "COOKING" -> StatusBadge(BadgeKind.WARNING, "COOKING", modifier)
        "PAID" -> StatusBadge(BadgeKind.NEUTRAL, "PAID", modifier)
        "CONFLICT_NEED_REVIEW" -> StatusBadge(BadgeKind.STOP, "KONFLIK", modifier)
        else -> StatusBadge(BadgeKind.INFO, status, modifier)
    }
}

/** Keadaan kosong/offline yang hangat (cangkir, bukan ikon error generik). */
@Composable
fun EmptyState(
    glyph: String,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(glyph, style = MaterialTheme.typography.displaySmall)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Stat Card Owner: angka besar bold + label kecil + aksen kategori. */
@Composable
fun StatCard(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Konfirmasi ringkas aksi destruktif (design.md §11) — dialog, bukan swipe. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

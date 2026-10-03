package com.coffeeos.erp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeeos.erp.ui.theme.Dimens
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme
import com.coffeeos.erp.ui.theme.status

enum class BadgeKind { SAFE, WARNING, STOP, INFO, NEUTRAL }

/**
 * Status Badge SuKopi POS (DESIGN.md §6):
 * Pill kecil: titik bulat 6dp + teks labelSmall.
 */
@Composable
fun StatusBadge(
    kind: BadgeKind,
    text: String,
    modifier: Modifier = Modifier,
) {
    val (container, dotColor) = when (kind) {
        BadgeKind.SAFE -> MaterialTheme.status.safeContainer to MaterialTheme.status.safe
        BadgeKind.WARNING -> MaterialTheme.status.warningContainer to MaterialTheme.status.warning
        BadgeKind.STOP -> MaterialTheme.status.stopContainer to MaterialTheme.status.stop
        BadgeKind.INFO -> MaterialTheme.status.infoContainer to MaterialTheme.status.info
        BadgeKind.NEUTRAL -> MaterialTheme.status.neutralContainer to MaterialTheme.status.neutral
    }
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = dotColor
            )
        }
    }
}

/** Badge status spesifik untuk status pesanan POS. */
@Composable
fun OrderBadge(status: String, modifier: Modifier = Modifier) {
    val (kind, label) = when (status.uppercase()) {
        "QUEUED", "NEW ORDER", "NEW" -> BadgeKind.INFO to "New Order"
        "COOKING", "PREPARING" -> BadgeKind.WARNING to "Preparing"
        "READY" -> BadgeKind.SAFE to "Ready"
        "PAID", "SERVED", "COMPLETED" -> BadgeKind.NEUTRAL to "Completed"
        "CANCELLED", "CONFLICT_NEED_REVIEW" -> BadgeKind.STOP to "Cancelled"
        else -> BadgeKind.NEUTRAL to status
    }
    StatusBadge(kind = kind, text = label, modifier = modifier)
}

/**
 * Badge Stok SuKopi di atas foto produk (DESIGN.md §2 & §6):
 * Format: "53 Stocks"
 * > 50: info teks / infoContainer latar
 * 20..50: textPrimary / surface latar
 * < 20: danger / dangerContainer latar
 * 0: "Sold out", textTertiary / outline latar
 */
@Composable
fun StockBadgePill(
    stock: Int,
    modifier: Modifier = Modifier,
) {
    val (bg, textColor, label) = when {
        stock <= 0 -> Triple(MaterialTheme.colorScheme.outline, SukopiTheme.colors.textTertiary, "Sold out")
        stock > 50 -> Triple(SukopiTheme.colors.infoContainer, SukopiTheme.colors.info, "$stock Stocks")
        stock in 20..50 -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, "$stock Stocks")
        else -> Triple(SukopiTheme.colors.dangerContainer, SukopiTheme.colors.danger, "$stock Stocks")
    }

    Surface(
        modifier = modifier,
        shape = PillShape,
        color = bg,
        border = BorderStroke(1.dp, if (stock in 20..50) MaterialTheme.colorScheme.outline else Color.Transparent)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Option Chip SuKopi (DESIGN.md §6 & §10):
 * Tinggi 36dp, bentuk pill atau rounded 10dp, border outlineStrong (unselected) / primary (selected).
 */
@Composable
fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = PillShape,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingText: String? = null,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val border = if (selected) MaterialTheme.colorScheme.primary else SukopiTheme.colors.outlineStrong
    val content = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dimens.ChipHeight),
        shape = shape,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            leadingIcon?.invoke()
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
            if (trailingText != null) {
                Text(
                    trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Badge Counter bulat di sidebar atau tab (DESIGN.md §2 & §6):
 * Lingkaran 16dp, latar badgeRed, teks labelSmall putih.
 */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(SukopiTheme.colors.badgeRed),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else "$count",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Blok Catatan Pelanggan (DESIGN.md §6 & §7.3):
 * Latar #FDECEA (dangerContainer), radius 10dp, padding 12dp.
 */
@Composable
fun CustomerNotesBlock(
    notes: String,
    modifier: Modifier = Modifier,
) {
    if (notes.isBlank()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = SukopiTheme.colors.dangerContainer
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SukopiTheme.colors.danger,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Customer Notes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = SukopiTheme.colors.danger
                )
            }
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Tag Tipe Order (DESIGN.md §6 & §7.3):
 * Pickup Order (purple), Delivery (info), Dine In (success).
 */
@Composable
fun OrderTypeTag(
    type: String,
    tableOrTime: String? = null,
    modifier: Modifier = Modifier,
) {
    val isDineIn = type.contains("DINE", ignoreCase = true)
    val isPickup = type.contains("PICKUP", ignoreCase = true) || type.contains("TAKE", ignoreCase = true)

    val (tagColor, icon, label) = when {
        isPickup -> Triple(SukopiTheme.colors.purple, Icons.Outlined.Inventory2, "Pickup Order")
        isDineIn -> Triple(SukopiTheme.colors.success, Icons.Default.Restaurant, "Dine In" + if (!tableOrTime.isNullOrBlank()) " • $tableOrTime" else "")
        else -> Triple(SukopiTheme.colors.info, Icons.Default.LocalShipping, "Delivery Order")
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tagColor, modifier = Modifier.size(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tagColor
        )
    }
}

/**
 * Placeholder Kosong SuKopi POS (DESIGN.md §8):
 * Ikon tas belanja / cangkir besar + judul + petunjuk textTertiary.
 */
@Composable
fun EmptyState(
    glyph: String? = null,
    icon: ImageVector? = Icons.Default.ShoppingBag,
    title: String,
    hint: String = "",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (glyph != null) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SukopiTheme.colors.textTertiary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (hint.isNotBlank()) {
            Spacer(Modifier.size(4.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = SukopiTheme.colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Tampilan QR Code (fallback visual) */
@Composable
fun QrCodeImage(
    content: String,
    size: Int = 200,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.size(size.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Icon(
                imageVector = Icons.Filled.QrCode,
                contentDescription = content,
                tint = Color.Black,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Extension kompatibilitas animateItem */
fun Modifier.animateItem(): Modifier = this


package com.coffeeos.erp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeeos.erp.ui.theme.SukopiTheme

/**
 * Komponen Bersama SuKopi POS (DESIGN.md §6 & §7.5).
 */

/** Status stok bahan sebagai badge semantik. */
@Composable
fun StockBadge(isStopped: Boolean, isLow: Boolean, modifier: Modifier = Modifier) {
    when {
        isStopped -> StatusBadge(BadgeKind.STOP, "STOP ≤2%", modifier)
        isLow -> StatusBadge(BadgeKind.WARNING, "WARNING ≤10%", modifier)
        else -> StatusBadge(BadgeKind.SAFE, "Aman", modifier)
    }
}

/**
 * Kartu KPI / Stat SuKopi (DESIGN.md §7.5):
 * Radius 14dp, border 1dp outline, background surface, label labelMedium, angka headlineMedium.
 */
@Composable
fun StatCard(
    value: String,
    label: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    delta: String? = null,
    isPositiveDelta: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (delta != null) {
                    Text(
                        text = delta,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositiveDelta) SukopiTheme.colors.success else SukopiTheme.colors.danger
                    )
                }
            }
        }
    }
}

package com.coffeeos.erp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeeos.erp.core.data.local.MenuEntity
import kotlin.math.abs

/**
 * Komponen energi kasir Fase 1 (referensi Croizan, tanpa foto).
 * Slot foto disiapkan via [imageUrl] (null = tile gradien + inisial).
 * Foto asli (Coil) menyusul Fase 2 bersama aset dari owner.
 */

private val TileGradients = listOf(
    Color(0xFF6B4226) to Color(0xFF3B2417),
    Color(0xFF9C6644) to Color(0xFF6B4226),
    Color(0xFF4B6350) to Color(0xFF2E4033),
    Color(0xFFC98A2C) to Color(0xFF8A5A16)
)

/** Kartu menu besar: tile + nama + kategori · harga + status stok. */
@Composable
fun MenuTile(
    menu: MenuEntity,
    imageUrl: String? = null,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (top, bottom) = remember(menu.id) {
        TileGradients[abs(menu.id.hashCode()) % TileGradients.size]
    }
    Card(
        enabled = menu.isAvailable,
        onClick = onAdd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(96.dp)
                    .background(Brush.linearGradient(listOf(top, bottom))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    menu.name.firstOrNull()?.uppercase() ?: "☕",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f)
                )
                if (!menu.isAvailable) {
                    Text(
                        "HABIS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(menu.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${menu.category} · Rp${menu.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** KPI mini kasir: angka besar + label (tanpa chart di Fase 1). */
@Composable
fun MiniKpi(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

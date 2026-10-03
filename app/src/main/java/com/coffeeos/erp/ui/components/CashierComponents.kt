package com.coffeeos.erp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.util.toRupiah
import com.coffeeos.erp.ui.theme.Dimens
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme
import kotlin.math.abs

/**
 * Komponen SuKopi POS (DESIGN.md §6 & §10):
 * ProductCard, CategoryCard, SidebarItem, MiniKpi, MenuTile.
 */

private val AestheticTileGradients = listOf(
    Color(0xFFF04A23) to Color(0xFFD63C18), // SuKopi Primary Orange-Red
    Color(0xFF2563EB) to Color(0xFF1D4ED8), // Coffee/Drink Blue
    Color(0xFF16A34A) to Color(0xFF15803D), // Matcha Green
    Color(0xFF7C3AED) to Color(0xFF6D28D9), // Taro Purple
    Color(0xFFEA7A0B) to Color(0xFFC2410C), // Warm Caramel
    Color(0xFF475569) to Color(0xFF334155), // Roast Slate
)

/**
 * Kartu Produk SuKopi (DESIGN.md §6 & §10):
 * - Surface, radius 14dp, border 1dp outline, padding 8dp.
 * - Foto rasio 1:1, radius 12dp.
 * - Badge stok pill di kiri atas foto (offset 8dp), teks labelSmall, format "53 Stocks".
 * - Nama titleMedium, 1 baris, ellipsis.
 * - Baris harga: kiri harga labelLarge; kanan opsional diskon labelSmall warna danger.
 */
@Composable
fun ProductCard(
    menu: MenuEntity,
    imageUrl: String? = null,
    stockCount: Int = 50,
    discountPercent: Int? = null,
    onAdd: () -> Unit,
    onCustomize: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val (gradTop, gradBottom) = remember(menu.id) {
        AestheticTileGradients[abs(menu.id.hashCode()) % AestheticTileGradients.size]
    }
    val imageRes = remember(menu.name, menu.id) {
        MenuImageResolver.getDrawableForMenu(menu.name, menu.id)
    }
    val isAvailable = menu.isAvailable && stockCount > 0

    Surface(
        onClick = onAdd,
        enabled = isAvailable,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(Modifier.padding(8.dp)) {
            // Container foto produk (rasio 1:1, radius 12dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(Dimens.ProductImageRadius))
                    .background(Brush.linearGradient(listOf(gradTop, gradBottom))),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = menu.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    // Gambar ilustratif / inisial menu jika foto belum ada
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = menu.name.firstOrNull()?.uppercase() ?: "☕",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }

                // Badge Stok di kiri atas foto (offset 8dp)
                StockBadgePill(
                    stock = if (!menu.isAvailable) 0 else stockCount,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )

                // Tombol Kustomisasi (Ikon sliders horizontal di pojok kanan bawah foto)
                if (isAvailable && onCustomize != null) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        IconButton(
                            onClick = onCustomize,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Kustomisasi",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Overlay Sold Out bila stok habis
                if (!isAvailable) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Text(
                                text = "Sold out",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SukopiTheme.colors.textTertiary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Nama produk
            Text(
                text = menu.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            // Baris harga dan diskon
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = menu.price.toRupiah(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (discountPercent != null && discountPercent > 0) {
                    Text(
                        text = "$discountPercent% Off",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SukopiTheme.colors.danger
                    )
                }
            }
        }
    }
}

/**
 * MenuTile lama diteruskan ke ProductCard untuk kompatibilitas penuh.
 */
@Composable
fun MenuTile(
    menu: MenuEntity,
    imageUrl: String? = null,
    onAdd: () -> Unit,
    onCustomize: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ProductCard(
        menu = menu,
        imageUrl = imageUrl,
        stockCount = if (menu.isAvailable) 53 else 0,
        onAdd = onAdd,
        onCustomize = onCustomize,
        modifier = modifier
    )
}

/**
 * Kartu Kategori SuKopi (DESIGN.md §6 Kartu Kategori):
 * - Tinggi ~64dp, radius 14dp.
 * - Isi: ikon 16dp, nama labelLarge, jumlah ("6 items") labelSmall textTertiary.
 * - Normal: surface + border outline.
 * - Terpilih: primaryContainer + border primary, ikon & teks primary.
 */
@Composable
fun CategoryCard(
    name: String,
    itemCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Coffee,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val content = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(1.dp, border),
        modifier = modifier.height(64.dp)
    ) {
        Row(
            Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = content,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$itemCount items",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else SukopiTheme.colors.textTertiary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Item Sidebar SuKopi (DESIGN.md §6 & §10):
 * Tinggi 40dp, pill, ikon 18dp + label labelLarge, gap 10dp, padding horizontal 12dp.
 * Normal: ikon & teks textSecondary, latar transparan.
 * Aktif: latar primary, ikon & teks putih.
 * Badge counter: lingkaran 16dp badgeRed, angka putih labelSmall di kanan item.
 */
@Composable
fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else SukopiTheme.colors.textSecondary

    Surface(
        onClick = onClick,
        shape = PillShape,
        color = bg,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeCount > 0) {
                CountBadge(count = badgeCount)
            }
        }
    }
}

/**
 * KPI mini SuKopi (DESIGN.md §7.5):
 * Radius 14dp, border 1dp outline, background surface.
 */
@Composable
fun MiniKpi(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

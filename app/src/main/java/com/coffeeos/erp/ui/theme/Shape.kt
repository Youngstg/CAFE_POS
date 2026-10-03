package com.coffeeos.erp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Bentuk & Dimensi SuKopi POS (DESIGN.md §4 & §10).
 */
val SukopiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),  // Badge
    small      = RoundedCornerShape(10.dp), // Chip option, kartu item, input
    medium     = RoundedCornerShape(14.dp), // Kartu produk, kategori, kartu order
    large      = RoundedCornerShape(20.dp), // Panel utama, dialog
)

val PillShape = RoundedCornerShape(percent = 50)

object Dimens {
    val SidebarWidth = 200.dp
    val CartPanelWidth = 320.dp
    val PagePadding = 24.dp
    val CardGap = 12.dp
    val ChipHeight = 36.dp
    val SearchHeight = 40.dp
    val ProductImageRadius = 12.dp
}

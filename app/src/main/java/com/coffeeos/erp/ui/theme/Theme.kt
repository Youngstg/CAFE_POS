package com.coffeeos.erp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design system CoffeeOS (design.md §2–§5).
 * Palet earthy hangat; tanpa merah murni — status semantik turun dari palet.
 * Font custom (Inter/Plus Jakarta Sans) menyusul: butuh file font + review lisensi.
 */

// --- Brand ---
private val Coffee900 = Color(0xFF3B2417)
private val Coffee700 = Color(0xFF6B4226)
private val Coffee500 = Color(0xFF9C6644)
private val Latte200 = Color(0xFFE8D5C4)
private val Latte100 = Color(0xFFF5EBE0)
private val Cream50 = Color(0xFFFBF7F2)

// --- Sekunder ---
private val Sage700 = Color(0xFF4B6350)
private val Sage200 = Color(0xFFD3DFD4)

/** Warna semantik status terpadu (design.md §9) — sama di semua modul. */
@Immutable
data class StatusColors(
    val safe: Color,
    val safeContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val stop: Color,
    val stopContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val neutral: Color,
    val neutralContainer: Color,
)

private val DefaultStatus = StatusColors(
    safe = Sage700,
    safeContainer = Sage200,
    warning = Color(0xFFC98A2C),
    warningContainer = Color(0xFFF3E3C3),
    stop = Color(0xFFB3402F),
    stopContainer = Color(0xFFF2D3CC),
    info = Color(0xFF6B7FA6),
    infoContainer = Color(0xFFD9E1F0),
    neutral = Color(0xFF8A7B6D),
    neutralContainer = Latte200
)

val LocalStatusColors = staticCompositionLocalOf { DefaultStatus }

private val LightColors = lightColorScheme(
    primary = Coffee700,
    onPrimary = Cream50,
    primaryContainer = Latte200,
    onPrimaryContainer = Coffee900,
    secondary = Coffee500,
    tertiary = Sage700,
    background = Latte100,
    onBackground = Coffee900,
    surface = Cream50,
    onSurface = Coffee900,
    surfaceVariant = Latte200,
    onSurfaceVariant = Coffee900,
    error = Color(0xFFB3402F),
    tertiaryContainer = Sage200
)

private val CoffeeosShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp)
)

/** Ambang selisih kas "kecil" (design.md §8.3): di bawah ini kuning, di atasnya merah. */
const val SMALL_DIFF_THRESHOLD = 10_000L

@Composable
fun CoffeeosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = CoffeeosShapes,
        content = content
    )
}

/** Akses: MaterialTheme.status.warning dst. */
val MaterialTheme.status: StatusColors
    @Composable get() = LocalStatusColors.current

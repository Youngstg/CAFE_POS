package com.coffeeos.erp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Palet Warna SuKopi POS (DESIGN.md §2).
 * Flat, bersih, satu aksen oranye-merah (primary).
 */

// --- Brand ---
val Primary = Color(0xFFF04A23)
val PrimaryPressed = Color(0xFFD63C18)
val PrimaryContainer = Color(0xFFFDEDE8)
val OnPrimary = Color(0xFFFFFFFF)

// --- Neutral ---
val Background = Color(0xFFF4F4F5)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF8F8F9)
val Outline = Color(0xFFE7E7EA)
val OutlineStrong = Color(0xFFD4D4D8)

val TextPrimary = Color(0xFF18181B)
val TextSecondary = Color(0xFF71717A)
val TextTertiary = Color(0xFFA1A1AA)
val ScrimColor = Color(0x73000000) // 45% black

// --- Semantic ---
val Info = Color(0xFF2563EB)
val InfoContainer = Color(0xFFE8F0FE)
val Warning = Color(0xFFEA7A0B)
val WarningContainer = Color(0xFFFFF1E0)
val Danger = Color(0xFFE5391B)
val DangerContainer = Color(0xFFFDECEA)
val Success = Color(0xFF16A34A)
val SuccessContainer = Color(0xFFE7F6EC)
val Purple = Color(0xFF7C3AED)
val BadgeRed = Color(0xFFEF4444)

// Backward compatibility alias
val EnergyOrange = Primary
val EnergyOrangeDark = PrimaryPressed

// --- Dark Mode Tokens (DESIGN.md §2) ---
val BackgroundDark = Color(0xFF0F0F11)
val SurfaceDark = Color(0xFF18181B)
val SurfaceVariantDark = Color(0xFF202024)
val OutlineDark = Color(0xFF2E2E33)
val TextPrimaryDark = Color(0xFFF4F4F5)
val TextSecondaryDark = Color(0xFFA1A1AA)
val PrimaryDark = Color(0xFFFF6A45)
val PrimaryContainerDark = Color(0xFF3A1A12)

/** Extended tokens di luar default MaterialTheme (DESIGN.md §10). */
@Immutable
data class SukopiColors(
    val info: Color = Info,
    val infoContainer: Color = InfoContainer,
    val warning: Color = Warning,
    val warningContainer: Color = WarningContainer,
    val danger: Color = Danger,
    val dangerContainer: Color = DangerContainer,
    val success: Color = Success,
    val successContainer: Color = SuccessContainer,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextTertiary,
    val outlineStrong: Color = OutlineStrong,
    val purple: Color = Purple,
    val badgeRed: Color = BadgeRed,
)

val LocalSukopiColors = staticCompositionLocalOf<SukopiColors> {
    SukopiColors()
}

object SukopiTheme {
    val colors: SukopiColors
        @Composable get() = LocalSukopiColors.current
}

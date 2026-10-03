package com.coffeeos.erp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tema SuKopi POS (DESIGN.md §2 & §10).
 * Menggantikan palet lama dengan visual bersih, flat, dan satu aksen oranye-merah (#F04A23).
 */

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

private val LightStatus = StatusColors(
    safe = Success,
    safeContainer = SuccessContainer,
    warning = Warning,
    warningContainer = WarningContainer,
    stop = Danger,
    stopContainer = DangerContainer,
    info = Info,
    infoContainer = InfoContainer,
    neutral = TextSecondary,
    neutralContainer = Outline
)

private val DarkStatus = StatusColors(
    safe = Success,
    safeContainer = Color(0xFF1E3324),
    warning = Warning,
    warningContainer = Color(0xFF3D2E0E),
    stop = Danger,
    stopContainer = Color(0xFF3D1510),
    info = Info,
    infoContainer = Color(0xFF1E2A3D),
    neutral = TextSecondaryDark,
    neutralContainer = OutlineDark
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = Primary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    error = Danger,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = SurfaceDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = PrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = Danger,
)

/** Ambang selisih kas untuk shift. */
const val SMALL_DIFF_THRESHOLD = 10_000L

/**
 * Tema utama SuKopi POS (DESIGN.md §10).
 */
@Composable
fun SukopiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extended = SukopiColors(
        info = Info,
        infoContainer = InfoContainer,
        warning = Warning,
        warningContainer = WarningContainer,
        danger = Danger,
        dangerContainer = DangerContainer,
        success = Success,
        successContainer = SuccessContainer,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textTertiary = TextTertiary,
        outlineStrong = OutlineStrong,
        purple = Purple,
        badgeRed = BadgeRed,
    )
    val statusColors = if (darkTheme) DarkStatus else LightStatus
    val colorScheme = if (darkTheme) DarkColors else LightColors

    CompositionLocalProvider(
        LocalSukopiColors provides extended,
        LocalStatusColors provides statusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SukopiTypography,
            shapes = SukopiShapes,
            content = content
        )
    }
}

/** Alias untuk kompatibilitas kode yang memanggil CoffeeosTheme. */
@Composable
fun CoffeeosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    SukopiTheme(darkTheme = darkTheme, content = content)
}

/** Akses cepat: MaterialTheme.status.warning, dst. */
val MaterialTheme.status: StatusColors
    @Composable get() = LocalStatusColors.current

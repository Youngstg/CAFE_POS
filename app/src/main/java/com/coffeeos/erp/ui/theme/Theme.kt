package com.coffeeos.erp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoffeeBrown = Color(0xFF4E342E)
private val Cream = Color(0xFFFFF8E1)

private val LightColors = lightColorScheme(
    primary = CoffeeBrown,
    onPrimary = Color.White,
    secondaryContainer = Cream
)

@Composable
fun CoffeeosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}

// Dark scheme disiapkan untuk mode malam KDS dapur (opsional).
@Suppress("unused")
private val DarkColors = darkColorScheme(primary = Color(0xFFD7CCC8))

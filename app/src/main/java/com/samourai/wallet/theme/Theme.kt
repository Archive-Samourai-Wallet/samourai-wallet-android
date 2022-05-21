package com.samourai.wallet.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = samouraiSlateGreyAccent,
    background = samouraiWindow,
    surface = samouraiWindow,
    onPrimary = samouraiTextPrimary,
    onSecondary = samouraiTextSecondary,
    secondary = samouraiAccent
)

@Composable
fun SamouraiWalletTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        DarkColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
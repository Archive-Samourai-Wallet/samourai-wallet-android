package com.samourai.wallet.util.tech

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

class ColorHelper() {

    companion object {
        fun getAttributeColor(
            context: Context,
            attr: Int,
        ): Color {
            val navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.resources.getColor(
                    context.theme.obtainStyledAttributes(intArrayOf(attr))
                        .getResourceId(0, 0), context.theme
                )
            } else {
                @Suppress("DEPRECATION")
                context.resources.getColor(
                    context.theme.obtainStyledAttributes(intArrayOf(attr))
                        .getResourceId(0, 0)
                )
            }
            return Color(navigationBarColor)
        }

        fun lightenColor(color: Color, factor: Float): Color {
            val clampFactor = factor.coerceIn(0f, 1f)
            return lerp(color, Color.White, clampFactor)
        }

        fun darkenColor(color: Color, factor: Float): Color {
            val clampFactor = factor.coerceIn(0f, 1f)
            return lerp(color, Color.Black, clampFactor)
        }
    }
}
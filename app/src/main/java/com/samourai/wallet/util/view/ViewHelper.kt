package com.samourai.wallet.util.view

import android.content.Context
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

class ViewHelper {

    companion object {
        fun convertDpToPx(valueInDp: Dp, density: Density): Float {
            return with(density) { valueInDp.toPx() }
        }

        fun getScreenWidthPx(context: Context): Int {
            val resources = context.resources
            val displayMetrics = resources.displayMetrics
            return displayMetrics.widthPixels
        }
    }
}
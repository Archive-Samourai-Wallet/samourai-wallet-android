package com.samourai.wallet.calculator

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@ExperimentalComposeUiApi
@Composable
fun CalculatorButton(
    symbol: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    textColor: Color = Color.White,
    textStyle: TextStyle = TextStyle(),
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color)
            .clickable {
                onClick()
            }
            .then(modifier)
    ) {
        if (symbol == "") {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = "")
            }
        } else {
            Text(
                text = symbol,
                style = textStyle,
                fontSize = 36.sp,
                color = textColor
            )
        }
    }
}
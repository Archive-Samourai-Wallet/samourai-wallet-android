package com.samourai.wallet.util.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SamCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkboxSize: Dp = 20.dp,
    strokeWidth: Dp = 2.dp,
    checkIconColor: Color = Color.Black,
    rectColor: Color = Color.White
) {
    val radiusRatio = 0.125f
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(checkboxSize + strokeWidth)
            .clip(RoundedCornerShape(checkboxSize.times(radiusRatio)))
            .background(if (checked) rectColor else Color.Transparent)
            .clickable(onClick = { onCheckedChange(!checked) })
    ) {
        Canvas(modifier = Modifier.size(checkboxSize)) {
            drawRoundRect(
                color = rectColor,
                size = size,
                style = Stroke(width = strokeWidth.toPx()),
                cornerRadius = CornerRadius(
                    x = checkboxSize.toPx() * radiusRatio,
                    y = checkboxSize.toPx() * radiusRatio)
            )
        }
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = checkIconColor,
                modifier = Modifier.size(checkboxSize * 0.95f)
            )
        }
    }
}

@Preview
@Composable
fun PreviewCustomCheckbox() {
    var isChecked by remember { mutableStateOf(true) }

    SamCheckbox(
        checked = isChecked,
        onCheckedChange = { isChecked = it }
    )
}

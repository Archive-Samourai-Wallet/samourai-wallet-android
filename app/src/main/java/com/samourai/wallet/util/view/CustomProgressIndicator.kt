package com.samourai.wallet.util.view
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.wallet.theme.samouraiAccent
import org.apache.commons.lang3.StringUtils

@Composable
fun CustomProgressIndicator(progress: LiveData<Float>,
                            visible: LiveData<Boolean> = MutableLiveData(true),
                            totalShapes: Int = 10,
                            progressShapeColor: Color = samouraiAccent,
                            progressBackgroundColor: Color = Color.LightGray,
                            backgroundColor: Color = Color.Transparent,
                            text: String = "",
                            textColor: Color = Color.LightGray,
                            padding: Dp = 4.dp
) {

    val progressValue by progress.observeAsState()
    val isVisible by visible.observeAsState()

    if (! isVisible!!) return

    Column (
        modifier = Modifier
            .background(backgroundColor)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        if (StringUtils.isNoneBlank(text)) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = textColor,
                fontWeight = FontWeight.Bold,
            )
        }

        Row {
            for (i in 1..totalShapes) {
                val color = if (i <= progressValue!! * totalShapes) progressShapeColor else progressBackgroundColor
                Canvas(modifier = Modifier
                    .size(width = 20.dp, height = 10.dp)
                ) {
                    val path = Path().apply {
                        val skewWidth = size.width / 4
                        moveTo(skewWidth, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width - skewWidth, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = color
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCustomProgressIndicator() {
    var progress by remember { mutableStateOf(0.6f) }
    CustomProgressIndicator(progress = MutableLiveData(progress), text = "LOADING")
}

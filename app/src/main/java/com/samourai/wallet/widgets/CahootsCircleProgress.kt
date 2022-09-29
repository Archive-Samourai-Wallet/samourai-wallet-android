package com.samourai.wallet.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData


class CahootsCircleProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var totalSteps = MutableLiveData(1);
    private var currentStep = MutableLiveData(1);

    @Composable
    override fun Content() {
        ProgressView(totalSteps, currentStep)
    }

    fun setTotalSteps(total: Int) {
        totalSteps.postValue(total)
    }

    fun setStep(step: Int) {
        currentStep.postValue(step)
    }
}

@Composable
fun ProgressView(totalSteps: MutableLiveData<Int>, currentStep: MutableLiveData<Int>) {
    val total by totalSteps.observeAsState(null)
    val current by currentStep.observeAsState(null)
    val progress:Float = if (current != null && total != null) {
        current!!.toFloat().div(total!!.toFloat())
    } else {
        .1f
    }
    BoxWithConstraints {
        val height = this.maxHeight;
        Box(
            modifier = Modifier
                .size(this.maxWidth, this.maxHeight)
                .align(Alignment.Center)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(), Arrangement.Center,
                Alignment.CenterVertically
            ) {
                CircleProgressView(
                    progressText = "${current}/${total}",
                    color = Color.White,
                    radius = height.times(0.85f),
                    progress = progress
                )
            }
        }
    }
}

@Composable
fun CircleProgressView(
    progress: Float,
    modifier: Modifier = Modifier,
    progressText: String,
    radius: Dp,
    color: Color = MaterialTheme.colors.primary
) {
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 820,
        )
    )
    Column(
        modifier = modifier
            .clip(CircleShape)
            .drawBehind {
                drawProgress(
                    progress = animatedProgress.value,
                    progressMax = 1f,
                    progressBarColor = color,
                    backgroundProgressBarWidth = 12.dp,
                    progressBarWidth = 16.dp,
                    roundBorder = true,
                    backgroundProgressBarColor = Color(0xC1292929)
                )
            }

            .size(radius),
        Arrangement.Center,
        Alignment.CenterHorizontally
    ) {
        Text(text = progressText, color = color, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}


fun DrawScope.drawProgress(
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    progressMax: Float = 100f,
    progressBarColor: Color = Color.Black,
    progressBarWidth: Dp = 7.dp,
    backgroundProgressBarColor: Color = Color.Gray,
    backgroundProgressBarWidth: Dp = 3.dp,
    roundBorder: Boolean = false,
    startAngle: Float = 0f
) {
    val canvasSize = size.minDimension

    val radius = canvasSize / 2 - maxOf(backgroundProgressBarWidth, progressBarWidth).toPx() / 2

    drawCircle(
        color = backgroundProgressBarColor,
        radius = radius,
        center = size.center,
        style = Stroke(width = backgroundProgressBarWidth.toPx())
    )

    drawArc(
        color = progressBarColor,
        startAngle = 270f + startAngle,
        sweepAngle = (progress / progressMax) * 360f,
        useCenter = false,
        topLeft = size.center - Offset(radius, radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(
            width = progressBarWidth.toPx(),
            cap = if (roundBorder) StrokeCap.Round else StrokeCap.Butt
        )
    )
}

@Preview(widthDp = 400, heightDp = 240)
@Composable
fun CircleProgressViewPreview() {
    ProgressView(MutableLiveData(6), MutableLiveData(2))
}
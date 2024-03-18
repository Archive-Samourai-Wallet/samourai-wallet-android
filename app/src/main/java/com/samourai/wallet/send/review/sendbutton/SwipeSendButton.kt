package com.samourai.wallet.send.review.sendbutton

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.samourai.wallet.R
import com.samourai.wallet.send.review.MyModelPreviewProvider
import com.samourai.wallet.send.review.ReviewTxModel
import com.samourai.wallet.send.review.sendbutton.SwipeSendButtonListener.EnumSwipeSendButtonState.DONE
import com.samourai.wallet.send.review.sendbutton.SwipeSendButtonListener.EnumSwipeSendButtonState.IS_SWIPING_DISABLED
import com.samourai.wallet.send.review.sendbutton.SwipeSendButtonListener.EnumSwipeSendButtonState.IS_SWIPING_ENABLED
import com.samourai.wallet.theme.samouraiBoxLightGrey
import com.samourai.wallet.theme.samouraiSuccess
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.tech.ColorHelper
import com.samourai.wallet.util.tech.ColorHelper.Companion.darkenColor
import com.samourai.wallet.util.tech.ColorHelper.Companion.lightenColor
import com.samourai.wallet.util.tech.HapticHelper.Companion.vibratePhone
import com.samourai.wallet.util.view.ViewHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import kotlin.math.roundToInt


@Composable
fun SwipeSendButtonContent(
    amountToLeaveWallet: LiveData<Long>,
    amountToLeaveWalletColor: Color = Color.White,
    action: Runnable?,
    enable: LiveData<Boolean>,
    listener: SwipeSendButtonListener?,
    alphaBackground: Float
) {

    val context = LocalContext.current

    val showTapAndHoldComponent = remember { mutableStateOf(false) }
    val isOnSwipeValidation = remember { mutableStateOf(false) }
    val whiteAlpha = if(isOnSwipeValidation.value) 0.1f else 0f

    val windowBackground = ColorHelper.getAttributeColor(
        context = context,
        attr = android.R.attr.windowBackground
    )

    Box (
        modifier = Modifier
            .background(lightenColor(windowBackground, whiteAlpha).copy(alpha = alphaBackground))
    ) {
        Column (
            verticalArrangement = Arrangement.Bottom
        ) {
            TapAndHoldInfoComponent(
                visible = showTapAndHoldComponent,
                whiteAlpha = whiteAlpha)

            SwipeSendButtonComponent(
                amount = amountToLeaveWallet,
                amountColor = amountToLeaveWalletColor,
                action = action,
                enable = enable,
                tapAndHoldInfo = showTapAndHoldComponent,
                isOnSwipeValidation = isOnSwipeValidation,
                listener = listener)
        }
    }
}

@Composable
private fun TapAndHoldInfoComponent(
    visible: MutableState<Boolean>,
    whiteAlpha: Float
) {

    val ralewayFont = FontFamily(
        Font(R.font.raleway_regular, FontWeight.Normal)
    )

    Row (
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = if (visible.value)
                Modifier
                    .background(
                        lightenColor(samouraiBoxLightGrey, whiteAlpha),
                        RoundedCornerShape(20.dp)
                    )
            else Modifier,
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 12.dp, top = 8.dp, start = 14.dp, end = 14.dp),
            ) {
                androidx.compose.material.Text(
                    text = if (visible.value) "Tap and hold" else StringUtils.EMPTY,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = ralewayFont
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SwipeSendButtonComponent(
    amount: LiveData<Long>,
    amountColor: Color = Color.White,
    action:Runnable?,
    enable: LiveData<Boolean>,
    tapAndHoldInfo: MutableState<Boolean>,
    isOnSwipeValidation: MutableState<Boolean>,
    listener: SwipeSendButtonListener?
) {

    val context = LocalContext.current
    val density = LocalDensity.current
    val buttonSize = 48.dp

    var jobHoldInfo by remember { mutableStateOf<Job?>(null) }
    var jobForSwipeValidation by remember { mutableStateOf<Job?>(null) }
    var isFullSwiped by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isButtonPressed by remember { mutableStateOf(false) }
    var btnAlpha = if (isButtonPressed || isOnSwipeValidation.value) 0.90f else 1f
    btnAlpha = if (enable.value!!) btnAlpha else btnAlpha - 0.5f

    val swipeButtonPx = ViewHelper.convertDpToPx(valueInDp = buttonSize, density = density)
    var componentSize by remember { mutableStateOf(Size.Zero) }
    var swipeButtonThresold = (componentSize.width / 2f - swipeButtonPx / 2f).toInt()

    val robotoMonoNormalFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Normal)
    )
    val ralewayFont = FontFamily(
        Font(R.font.raleway_regular, FontWeight.Normal)
    )
    val hapticTadaPattern = longArrayOf(30, 64, 120, 64)

    val executeAction: () -> Unit = {
        action?.run()
    }

    var dragOffset by remember { mutableStateOf(IntOffset(0, 0)) }

    val windowBackground = ColorHelper.getAttributeColor(
        context = context,
        attr = android.R.attr.windowBackground
    )

    Box (
        modifier = if (isOnSwipeValidation.value)
            Modifier
                .padding(bottom = 7.dp, top = 7.dp)
                .background(windowBackground, RoundedCornerShape(20.dp))
                .onSizeChanged { newSize ->
                    componentSize = Size(newSize.width.toFloat(), newSize.height.toFloat())
                    swipeButtonThresold = (componentSize.width / 2f - swipeButtonPx / 2f).toInt()
                } else
            Modifier
                .padding(bottom = 7.dp, top = 7.dp)
                .onSizeChanged { newSize ->
                    componentSize = Size(newSize.width.toFloat(), newSize.height.toFloat())
                    swipeButtonThresold = (componentSize.width / 2f - swipeButtonPx / 2f).toInt()
                }
    ) {
        if (isOnSwipeValidation.value) {
            Row (
                modifier = Modifier
                    .padding(start = 12.dp)
                    .height(buttonSize),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material.Text(
                    text = FormatsUtil.formatBTC(amount.value),
                    color = amountColor,
                    fontSize = 14.sp,
                    fontFamily = robotoMonoNormalFont
                )
            }
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .height(buttonSize),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column (
                    modifier = Modifier.padding(bottom = 5.dp)
                ) {
                    androidx.compose.material.Text(
                        text = "Swipe to send",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = ralewayFont
                    )
                }
            }
        }

        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {

            Box (
                modifier = Modifier
                    .size(buttonSize)
                    .offset { dragOffset }
                    .then(
                        if (enable.value!!) {
                            Modifier
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            if (!isFullSwiped) {
                                                vibratePhone(durationMs = 50L, context = context)
                                                jobForSwipeValidation?.cancel()
                                                isOnSwipeValidation.value = true
                                                listener?.onStateChange(IS_SWIPING_ENABLED)
                                            }
                                        },
                                        onDragEnd = {
                                            if (!isFullSwiped) {
                                                dragOffset = IntOffset(0, dragOffset.y)
                                                jobForSwipeValidation?.cancel()
                                                jobForSwipeValidation = scope.launch {
                                                    delay(250L)
                                                    isOnSwipeValidation.value = false
                                                    listener?.onStateChange(IS_SWIPING_DISABLED)
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            if (!isFullSwiped) {
                                                dragOffset = IntOffset(0, dragOffset.y)
                                                jobForSwipeValidation?.cancel()
                                                jobForSwipeValidation = scope.launch {
                                                    delay(250L)
                                                    isOnSwipeValidation.value = false
                                                    listener?.onStateChange(IS_SWIPING_DISABLED)
                                                }
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            if (!isFullSwiped) {

                                                val newX =
                                                    (dragOffset.x + dragAmount.x.roundToInt())
                                                        .coerceAtLeast(0)
                                                        .coerceAtMost(swipeButtonThresold)

                                                dragOffset = IntOffset(newX, dragOffset.y)
                                                change.consume()
                                                if (newX >= swipeButtonThresold) {
                                                    isFullSwiped = true
                                                    vibratePhone(
                                                        durationMs = 50L,
                                                        context = context
                                                    )
                                                    scope.launch {
                                                        executeAction.invoke()
                                                        listener?.onStateChange(DONE)
                                                        scope.launch {
                                                            delay(1000L)
                                                            dragOffset = IntOffset(0, dragOffset.y)
                                                            jobForSwipeValidation?.cancel()
                                                            jobForSwipeValidation = scope.launch {
                                                                delay(250L)
                                                                isOnSwipeValidation.value = false
                                                                isFullSwiped = false
                                                                listener?.onStateChange(IS_SWIPING_DISABLED)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                .pointerInteropFilter { event ->
                                    when (event.action) {

                                        MotionEvent.ACTION_DOWN -> {
                                            isButtonPressed = true
                                            jobHoldInfo?.cancel()
                                            tapAndHoldInfo.value = false
                                            true
                                        }

                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            isButtonPressed = false
                                            jobHoldInfo?.cancel()
                                            jobHoldInfo = scope.launch {
                                                delay(2000)
                                                tapAndHoldInfo.value = false
                                            }
                                            if (!isOnSwipeValidation.value) {
                                                tapAndHoldInfo.value = true
                                                vibratePhone(
                                                    pattern = hapticTadaPattern,
                                                    context = context
                                                )
                                            }
                                            true
                                        }

                                        else -> false
                                    }
                                }
                        } else {
                            Modifier
                        }
                    )

            ) {
                Row (
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    IconButton(
                        onClick = { },
                        enabled = enable.value!!,
                        modifier = Modifier
                            .size(buttonSize)
                            .background(
                                darkenColor(samouraiSuccess, 1f - btnAlpha),
                                CircleShape
                            )
                            .clip(CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = null,
                            tint = darkenColor(Color(255, 255, 255, 255), 1f-btnAlpha)
                        )
                    }

                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
fun SwipeSendButtonContentPreview(
    @PreviewParameter(MyModelPreviewProvider::class) model: ReviewTxModel
) {
    SwipeSendButtonContent(
        amountToLeaveWallet = MutableLiveData(125000L),
        action = null,
        enable = MutableLiveData(true),
        listener = null,
        alphaBackground = 1f)
}
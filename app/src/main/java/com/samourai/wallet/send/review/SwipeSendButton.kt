package com.samourai.wallet.send.review

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
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
import com.samourai.wallet.send.review.SwipeSendButtonListener.EnumSwipeSendButtonState.DONE
import com.samourai.wallet.send.review.SwipeSendButtonListener.EnumSwipeSendButtonState.IS_SWIPING_DISABLED
import com.samourai.wallet.send.review.SwipeSendButtonListener.EnumSwipeSendButtonState.IS_SWIPING_ENABLED
import com.samourai.wallet.theme.samouraiBoxLightGrey
import com.samourai.wallet.theme.samouraiLightGreyAccent
import com.samourai.wallet.theme.samouraiSuccess
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.tech.ColorHelper.Companion.darkenColor
import com.samourai.wallet.util.tech.ColorHelper.Companion.lightenColor
import com.samourai.wallet.util.tech.HapticHelper.Companion.vibratePhone
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import kotlin.math.roundToInt


@Composable
fun SwipeSendButtonContent(
    amountToLeaveWallet: LiveData<Long>,
    action: Runnable?,
    enable: LiveData<Boolean>,
    listener: SwipeSendButtonListener?
) {

    val showTapAndHoldComponent = remember { mutableStateOf(false) }
    val isOnSwipeValidation = remember { mutableStateOf(false) }
    val whiteAlpha = if(isOnSwipeValidation.value) 0.1f else 0f

    Box {
        Column (
            verticalArrangement = Arrangement.Bottom
        ) {
            TapAndHoldInfoComponent(
                visible = showTapAndHoldComponent,
                whiteAlpha = whiteAlpha)

            SwipeSendButtonComponent(
                amount = amountToLeaveWallet,
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
        Font(R.font.raleway, FontWeight.Normal)
    )

    Row (
        modifier = if (visible.value)
            Modifier
                .fillMaxWidth()
                .background(
                    lightenColor(samouraiBoxLightGrey, whiteAlpha),
                    RoundedCornerShape(20.dp)
                )
        else Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 14.dp, top = 9.dp, start = 18.dp, end = 18.dp),
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SwipeSendButtonComponent(
    amount: LiveData<Long>,
    action:Runnable?,
    enable: LiveData<Boolean>,
    tapAndHoldInfo: MutableState<Boolean>,
    isOnSwipeValidation: MutableState<Boolean>,
    listener: SwipeSendButtonListener?
) {

    val context = LocalContext.current
    val buttonSize = 48.dp
    val screenWidthPx = getScreenWidthPx()

    var jobHoldInfo by remember { mutableStateOf<Job?>(null) }
    var jobForSwipeValidation by remember { mutableStateOf<Job?>(null) }
    var isFullSwiped by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isButtonPressed by remember { mutableStateOf(false) }
    var btnAlpha = if (isButtonPressed || isOnSwipeValidation.value) 0.90f else 1f
    btnAlpha = if (enable.value!!) btnAlpha else btnAlpha - 0.5f

    val swipeButtonPx = convertDpToPx(valueInDp = buttonSize)
    val externalPaddingPx = convertDpToPx(valueInDp = 18.dp)

    val robotoMonoNormalFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Normal)
    )
    val ralewayFont = FontFamily(
        Font(R.font.raleway, FontWeight.Normal)
    )
    val hapticTadaPattern = longArrayOf(30, 64, 120, 64)

    val executeAction: () -> Unit = {
        action?.run()
    }

    var dragOffset by remember { mutableStateOf(IntOffset(0, 0)) }

    Box (
        modifier = if (isOnSwipeValidation.value)
            Modifier
                .padding(bottom = 9.dp, top = 9.dp)
                .background(samouraiLightGreyAccent, RoundedCornerShape(20.dp)) else
            Modifier
                .padding(bottom = 9.dp, top = 9.dp)
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
                    color = Color.White,
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
                                                dragOffset = IntOffset(newX, dragOffset.y)
                                                change.consume()
                                                if (newX >= (screenWidthPx / 2f - externalPaddingPx - swipeButtonPx / 2f)) {
                                                    isFullSwiped = true
                                                    vibratePhone(
                                                        durationMs = 50L,
                                                        context = context
                                                    )
                                                    scope.launch {
                                                        executeAction.invoke()
                                                    }
                                                    listener?.onStateChange(DONE)
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
        MutableLiveData(125000L),
        null,
        MutableLiveData(true),
        null)
}
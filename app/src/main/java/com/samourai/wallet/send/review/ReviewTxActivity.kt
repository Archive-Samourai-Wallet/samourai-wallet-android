package com.samourai.wallet.send.review

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import com.google.common.collect.ImmutableList
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.home.TestApplication
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.send.review.ReviewTxActivity.Companion.TAG
import com.samourai.wallet.send.review.ReviewTxBottomSheet.ReviewSheetType
import com.samourai.wallet.send.review.ReviewTxBottomSheet.ReviewSheetType.FILTER_UTXO
import com.samourai.wallet.send.review.ReviewTxBottomSheet.ReviewSheetType.MANAGE_FEE
import com.samourai.wallet.send.review.ReviewTxBottomSheet.ReviewSheetType.PREVIEW_TX
import com.samourai.wallet.send.review.preview.PreviewTx
import com.samourai.wallet.send.review.ref.EnumReviewScreen
import com.samourai.wallet.send.review.ref.EnumSendType
import com.samourai.wallet.send.review.ref.EnumTxAlert
import com.samourai.wallet.send.review.sendbutton.SwipeSendButtonContent
import com.samourai.wallet.send.review.sendbutton.SwipeSendButtonListener
import com.samourai.wallet.send.review.sendbutton.SwipeSendButtonListener.EnumSwipeSendButtonState
import com.samourai.wallet.theme.samouraiAlerts
import com.samourai.wallet.theme.samouraiBlueButton
import com.samourai.wallet.theme.samouraiLightGreyAccent
import com.samourai.wallet.theme.samouraiPostmixSpendBlueButton
import com.samourai.wallet.theme.samouraiSlateGreyAccent
import com.samourai.wallet.theme.samouraiTextLightGrey
import com.samourai.wallet.util.func.BatchSendUtil
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.tech.ColorHelper.Companion.getAttributeColor
import com.samourai.wallet.util.tech.ColorHelper.Companion.lightenColor
import com.samourai.wallet.util.view.rememberImeState
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.collections4.ListUtils
import org.apache.commons.lang3.StringUtils.EMPTY
import org.apache.commons.lang3.StringUtils.SPACE
import org.apache.commons.lang3.StringUtils.isBlank
import java.lang.String.format
import java.text.DecimalFormat
import java.util.Objects
import java.util.Objects.nonNull

class ReviewTxActivity : SamouraiActivity() {

    companion object {
        const val TAG = "ReviewTxActivity"
    }

    private val reviewTxModel: ReviewTxModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        account = intent.extras!!.getInt("_account")
        val type = intent.getIntExtra("sendType", 0)

        reviewTxModel
            .setAccount(account)
            .setAddress(intent.getStringExtra("sendAddress"))
            .setAddressLabel(intent.getStringExtra("sendAddressLabel"))
            .setAmount(intent.getLongExtra("sendAmount", 0L))
            .setRicochetStaggeredDelivery(intent.getBooleanExtra("ricochetStaggeredDelivery", false))
            .setType(EnumSendType.firstFromType(type))

        if (intent.hasExtra("preselected")) {
            reviewTxModel
                .setPreselectedUtxo(intent.getStringExtra("preselected"))
        }

        setupThemes()

        setContent {
            ReviewTxActivityContent(model = reviewTxModel, activity = this)
        }
    }
}

@Composable
fun ReviewTxActivityContent(model: ReviewTxModel, activity: SamouraiActivity?) {

    val context = LocalContext.current

    var isOnSwipeValidation = remember { mutableStateOf(false) }
    val whiteAlpha = if(isOnSwipeValidation.value) 0.1f else 0f
    val currentScreen by model.currentScreen.observeAsState()

    UpdateStatusBarColorBasedOnDragState(isDraggable = isOnSwipeValidation, whiteAlpha = whiteAlpha)
    UpdateNavigationBarColorBasedOnDragState(isDraggable = isOnSwipeValidation, whiteAlpha = whiteAlpha)

    val windowBackground = getAttributeColor(
        context = context,
        attr = android.R.attr.windowBackground)

    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(lightenColor(windowBackground, whiteAlpha))
    ) {

        Column (
            modifier = Modifier
                .fillMaxSize()
        ) {

            ReviewTxActivityContentHeader(
                model = model,
                activity = activity,
                whiteAlpha = whiteAlpha,
                screen = currentScreen)

            when (currentScreen) {

                EnumReviewScreen.TX_PREVIEW -> PreviewTx(model = model, activity = activity)

                EnumReviewScreen.TX_ALERT -> ReviewTxAlert(model = model, activity = activity)

                EnumReviewScreen.TX_INFO ->
                    ReviewTxInfo(
                        model = model,
                        activity = activity,
                        whiteAlpha = whiteAlpha,
                        isOnSwipeValidation = isOnSwipeValidation)

                else -> throw RuntimeException(format("unknown %s value in EnumReviewScreen", currentScreen))
            }

        }
    }
}

@Composable
private fun ReviewTxInfo(
    model: ReviewTxModel,
    activity: SamouraiActivity?,
    whiteAlpha: Float,
    isOnSwipeValidation: MutableState<Boolean>
) {

    val verticalScroll = rememberScrollState(0)
    val impliedSendType by model.impliedSendType.observeAsState()

    val destinationAmount by model.impliedAmount.observeAsState()
    val feeAggregated by model.feeAggregated.observeAsState()
    val txData by model.txData.observeAsState()
    val totalAmountToSpend = (destinationAmount ?: 0L) + (feeAggregated ?: 0L)
    val amountToLeaveWallet = Math.min(totalAmountToSpend, txData!!.totalAmountInTxInput)
    val amountToLeaveWalletColor =
        if (amountToLeaveWallet < totalAmountToSpend) samouraiAlerts else Color.White

    val scope = rememberCoroutineScope()

    val swipeSendButtonContentListener =
        SwipeSendButtonListener { state ->

            scope.launch {
                val lightenColor =
                    state == EnumSwipeSendButtonState.IS_SWIPING_ENABLED ||
                            state == EnumSwipeSendButtonState.DONE
                isOnSwipeValidation.value = lightenColor
            }
        }

    val sendTx: () -> Unit = {
        try {
            model.sendType.broadcastTx(model, activity)
        } catch (e : Exception) {
            Log.e(TAG, e.message, e)
            Toast.makeText(activity, format("issue when broadcasting %s transaction", model.sendType.type), Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .verticalScroll(
                    state = verticalScroll,
                    enabled = true,
                )
                .fillMaxSize()
        ) {
            Box {
                ReviewTxActivityContentDestination(
                    model = model,
                    whiteAlpha = whiteAlpha
                )
            }
            ReviewTxActivityContentFees(model = model, activity = activity, whiteAlpha = whiteAlpha)
            ReviewTxActivityContentTransaction(model = model, whiteAlpha = whiteAlpha)
            ReviewTxActivityContentAlert(model = model, whiteAlpha = whiteAlpha)
            Column(
                modifier = Modifier
                    .padding(top = 4.dp)
            ) {
                ReviewTxActivityContentSendNote(model = model)
            }
            Spacer(modifier = Modifier
                .height(115.dp)
                .background(Color.Yellow))
        }

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (impliedSendType == EnumSendType.SPEND_JOINBOT) {
                    JoinbotSendButton(
                        isOnSwipeValidation = isOnSwipeValidation,
                        action = sendTx,
                        alphaBackground = 0f
                    )
                } else {
                    SwipeSendButtonContent(
                        amountToLeaveWallet = MutableLiveData(amountToLeaveWallet),
                        amountToLeaveWalletColor = amountToLeaveWalletColor,
                        action = sendTx,
                        enable = MutableLiveData(true),
                        listener = swipeSendButtonContentListener,
                        alphaBackground = 0f
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewTxActivityContentHeader(
    model: ReviewTxModel,
    activity: SamouraiActivity?,
    whiteAlpha: Float,
    screen: EnumReviewScreen?
) {

    val impliedSendType by model.impliedSendType.observeAsState()

    val account = if (nonNull(activity)) activity!!.getIntent().extras!!.getInt("_account") else 0
    val backgroundColor = if (account == SamouraiAccountIndex.POSTMIX) samouraiPostmixSpendBlueButton else samouraiSlateGreyAccent
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .background(lightenColor(backgroundColor, whiteAlpha))
    ){

        Column {
            IconButton(onClick = {
                when(screen) {
                    EnumReviewScreen.TX_PREVIEW -> model.currentScreen.postValue(
                        EnumReviewScreen.TX_INFO)
                    EnumReviewScreen.TX_ALERT -> model.currentScreen.postValue(
                        EnumReviewScreen.TX_INFO)
                    EnumReviewScreen.TX_INFO -> activity!!.onBackPressed()
                    else -> throw RuntimeException(format("unknown %s value in EnumReviewScreen", screen))
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
        Column (
            modifier = Modifier.weight(1f)
        ) {}
        Row {
            if (screen == EnumReviewScreen.TX_PREVIEW) {
                if (impliedSendType == EnumSendType.SPEND_BOLTZMANN) {
                    IconButton(onClick = {
                        model.recomposeStonewall()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                } else if (impliedSendType!!.isCustomSelection) {
                    IconButton(onClick = {
                        showBottomSheet(type = FILTER_UTXO, model = model, activity = activity)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_filter_list),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                IconButton(onClick = {
                    showBottomSheet(type = PREVIEW_TX, model = model, activity = activity)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_receipt_text_edit_outline),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

    }
}

@Composable
fun ReviewTxActivityContentDestination(
    model: ReviewTxModel,
    whiteAlpha: Float
) {

    val destinationAmount by model.impliedAmount.observeAsState()

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )
    val robotoMonoNormalFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Normal)
    )
    val robotoMonoBoldFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Bold)
    )

    var expand by remember { mutableStateOf(false) }

    Box (
        modifier = Modifier
            .padding(bottom = 8.dp, top = 16.dp)
            .background(lightenColor(samouraiLightGreyAccent, whiteAlpha), RoundedCornerShape(6.dp))
    ) {
        Row (
            modifier = Modifier
                .padding(all = 12.dp)
        ) {
            Column (
                modifier = Modifier
                    .padding(end = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right_top),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            Column {
                Row (
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                ) {
                    Column {
                        Text(
                            text = if (model.sendType.isBatchSpend)
                                "Batch transaction total" else "Destination",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = robotoMediumBoldFont
                        )
                    }
                    Column (
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = FormatsUtil.formatBTC(destinationAmount),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = robotoMonoBoldFont
                        )
                    }
                }
                if (model.sendType.isBatchSpend) {
                    val batchSpendList = ListUtils.emptyIfNull(BatchSendUtil.getInstance().sends)
                    if (batchSpendList.isEmpty()) return
                    if (expand) {
                        for (spend in batchSpendList) {
                            TransactionOutput(
                                spendInfo = spend,
                                model = model
                            )
                        }
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expand = false
                                },
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "view less",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumBoldFont
                            )
                        }
                    } else {
                        TransactionOutput(
                            spendInfo = batchSpendList.get(0),
                            model = model
                        )
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expand = true
                                },
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = format("view %s more", batchSpendList.size - 1),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumBoldFont
                            )
                        }
                    }
                } else {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column {
                            Text(
                                text = model.addressLabel,
                                color = samouraiTextLightGrey,
                                fontSize = 14.sp,
                                fontFamily = robotoMonoNormalFont
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionOutput(
    spendInfo: BatchSendUtil.BatchSend,
    model: ReviewTxModel) {

    val robotoMonoBoldFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Bold)
    )

    val alertReused = model.alertReviews.value!!.get(EnumTxAlert.REUSED_SENDING_ADDRESS)
    val isReusedAddr = if (nonNull(alertReused)) alertReused!!.isReusedAddress(spendInfo.addr) else false

    Row (
        modifier = Modifier
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column (
            modifier = Modifier
                .weight(1f),
        ) {
            DisplayAddress(
                address = spendInfo.captionDestination(),
                addressReused = isReusedAddr
            )
        }
        Column (
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = FormatsUtil.formatBTC(spendInfo.amount),
                color = samouraiTextLightGrey,
                fontSize = 12.sp,
                fontFamily = robotoMonoBoldFont
            )
        }
    }
}

@Composable
fun DisplayAddress(
    address: String,
    addressReused: Boolean) {

    val robotoMonoNormalFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Normal)
    )

    if (addressReused) {

        val inlineContentId = address + "inlineIcon"
        val annotatedString = buildAnnotatedString {
            append(address)
            append(SPACE)
            appendInlineContent(inlineContentId, "[icon]")
            append("Reused sending address")
        }

        val inlineContent = mapOf(
            Pair(
                inlineContentId,
                InlineTextContent(
                    Placeholder(
                        width = 18.sp,
                        height = 18.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_alert_outline_red),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                }
            )
        )
        Text(
            text = annotatedString,
            inlineContent = inlineContent,
            color = samouraiTextLightGrey,
            fontSize = 12.sp,
            fontFamily = robotoMonoNormalFont,
            softWrap = true,
            maxLines = 3
        )

    } else {
        Text(
            text = address,
            color = samouraiTextLightGrey,
            fontSize = 12.sp,
            fontFamily = robotoMonoNormalFont,
            softWrap = true,
            maxLines = 3
        )
    }
}

@Composable
fun ReviewTxActivityContentFees(model : ReviewTxModel,
                                activity: SamouraiActivity?,
                                whiteAlpha: Float) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )
    val robotoItalicBoldFont = FontFamily(
        Font(R.font.roboto_italic, FontWeight.Bold)
    )
    val robotoMonoBoldFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Bold)
    )

    val feeRate by model.minerFeeRates.observeAsState()
    val fees by model.fees.observeAsState()
    val feeAggregated by model.feeAggregated.observeAsState()
    val transactionPriorityRequested by model.transactionPriorityRequested.observeAsState()

    val coroutineScope = rememberCoroutineScope()

    Box (
        modifier = Modifier
            .padding(bottom = 8.dp, top = 8.dp)
            .background(lightenColor(samouraiLightGreyAccent, whiteAlpha), RoundedCornerShape(6.dp))
    ) {
        Row (
            modifier = Modifier
                .padding(all = 12.dp)
        ) {
            Column (
                modifier = Modifier
                    .padding(end = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_motion),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            Column {
                Row (
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                ) {
                    Column {
                        Text(
                            text = "Fees",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = robotoMediumBoldFont
                        )
                    }
                    Column (
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = FormatsUtil.formatBTC(feeAggregated),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = robotoMonoBoldFont
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row (
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Row (
                            modifier = Modifier
                                .weight(0.38f)
                        ) {
                            Text(
                                text = "Miner Fee",
                                color = samouraiTextLightGrey,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumBoldFont
                            )
                        }

                        Row (
                            modifier = Modifier
                                .weight(1f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = format("%s sat/vB", feeRate),
                                color = samouraiTextLightGrey,
                                fontSize = 12.sp,
                                fontFamily = robotoItalicBoldFont
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (nonNull(transactionPriorityRequested))
                                    transactionPriorityRequested!!.getCaption(FeeUtil.getInstance().feeRepresentation)
                                else "Custom",
                                color = samouraiTextLightGrey,
                                fontSize = 12.sp,
                                fontFamily = robotoMonoBoldFont
                            )
                        }

                    }
                    Column (
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .weight(0.5f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = FormatsUtil.formatBTC(fees!!.get("miner") ?: 0L),
                            color = samouraiTextLightGrey,
                            fontSize = 12.sp,
                            fontFamily = robotoMonoBoldFont
                        )
                    }
                }
                if (fees!!.size > 1) {
                    val allFeeNames: List<String> = ImmutableList.copyOf(fees!!.keys)
                    for (i in 1 until allFeeNames.size) {
                        val name = allFeeNames.get(i)
                        val values = fees!!.get(name);
                        Row (
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                        ) {
                            Row {
                                Text(
                                    text = name,
                                    color = samouraiTextLightGrey,
                                    fontSize = 12.sp,
                                    fontFamily = robotoMediumBoldFont
                                )
                            }
                            Column (
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .weight(1f),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = FormatsUtil.formatBTC(values),
                                    color = samouraiTextLightGrey,
                                    fontSize = 12.sp,
                                    fontFamily = robotoMonoBoldFont
                                )
                            }
                        }
                    }
                }
                Row (
                    modifier = Modifier
                        .padding(top = 12.dp)
                ) {
                    Column (
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row (
                            modifier = Modifier
                                .clickable {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        model.refreshFees {}
                                    }
                                    showBottomSheet(
                                        type = MANAGE_FEE,
                                        model = model,
                                        activity = activity)
                                }
                        ) {
                            Text(
                                text = "MANAGE",
                                color = samouraiBlueButton,
                                fontSize = 14.sp,
                                fontFamily = robotoMediumBoldFont
                            )
                        }

                    }
                }
            }
        }
    }
}

fun showBottomSheet(type: ReviewSheetType, model: ReviewTxModel, activity: SamouraiActivity?) {
    ReviewTxBottomSheet(model = model, type = type)
        .apply {
            show(activity!!.supportFragmentManager, this.tag)
        }
}

@Composable
fun ReviewTxActivityContentAlert(model: ReviewTxModel, whiteAlpha: Float) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )

    val alert by model.alertReviews.observeAsState()
    if (alert!!.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()

    Box (
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, top = 8.dp)
            .background(lightenColor(samouraiAlerts, whiteAlpha), RoundedCornerShape(6.dp))
            .clickable {
                coroutineScope.launch(Dispatchers.Main) {
                    model.currentScreen.postValue(EnumReviewScreen.TX_ALERT)
                }
            }
    ) {
        Row (
            modifier = Modifier
                .padding(all = 12.dp)
        ) {
            Column (
                modifier = Modifier
                    .padding(end = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_receipt_text),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            Column {
                Row (
                    modifier = Modifier
                ) {
                    Column {
                        Text(
                            text = format("Transaction Alert%s (%s)", if (alert!!.size > 1) "s" else EMPTY, alert!!.size),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = robotoMediumBoldFont
                        )
                    }
                    Column (
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "REVIEW",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = robotoMediumBoldFont
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewTxActivityContentTransaction(
    model: ReviewTxModel,
    whiteAlpha: Float,
) {

    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Normal)
    )
    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )

    val impliedSendType by model.impliedSendType.observeAsState()

    val entropy by model.entropy.observeAsState()

    val coroutineScope = rememberCoroutineScope()

    Box (
        modifier = Modifier
            .padding(bottom = 8.dp, top = 8.dp)
            .background(lightenColor(samouraiLightGreyAccent, whiteAlpha), RoundedCornerShape(6.dp))
    ) {
        Row (
            modifier = Modifier
                .padding(all = 12.dp)
        ) {
            Column (
                modifier = Modifier
                    .padding(end = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_receipt_text),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            Column {
                Row (
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                ) {
                    Column {
                        Text(
                            text = "Transaction",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = robotoMediumBoldFont
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                ) {
                    Column {
                        Text(
                            text = "Type",
                            color = samouraiTextLightGrey,
                            fontSize = 12.sp,
                            fontFamily = robotoMediumNormalFont
                        )
                    }
                    Column (
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = impliedSendType!!.caption,
                            color = samouraiTextLightGrey,
                            fontSize = 12.sp,
                            fontFamily = robotoMediumNormalFont
                        )
                    }
                }
                if (impliedSendType == EnumSendType.SPEND_BOLTZMANN) {
                    Row (
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Entropy",
                                color = samouraiTextLightGrey,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumNormalFont
                            )
                        }
                        Column (
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (nonNull(entropy!!.entropy))
                                    DecimalFormat("##.#").format(entropy!!.entropy) + " bits" else "? bits",
                                color = samouraiTextLightGrey,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumNormalFont
                            )
                        }
                    }
                    Row (
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Interpretations",
                                color = samouraiTextLightGrey,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumNormalFont
                            )
                        }
                        Column (
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (nonNull(entropy!!.interpretations))
                                    Objects.toString(entropy!!.interpretations) else "?",
                                color = samouraiTextLightGrey,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumNormalFont
                            )
                        }
                    }
                }
                if (nonNull(impliedSendType!!.coinSelectionType)) {
                    Row (
                        modifier = Modifier
                            .padding(top = 12.dp)
                    ) {
                        Column (
                            modifier = Modifier
                                .weight(1f),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row (
                                modifier = Modifier.clickable {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        model.currentScreen.postValue(EnumReviewScreen.TX_PREVIEW)
                                    }
                                }
                            ) {
                                Text(
                                    text = "PREVIEW",
                                    color = samouraiBlueButton,
                                    fontSize = 14.sp,
                                    fontFamily = robotoMediumBoldFont
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewTxActivityContentSendNote(model: ReviewTxModel) {

    val robotoItalicNormalFont = FontFamily(
        Font(R.font.roboto_italic, FontWeight.Normal)
    )
    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Normal)
    )
    val textColorDarkGray = Color(95, 95, 95)

    var fieldNote by remember { mutableStateOf(model.txNote.value!!) }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val viewRequester = remember { BringIntoViewRequester() }
    val imeState = rememberImeState()

    LaunchedEffect(key1 = imeState.value) {
        if (! imeState.value) {
            focusManager.clearFocus(true)
        }
    }

    Box {
        Row (
            modifier = Modifier
                .padding(all = 12.dp)
        ) {
            Column (
                modifier = Modifier
                    .padding(end = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_note),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            Column {
                Row (
                    modifier = Modifier
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(start = 6.dp),
                                text = if (isBlank(fieldNote)) "Add a note to this transaction" else "",
                                color = textColorDarkGray,
                                fontSize = 14.sp,
                                fontFamily = robotoItalicNormalFont
                            )
                            BasicTextField(
                                value = fieldNote,
                                onValueChange = {
                                    fieldNote = it
                                    model.setTxNote(it)
                                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            var tryDone = 0
                                            val pauseMs = 30L
                                            coroutineScope.launch {
                                                while (tryDone < 4) {
                                                    delay(pauseMs * (tryDone + 1))
                                                    viewRequester.bringIntoView()
                                                    ++tryDone;
                                                }
                                            }
                                        }
                                    },
                                textStyle = LocalTextStyle.current.copy(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = robotoMediumNormalFont
                                ),
                                cursorBrush = SolidColor(Color.White),
                                singleLine = true,
                            )
                        }
                        Divider(
                            color = textColorDarkGray,
                            thickness = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .bringIntoViewRequester(viewRequester),
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
fun JoinbotSendButton(
    isOnSwipeValidation: MutableState<Boolean>,
    action: () -> Unit,
    alphaBackground: Float,
) {

    val buttonSize = 48.dp

    var dragOffset by remember { mutableStateOf(IntOffset(0, 0)) }

    Box (
        modifier = if (isOnSwipeValidation.value)
            Modifier
                .padding(bottom = 8.dp, top = 8.dp, start = 16.dp, end = 16.dp)
                .background(
                    samouraiLightGreyAccent.copy(alphaBackground),
                    RoundedCornerShape(20.dp)
                ) else
                    Modifier
                        .padding(bottom = 8.dp, top = 8.dp, start = 16.dp, end = 16.dp)
    ) {

        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {

            Box (
                modifier = Modifier
                    .size(buttonSize)
                    .offset { dragOffset }
            ) {
                Row (
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    IconButton(
                        onClick = action,
                        modifier = Modifier
                            .size(buttonSize)
                            .background(Color.White, CircleShape)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_joinbot_send),
                            contentDescription = null,
                            tint = Color(52, 52, 52, 255)
                        )
                    }

                }
            }
        }
    }
}

@Composable
fun UpdateStatusBarColorBasedOnDragState(
    isDraggable: MutableState<Boolean>,
    whiteAlpha: Float
) {
    val context = LocalContext.current
    val navigationBarColor = getAttributeColor(context = context, attr = android.R.attr.statusBarColor)

    LaunchedEffect(isDraggable.value) {
        val activity = context as? SamouraiActivity
        activity?.window?.statusBarColor = lightenColor(navigationBarColor, whiteAlpha).toArgb()
    }
}

@Composable
fun UpdateNavigationBarColorBasedOnDragState(
    isDraggable: MutableState<Boolean>,
    whiteAlpha: Float
) {
    val context = LocalContext.current
    val navigationBarColor = getAttributeColor(context = context, attr = android.R.attr.navigationBarColor)

    LaunchedEffect(isDraggable.value) {
        val activity = context as? SamouraiActivity
        activity?.window?.navigationBarColor = lightenColor(navigationBarColor, whiteAlpha).toArgb()
    }
}

@Preview(showBackground = true, heightDp = 100, widthDp = 420)
@Composable
fun TransactionOutputPreview(
    @PreviewParameter(MyModelPreviewProvider::class) reviewTxModel: ReviewTxModel
) {

    val spend = BatchSendUtil.BatchSend()
    spend.addr = "this is address"
    spend.amount = 123456

    TransactionOutput(
        spendInfo = spend,
        model = reviewTxModel
    )
}

@Preview(showBackground = true, heightDp = 780, widthDp = 420)
@Composable
fun DefaultPreview(
    @PreviewParameter(MyModelPreviewProvider::class) reviewTxModel: ReviewTxModel
) {
    ReviewTxActivityContent(
        model = reviewTxModel,
        activity = null
    )
}

class MyModelPreviewProvider : PreviewParameterProvider<ReviewTxModel> {
    override val values: Sequence<ReviewTxModel>
        get() = sequenceOf(ReviewTxModel(TestApplication()))
}
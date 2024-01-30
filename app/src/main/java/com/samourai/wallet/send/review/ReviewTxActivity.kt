package com.samourai.wallet.send.review

import android.os.Build
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.collect.ImmutableList
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.home.TestApplication
import com.samourai.wallet.send.review.ReviewTxActivity.Companion.TAG
import com.samourai.wallet.theme.samouraiBlueButton
import com.samourai.wallet.theme.samouraiLightGreyAccent
import com.samourai.wallet.theme.samouraiPostmixSpendBlueButton
import com.samourai.wallet.theme.samouraiSlateGreyAccent
import com.samourai.wallet.theme.samouraiSuccess
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.view.rememberImeState
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

        val account = intent.extras!!.getInt("_account")
        val type = intent.getIntExtra("sendType", 0)

        reviewTxModel
            .setAccount(account)
            .setAddress(intent.getStringExtra("sendAddress"))
            .setAddressLabel(intent.getStringExtra("sendAddressLabel"))
            .setAmount(intent.getLongExtra("sendAmount", 0L))
            .setRicochetStaggeredDelivery(intent.getBooleanExtra("ricochetStaggeredDelivery", false))
            .setType(EnumSendType.fromType(type))

        if (intent.hasExtra("preselected")) {
            reviewTxModel
                .setPreselectedUtxo(intent.getStringExtra("preselected"))
        }

        if (account == SamouraiAccountIndex.POSTMIX) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                window.statusBarColor = resources.getColor(R.color.samourai_blue)
            } else {
                window.statusBarColor = getColor(R.color.samourai_blue)
            }
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                window.statusBarColor = resources.getColor(R.color.grey_accent)
            } else {
                window.statusBarColor = getColor(R.color.grey_accent)
            }
        }

        setContent {
            ReviewTxActivityContent(model = reviewTxModel, activity = this)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewTxActivityContent(model: ReviewTxModel, activity: SamouraiActivity?) {

    Column (
        modifier = Modifier
            .fillMaxSize()
    ) {
        ReviewTxActivityContentHeader(activity = activity)
        ReviewTxActivityContentDestination(model = model, activity = activity)
        ReviewTxActivityContentFees(model = model, activity = activity)
        ReviewTxActivityContentTransaction(model = model)
        Column (
            modifier = Modifier
                .padding(top = 24.dp)
                .weight(1f),
        ) {
            ReviewTxActivityContentSendNote(model = model)
        }
        ReviewTxActivityContentSendButton(model = model, activity = activity)
        Box (
            modifier = Modifier
                .height(50.dp)
        ) {}
    }
}

@Composable
fun ReviewTxActivityContentHeader(activity: SamouraiActivity?) {
    val account = if (nonNull(activity)) activity!!.getIntent().extras!!.getInt("_account") else 0
    val backgroundColor = if (account == SamouraiAccountIndex.POSTMIX) samouraiPostmixSpendBlueButton else samouraiSlateGreyAccent
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ){
        IconButton(onClick = { activity!!.onBackPressed() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun ReviewTxActivityContentDestination(model: ReviewTxModel, activity: SamouraiActivity?) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )
    val robotoMonoNormalFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Normal)
    )
    val robotoMonoBoldFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Bold)
    )
    val textColorGray = Color(184, 184, 184)

    Box (
        modifier = Modifier
            .padding(bottom = 9.dp, top = 18.dp, start = 18.dp, end = 18.dp)
            .background(samouraiLightGreyAccent, RoundedCornerShape(6.dp))
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
                        .padding(bottom = 18.dp)
                ) {
                    Column {
                        Text(
                            text = "Destination",
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
                            text = FormatsUtil.formatBTC(model.amount),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = robotoMonoBoldFont
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column (
                    ) {
                        Text(
                            text = model.addressLabel,
                            color = textColorGray,
                            fontSize = 14.sp,
                            fontFamily = robotoMonoNormalFont
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewTxActivityContentFees(model : ReviewTxModel, activity: SamouraiActivity?) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )
    val robotoItalicBoldFont = FontFamily(
        Font(R.font.roboto_italic, FontWeight.Bold)
    )
    val robotoMonoBoldFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Bold)
    )
    val textColorGray = Color(184, 184, 184)

    val feeRate by model.minerFeeRates.observeAsState()
    val fees by model.fees.observeAsState()
    val feeAggregated by model.feeAggregated.observeAsState()

    Box (
        modifier = Modifier
            .padding(bottom = 9.dp, top = 9.dp, start = 18.dp, end = 18.dp)
            .background(samouraiLightGreyAccent, RoundedCornerShape(6.dp))
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
                        .padding(bottom = 18.dp)
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
                        .padding(bottom = 18.dp)
                ) {
                    Row {
                        Text(
                            text = "Miner Fees",
                            color = textColorGray,
                            fontSize = 12.sp,
                            fontFamily = robotoMediumBoldFont
                        )
                        Spacer(modifier = Modifier.size(18.dp))
                        Text(
                            text = format("%s sat/vB", feeRate),
                            color = textColorGray,
                            fontSize = 12.sp,
                            fontFamily = robotoItalicBoldFont
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
                            text = FormatsUtil.formatBTC(fees!!.get("miner") ?: 0L),
                            color = textColorGray,
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
                                .padding(bottom = 18.dp)
                        ) {
                            Row {
                                Text(
                                    text = name,
                                    color = textColorGray,
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
                                    color = textColorGray,
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
                                    showFeeManager(model = model, activity = activity)
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

fun showFeeManager(model: ReviewTxModel, activity: SamouraiActivity?) {
    ReviewTxBottomSheet(model = model, type = ReviewTxBottomSheet.ReviewSheetType.MANAGE_FEE)
        .apply {
            show(activity!!.supportFragmentManager, this.tag)
        }
}

@Composable
fun ReviewTxActivityContentTransaction(model: ReviewTxModel) {

    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Normal)
    )
    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )
    val textColorGray = Color(184, 184, 184)

    val impliedSendType = model.impliedSendType.observeAsState()

    val entropy by model.entropy.observeAsState()

    val sendTypesHavingPreview: List<EnumSendType> =
        ImmutableList.of(EnumSendType.SPEND_SIMPLE, EnumSendType.SPEND_BOLTZMANN)

    Box (
        modifier = Modifier
            .padding(bottom = 9.dp, top = 9.dp, start = 18.dp, end = 18.dp)
            .background(samouraiLightGreyAccent, RoundedCornerShape(6.dp))
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
                        .padding(bottom = 18.dp)
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
                        .padding(bottom = 18.dp)
                ) {
                    Column {
                        Text(
                            text = "Type",
                            color = textColorGray,
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
                            text = toSendTypeDesc(impliedSendType.value!!),
                            color = textColorGray,
                            fontSize = 12.sp,
                            fontFamily = robotoMediumNormalFont
                        )
                    }
                }
                if (impliedSendType.value == EnumSendType.SPEND_BOLTZMANN) {
                    Row (
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Entropy",
                                color = textColorGray,
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
                                color = textColorGray,
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
                                color = textColorGray,
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
                                color = textColorGray,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumNormalFont
                            )
                        }
                    }
                }
                if (sendTypesHavingPreview.contains(impliedSendType.value)) {
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

fun toSendTypeDesc(sendType: EnumSendType): String {
    val sendTypeDesc = when (sendType) {
        EnumSendType.SPEND_SIMPLE -> "Simple"
        EnumSendType.SPEND_BOLTZMANN -> "STONEWALL"
        EnumSendType.SPEND_RICOCHET -> "Ricochet"
        EnumSendType.SPEND_JOINBOT -> "Joinbot"
    }
    return sendTypeDesc
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

    Box (
        modifier = Modifier
            .padding(bottom = 9.dp, top = 9.dp, start = 18.dp, end = 18.dp)
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
                    painter = painterResource(id = R.drawable.ic_note),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            Column {
                Row (
                    modifier = Modifier
                        .padding(bottom = 12.dp)
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
                                                    delay(pauseMs * (tryDone+1))
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
fun ReviewTxActivityContentSendButton(model: ReviewTxModel, activity: SamouraiActivity?) {

    val impliedSendType = model.impliedSendType.observeAsState()

    val onClick: () -> Unit = {
        try {
            model.sendType.broadcastTx(model, activity)
        } catch (e : Exception) {
            Log.e(TAG, e.message, e)
            Toast.makeText(activity, format("issue when broadcasting %s transaction", model.sendType.type), Toast.LENGTH_LONG).show()
        }
    }

    Box (
        modifier = Modifier
            .padding(bottom = 9.dp, top = 9.dp, start = 18.dp, end = 18.dp)
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (impliedSendType.value == EnumSendType.SPEND_JOINBOT) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onClick)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_joinbot_send),
                        contentDescription = null,
                        tint = Color(52, 52, 52, 255)
                    )
                }
            } else {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(samouraiSuccess, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onClick)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_send),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }

}

@Preview(showBackground = true, heightDp = 780, widthDp = 420)
@Composable
fun DefaultPreview(
    @PreviewParameter(MyModelPreviewProvider::class) reviewTxModel: ReviewTxModel
) {
    ReviewTxActivityContent(reviewTxModel, null)
}

class MyModelPreviewProvider : PreviewParameterProvider<ReviewTxModel> {
    override val values: Sequence<ReviewTxModel>
        get() = sequenceOf(ReviewTxModel(TestApplication()))
}
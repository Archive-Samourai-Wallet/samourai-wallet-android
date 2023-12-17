package com.samourai.wallet.send.review

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.samourai.wallet.theme.samouraiSlateGreyAccent
import com.samourai.wallet.theme.samouraiSuccess
import com.samourai.wallet.theme.samouraiWindow
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex
import org.apache.commons.lang3.StringUtils.defaultIfBlank
import org.apache.commons.lang3.StringUtils.isBlank
import java.lang.String.format
import java.text.DecimalFormat
import java.util.Objects
import java.util.Objects.nonNull

class ReviewTxActivity : SamouraiActivity() {

    companion object {
        public const val TAG = "ReviewTxActivity"
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

@Composable
fun ReviewTxActivityContent(model: ReviewTxModel, activity: SamouraiActivity?) {
    Surface(
        color = samouraiWindow
    ) {
        Column (
            modifier = Modifier
                .fillMaxSize()
        ) {
            ReviewTxActivityContentHeader(activity = activity)
            ReviewTxActivityContentDestination(activity = activity)
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
}

@Composable
fun ReviewTxActivityContentHeader(activity: SamouraiActivity?) {
    val account = if (nonNull(activity)) activity!!.getIntent().extras!!.getInt("_account") else 0
    val backgroundColor = if (account == SamouraiAccountIndex.POSTMIX) samouraiBlueButton else samouraiSlateGreyAccent
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
fun ReviewTxActivityContentDestination(activity: SamouraiActivity?) {

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

    val amount = if (nonNull(activity)) activity!!.intent.getLongExtra("sendAmount", 0L) else 0L
    val address = if (nonNull(activity)) defaultIfBlank( activity!!.intent.getStringExtra("sendAddressLabel"), "missing address")!! else "missing address"

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
                        .padding(bottom = 12.dp)
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
                            text = FormatsUtil.formatBTC(amount),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = robotoMonoBoldFont
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column (
                    ) {
                        Text(
                            text = address,
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
                        .padding(bottom = 12.dp)
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
                        .padding(bottom = 12.dp)
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
                            text = format("%s sat/b", feeRate),
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
                                .padding(bottom = 12.dp)
                        ) {
                            Row {
                                Text(
                                    text = name,
                                    color = textColorGray,
                                    fontSize = 14.sp,
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
                                    fontSize = 14.sp,
                                    fontFamily = robotoMonoBoldFont
                                )
                            }
                        }
                    }
                }
                Row {
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

    val sendTypeDesc = toSendTypeDesc(model.sendType)

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
                        .padding(bottom = 12.dp)
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
                        .padding(bottom = 12.dp)
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
                            text = sendTypeDesc,
                            color = textColorGray,
                            fontSize = 12.sp,
                            fontFamily = robotoMediumNormalFont
                        )
                    }
                }
                if (model.sendType == EnumSendType.SPEND_BOLTZMANN) {
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
                if (sendTypesHavingPreview.contains(model.sendType)) {
                    Row {
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

@Composable
fun ReviewTxActivityContentSendNote(model: ReviewTxModel) {

    val robotoItalicNormalFont = FontFamily(
        Font(R.font.roboto_italic, FontWeight.Normal)
    )
    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Normal)
    )
    val textColorDarkGray = Color(95, 95, 95)

    val note by model.txNote.observeAsState()

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
                                text = if (isBlank(note!!)) "Add a note to this transaction" else "",
                                color = textColorDarkGray,
                                fontSize = 14.sp,
                                fontFamily = robotoItalicNormalFont
                            )
                            BasicTextField(
                                value = note!!,
                                onValueChange = { model.setTxNote(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    color = textColorDarkGray,
                                    fontSize = 14.sp,
                                    fontFamily = robotoMediumNormalFont
                                ),
                                cursorBrush = SolidColor(Color.White),
                                singleLine = true
                            )
                        }
                        Divider(
                            color = textColorDarkGray,
                            thickness = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewTxActivityContentSendButton(model: ReviewTxModel, activity: SamouraiActivity?) {

    val onClick: () -> Unit = {
        try {
            model.sendType.broadcastTx(model, activity)
        } catch (e : Exception) {
            Log.e(TAG, e.message, e)
            //todo
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
            if (model.sendType == EnumSendType.SPEND_JOINBOT) {
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
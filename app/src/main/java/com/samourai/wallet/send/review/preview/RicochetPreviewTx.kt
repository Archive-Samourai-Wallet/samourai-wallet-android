package com.samourai.wallet.send.review.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.send.review.MyModelPreviewProvider
import com.samourai.wallet.send.review.ReviewTxBottomSheet.ReviewSheetType.MANAGE_FEE
import com.samourai.wallet.send.review.ReviewTxModel
import com.samourai.wallet.send.review.ReviewTxModel.MINER
import com.samourai.wallet.send.review.ReviewTxModel.RICOCHET_FEE
import com.samourai.wallet.send.review.showBottomSheet
import com.samourai.wallet.theme.samouraiLightGreyAccent
import com.samourai.wallet.theme.samouraiTextLightGrey
import com.samourai.wallet.theme.samouraiWindow
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.func.MyTransactionOutPointAmountComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils.EMPTY
import java.lang.String.format

@Composable
fun RicochetPreviewTx(model: ReviewTxModel, activity: SamouraiActivity?) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )

    val impliedSendType by model.impliedSendType.observeAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = impliedSendType!!.coinSelectionType.caption,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = robotoMediumBoldFont
            )

            Column (
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column (modifier = Modifier.weight(0.5f, false)) {
                    RicochetPreviewTxInput(model = model, activity = activity)
                }
                Column (modifier = Modifier.weight(0.5f, true)) {
                    RicochetPreviewTxOutput(model = model, activity = activity)
                }
            }
        }
    }
}

@Composable
fun RicochetPreviewTxInput(
    model: ReviewTxModel,
    activity: SamouraiActivity?) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )

    val verticalScroll = rememberScrollState(0)

    val txData by model.txData.observeAsState()
    val selectedUTXOPointCount = txData!!.selectedUTXOPoints.size

    Box (
        modifier = Modifier
            .background(samouraiLightGreyAccent, RoundedCornerShape(6.dp))
    ) {
        Column (
            modifier = Modifier
                .padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = format("Input%s (%s)", if (selectedUTXOPointCount > 1) "s" else EMPTY, selectedUTXOPointCount),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumBoldFont
                )
            }
            Column (
                modifier = Modifier
                    .verticalScroll(
                        state = verticalScroll,
                        enabled = true,
                    ),
            ) {
                for (utxoPoint in txData!!.selectedUTXOPoints.sortedWith(MyTransactionOutPointAmountComparator(true))) {
                    DisplayUtxoOutPoint(
                        model = model,
                        activity = activity,
                        utxoOutPoint = utxoPoint,
                        selected = true)
                }
            }
        }
    }
}

@Composable
fun RicochetPreviewTxOutput(model: ReviewTxModel, activity: SamouraiActivity?) {

    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Normal)
    )
    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )
    val robotoMonoBoldFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Bold)
    )

    val fees by model.fees.observeAsState()
    val destinationAmount by model.impliedAmount.observeAsState()
    val txData by model.txData.observeAsState()

    val coroutineScope = rememberCoroutineScope()

    val verticalScroll = rememberScrollState(0)

    Box (
        modifier = Modifier
            .background(samouraiLightGreyAccent, RoundedCornerShape(6.dp))
    ) {
        Column (
            modifier =  Modifier
                .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = if (txData!!.change > 0L) "Outputs (3)" else "Output (2)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumBoldFont
                )
            }
            Column (
                modifier = Modifier
                    .verticalScroll(
                        state = verticalScroll,
                        enabled = true,
                    ),
            ) {
                Row (
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right_top),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Column (
                        modifier = Modifier
                            .weight(0.7f),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = "Spend destination (at hop ${txData!!.ricochetTransactionInfo?.nbHops()})",
                            maxLines = 1,
                            color = samouraiTextLightGrey,
                            fontSize = 12.sp,
                            fontFamily = robotoMonoBoldFont
                        )
                    }
                    Column (
                        modifier = Modifier
                            .weight(0.35f),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = FormatsUtil.formatBTC(destinationAmount),
                            maxLines = 1,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = robotoMediumNormalFont
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .padding(top = 6.dp, bottom = 6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left_bottom),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                    Column (
                        modifier = Modifier,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = "Returned to wallet",
                            color = samouraiTextLightGrey,
                            fontSize = 12.sp,
                            fontFamily = robotoMonoBoldFont
                        )
                    }
                    Column (
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = FormatsUtil.formatBTC(txData!!.change.coerceAtLeast(0L)),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = robotoMediumNormalFont
                        )
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch(Dispatchers.IO) {
                                model.refreshFees {}
                            }
                            showBottomSheet(type = MANAGE_FEE, model = model, activity = activity)
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row (
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_motion),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                        Column (
                            modifier = Modifier,
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                text = "Miner fee",
                                color = samouraiTextLightGrey,
                                fontSize = 12.sp,
                                fontFamily = robotoMonoBoldFont
                            )
                        }
                        Column (
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = FormatsUtil.formatBTC(fees!!.get(MINER) ?: 0L),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumNormalFont
                            )
                        }
                    }
                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row (
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_motion),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                        Column (
                            modifier = Modifier,
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                text = "Richochet fee",
                                color = samouraiTextLightGrey,
                                fontSize = 12.sp,
                                fontFamily = robotoMonoBoldFont
                            )
                        }
                        Column (
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = FormatsUtil.formatBTC(fees!!.get(RICOCHET_FEE)?:0),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = robotoMediumNormalFont
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(heightDp = 780, widthDp = 420)
@Composable
fun DefaultRicochetPreviewTx(
    @PreviewParameter(MyModelPreviewProvider::class) reviewTxModel: ReviewTxModel
) {
    Box(modifier = Modifier.background(samouraiWindow)) {
        RicochetPreviewTx(model = reviewTxModel, activity = null)
    }
}
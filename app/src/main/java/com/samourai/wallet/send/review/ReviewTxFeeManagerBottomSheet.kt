package com.samourai.wallet.send.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samourai.wallet.R
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.theme.samouraiAccent
import com.samourai.wallet.theme.samouraiBottomSheetBackground
import com.samourai.wallet.theme.samouraiDarkGray
import com.samourai.wallet.tools.WrapToolsPageAnimation
import com.samourai.wallet.util.func.FormatsUtil


@Composable
fun ReviewTxFeeManager(model: ReviewTxModel) {

    Box(
    ) {
        WrapToolsPageAnimation(
            visible = true,
        ) {
            Column (
                Modifier
                    .background(samouraiBottomSheetBackground)
            ) {
                TopAppBar(
                    elevation = 0.dp,
                    backgroundColor = samouraiBottomSheetBackground,
                    title = {
                        Text(
                            text = "Manage transaction priority", fontSize = 13.sp,
                            color = samouraiAccent
                        )
                    },
                )
                Body(model = model)
            }
        }
    }

}

@Composable
fun Body(model: ReviewTxModel) {

    val maxSatByKB by model.feeMaxRate.observeAsState()
    var sliderPosition by remember { mutableStateOf(model.minerFeeRates.value!!.toFloat()) }

    val minerFeeRates by model.minerFeeRates.observeAsState()
    val transactionPriority by model.transactionPriority.observeAsState()

    val fees by model.fees.observeAsState()

    val colorGreenUI2 = colorResource(id = R.color.green_ui_2)
    val colorGrayUI2 = colorResource(id = R.color.gray_ui_2)

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )
    val robotoItalicBoldFont = FontFamily(
        Font(R.font.roboto_italic, FontWeight.Bold)
    )
    val robotoMonoNormalFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Normal)
    )
    val robotoMonoBoldFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Bold)
    )
    val textColorGray = Color(184, 184, 184)

    Column (
        modifier = Modifier
            .padding(top = 9.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ){
            Button(
                onClick = {
                    val feeRate = model.feeLowRate.value!!
                    sliderPosition = feeRate.toFloat()
                    model.setMinerFeeRatesAndComputeFees(feeRate)
                          },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = if (model.feeLowRate.value == minerFeeRates) samouraiAccent else samouraiDarkGray,
                    contentColor = Color.White
                ),
            ) {
                Text(text = EnumTransactionPriority.LOW.getCaption(FeeUtil.getInstance().feeRepresentation))
            }
            Button(
                onClick = {
                    val feeRate = model.feeMedRate.value!!
                    sliderPosition = feeRate.toFloat()
                    model.setMinerFeeRatesAndComputeFees(feeRate)
                          },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = if (model.feeMedRate.value == minerFeeRates) samouraiAccent else samouraiDarkGray,
                    contentColor = Color.White
                ),
            ) {
                Text(text = EnumTransactionPriority.NORMAL.getCaption(FeeUtil.getInstance().feeRepresentation))
            }
            Button(
                onClick = {
                    val feeRate = model.feeHighRate.value!!
                    sliderPosition = feeRate.toFloat()
                    model.setMinerFeeRatesAndComputeFees(feeRate)
                          },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = if (model.feeHighRate.value == minerFeeRates) samouraiAccent else samouraiDarkGray,
                    contentColor = Color.White
                ),
            ) {
                Text(text = EnumTransactionPriority.NEXT_BLOCK.getCaption(FeeUtil.getInstance().feeRepresentation))
            }
        }
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                modifier = Modifier
                    .weight(0.77f),
                value = sliderPosition,
                valueRange = 1f..maxSatByKB!!.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = colorGreenUI2,
                    activeTrackColor = colorGreenUI2,
                    activeTickColor = colorGreenUI2,
                    inactiveTrackColor = colorGrayUI2,
                    inactiveTickColor = colorGrayUI2
                ),
                onValueChange = {
                    sliderPosition = it
                    model.setMinerFeeRatesAndComputeFeesAsync(it.toLong())
                }
            )
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.23f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = transactionPriority!!.getCaption(FeeUtil.getInstance().feeRepresentation),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumBoldFont
                )
                Text(
                    text = sliderPosition.toLong().toString() + " sat/vB",
                    color = textColorGray,
                    fontSize = 12.sp,
                    fontFamily = robotoMediumBoldFont
                )
            }
        }
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Total miner fee",
                    color = textColorGray,
                    fontSize = 12.sp,
                    fontFamily = robotoMediumBoldFont
                )
                Text(
                    text =  FormatsUtil.formatBTC(fees!!.get("miner") ?: 0L),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMonoBoldFont
                )
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Est confirmation time",
                    color = textColorGray,
                    fontSize = 12.sp,
                    fontFamily = robotoMediumBoldFont
                )
                Text(
                    text = transactionPriority!!.getDescription(FeeUtil.getInstance().feeRepresentation),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumBoldFont
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
fun DefaultPreviewReviewTxFeeManager(
    @PreviewParameter(MyModelPreviewProvider::class) model: ReviewTxModel
) {
    ReviewTxFeeManager(model)
}
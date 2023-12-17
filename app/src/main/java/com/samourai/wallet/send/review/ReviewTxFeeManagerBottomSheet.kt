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

    val sliderValue by model.minerFeeRates.observeAsState()

    val transactionPriority by model.transactionPriority.observeAsState()

    val maxSatByKB = computeMaxSatByKB()
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
                onClick = { model.setMinerFeeRates(model.feeLowRate.value!!)},
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = samouraiAccent,
                    contentColor = Color.White
                ),
            ) {
                Text(text = "Low")
            }
            Button(
                onClick = { model.setMinerFeeRates(model.feeMedRate.value!!) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = samouraiAccent,
                    contentColor = Color.White
                ),
            ) {
                Text(text = "Normal")
            }
            Button(
                onClick = { model.setMinerFeeRates(model.feeHighRate.value!!) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = samouraiAccent,
                    contentColor = Color.White
                ),
            ) {
                Text(text = "Next Block")
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
                value = sliderValue!!.toFloat(),
                valueRange = 1f..maxSatByKB,
                colors = SliderDefaults.colors(
                    thumbColor = colorGreenUI2,
                    activeTrackColor = colorGreenUI2,
                    activeTickColor = colorGreenUI2,
                    inactiveTrackColor = colorGrayUI2,
                    inactiveTickColor = colorGrayUI2
                ),
                onValueChange = {
                    model.setMinerFeeRates(it.toLong())
                }
            )
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.23f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = transactionPriority!!.caption,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumBoldFont
                )
                Text(
                    text = sliderValue.toString() + " sat/b",
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
                    text = transactionPriority!!.description,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumBoldFont
                )
            }
        }
    }
}

@Composable
private fun computeMaxSatByKB(): Float {
    val feeHigh = FeeUtil.getInstance().highFee.defaultPerKB.toLong()
    val high = feeHigh.toFloat() / 2 + feeHigh.toFloat()
    val feeHighSliderValue = (high / 1000f).toInt()
    val maxSatByKB = Math.max(1f, feeHighSliderValue + 10f)
    return maxSatByKB
}

@Preview(showBackground = true, widthDp = 420)
@Composable
fun DefaultPreviewReviewTxFeeManager(
    @PreviewParameter(MyModelPreviewProvider::class) model: ReviewTxModel
) {
    ReviewTxFeeManager(model)
}
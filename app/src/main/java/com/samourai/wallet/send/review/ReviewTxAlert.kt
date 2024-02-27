package com.samourai.wallet.send.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.theme.samouraiBlueButton
import com.samourai.wallet.theme.samouraiLightGreyAccent
import com.samourai.wallet.theme.samouraiTextLightGrey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Objects.nonNull

@Composable
fun ReviewTxAlert(model: ReviewTxModel, activity: SamouraiActivity?) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )

    val verticalScroll = rememberScrollState(0)
    val alertReviews by model.alertReviews.observeAsState()

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
                text = if (nonNull(activity)) activity!!.getString(R.string.tx_alert_title) else "Transaction alerts",
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = robotoMediumBoldFont
            )

            Column (
                modifier = Modifier
                    .verticalScroll(
                        state = verticalScroll,
                        enabled = true,
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (alert in alertReviews!!.values) {
                    ReviewTxAlert(model = model, activity = activity, alert = alert)
                }
            }
        }
    }
}

@Composable
private fun ReviewTxAlert(
    model: ReviewTxModel,
    activity: SamouraiActivity?,
    alert: TxAlertReview?) {

    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Normal)
    )
    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )

    val coroutineScope = rememberCoroutineScope()

    Box (
        modifier = Modifier
            .background(samouraiLightGreyAccent, RoundedCornerShape(6.dp))
    ) {
        Column (
            modifier =  Modifier.padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = alert!!.getTitle(activity),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumBoldFont
                )
            }
            Column (
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = alert!!.getExplanation(activity),
                    color = samouraiTextLightGrey,
                    fontSize = 12.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
            if (alert!!.isWithFixSuggestion) {
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
                                    coroutineScope.launch(Dispatchers.Main) {
                                        alert!!.fixAction.call()
                                    }
                                }
                        ) {
                            Text(
                                text = "FIX THIS",
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

@Preview(showBackground = true, heightDp = 780, widthDp = 420)
@Composable
fun DefaultAlertPreview(
    @PreviewParameter(MyModelPreviewProvider::class) reviewTxModel: ReviewTxModel
) {
    ReviewTxAlert(model = reviewTxModel, activity = null)
}
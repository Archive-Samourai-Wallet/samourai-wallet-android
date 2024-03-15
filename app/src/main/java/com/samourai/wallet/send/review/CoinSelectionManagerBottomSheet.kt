package com.samourai.wallet.send.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import com.google.common.collect.ImmutableList
import com.samourai.wallet.R
import com.samourai.wallet.send.review.ref.EnumCoinSelectionType.CUSTOM
import com.samourai.wallet.send.review.ref.EnumCoinSelectionType.SIMPLE
import com.samourai.wallet.send.review.ref.EnumCoinSelectionType.STONEWALL
import com.samourai.wallet.theme.samouraiAccent
import com.samourai.wallet.theme.samouraiBottomSheetBackground
import com.samourai.wallet.theme.samouraiTextLightGrey
import com.samourai.wallet.tools.WrapToolsPageAnimation
import com.samourai.wallet.util.tech.ColorHelper
import java.util.Objects.nonNull


@Composable
fun ReviewTxCoinSelectionManager(model: ReviewTxModel) {
    Box(
    ) {
        WrapToolsPageAnimation(
            visible = true,
        ) {
            Column (
                Modifier
                    .background(samouraiBottomSheetBackground)
            ) {
                Box {
                    TopAppBar(
                        elevation = 0.dp,
                        backgroundColor = samouraiBottomSheetBackground,
                        title = {
                            Text(
                                text = "Coin selection",
                                fontSize = 14.sp,
                                color = samouraiAccent
                            )
                        },
                    )
                }
                ReviewTxCoinSelectionManagerBody(model = model)
            }
        }
    }
}

@Composable
fun ReviewTxCoinSelectionManagerBody(model: ReviewTxModel) {

    val currentSendType by model.impliedSendType.observeAsState()
    val stonewallPossible by model.isStonewallPossible.observeAsState();

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )

    Column (
        modifier = Modifier
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (selectionType in ImmutableList.of(STONEWALL, SIMPLE, CUSTOM)) {
            val enable = selectionType != STONEWALL || stonewallPossible!!
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp),
            ) {
                Column (
                    modifier = Modifier
                        .height(48.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Column {
                        Text(
                            text = selectionType.caption,
                            color = ColorHelper.darkenColor(Color.White, if (enable) 0f else 0.4f),
                            fontSize = 14.sp,
                            fontFamily = robotoMediumBoldFont
                        )
                    }
                    Column (
                    ) {
                        Text(
                            text = selectionType.description,
                            maxLines = 1,
                            color = ColorHelper.darkenColor(samouraiTextLightGrey, if (enable) 0f else 0.4f),
                            fontSize = 12.sp,
                            fontFamily = robotoMediumBoldFont
                        )
                    }
                }
                Row (
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        enabled = enable,
                        selected = selectionType == currentSendType!!.coinSelectionTypeView,
                        colors = RadioButtonDefaults.colors(
                            disabledColor = ColorHelper.darkenColor(Color.LightGray, if (enable) 0f else 0.4f),
                        ),
                        onClick = {
                            val sendType = currentSendType!!.toSelection(selectionType)
                            if (nonNull(sendType)) {
                                model.setType(sendType)
                            }
                        })
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, apiLevel = 33)
@Composable
fun DefaultPreviewReviewTxCoinSelectionManager(
    @PreviewParameter(MyModelPreviewProvider::class) model: ReviewTxModel
) {
    ReviewTxCoinSelectionManager(model)
}
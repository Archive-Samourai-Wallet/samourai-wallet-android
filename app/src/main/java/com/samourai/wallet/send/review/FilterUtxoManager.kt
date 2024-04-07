package com.samourai.wallet.send.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
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
import com.samourai.wallet.R
import com.samourai.wallet.theme.samouraiAccent
import com.samourai.wallet.theme.samouraiBottomSheetBackground
import com.samourai.wallet.theme.samouraiCheckboxBlue
import com.samourai.wallet.tools.WrapToolsPageAnimation

@Composable
fun FilterUtxoManager(model: ReviewTxModel) {
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
                                text = "Filter",
                                fontSize = 14.sp,
                                color = samouraiAccent
                            )
                        },
                    )
                }
                FilterUtxoManagerBody(model = model)
            }
        }
    }
}

@Composable
fun FilterUtxoManagerBody(model: ReviewTxModel) {
    Column (
        modifier = Modifier.padding(all = 26.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        FilterUtxoManagerByAddressType(model = model)
        if (model.isPostmixAccount) {
            FilterUtxoManagerByStatusForPostmixAccount(model = model)
        } else {
            FilterUtxoManagerByStatusForDepositAccount(model = model)
        }
    }
}

@Composable
fun FilterUtxoManagerByAddressType(model: ReviewTxModel) {

    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_regular, FontWeight.Normal)
    )

    val utxoFilterModel by model.utxoFilterModel.observeAsState()

    Column (
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Address Type",
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = robotoMediumNormalFont
        )

        Column (
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = utxoFilterModel!!.isSegwitNative,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.Black,
                        uncheckedColor = samouraiCheckboxBlue,
                        checkedColor = samouraiCheckboxBlue
                    ),
                    onCheckedChange = {
                        model.updateUtxoFilterModel(utxoFilterModel!!.setSegwitNative(it))
                    }
                )
                Text(
                    text = "Segwit native",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = utxoFilterModel!!.isSegwitCompatible,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.Black,
                        uncheckedColor = samouraiCheckboxBlue,
                        checkedColor = samouraiCheckboxBlue
                    ),
                    onCheckedChange = {
                        model.updateUtxoFilterModel(utxoFilterModel!!.setSegwitCompatible(it))
                    }
                )
                Text(
                    text = "Segwit compatibility",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = utxoFilterModel!!.isLegacy,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.Black,
                        uncheckedColor = samouraiCheckboxBlue,
                        checkedColor = samouraiCheckboxBlue
                    ),
                    onCheckedChange = {
                        model.updateUtxoFilterModel(utxoFilterModel!!.setLegacy(it))
                    }
                )
                Text(
                    text = "Legacy",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
        }
    }
}

@Composable
fun FilterUtxoManagerByStatusForDepositAccount(model: ReviewTxModel) {

    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_regular, FontWeight.Normal)
    )

    val utxoFilterModel by model.utxoFilterModel.observeAsState()

    Column (
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Status",
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = robotoMediumNormalFont
        )

        Column (
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = utxoFilterModel!!.isPayNymOutputs,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.Black,
                        uncheckedColor = samouraiCheckboxBlue,
                        checkedColor = samouraiCheckboxBlue
                    ),
                    onCheckedChange = {
                        model.updateUtxoFilterModel(utxoFilterModel!!.setPayNymOutputs(it))
                    }
                )
                Text(
                    text = "PayNym outputs",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = utxoFilterModel!!.isUnmixedToxicChange,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.Black,
                        uncheckedColor = samouraiCheckboxBlue,
                        checkedColor = samouraiCheckboxBlue
                    ),
                    onCheckedChange = {
                        model.updateUtxoFilterModel(utxoFilterModel!!.setUnmixedToxicChange(it))
                    }
                )
                Text(
                    text = "Unmixed toxic change",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = utxoFilterModel!!.isUnconfirmed,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.Black,
                        uncheckedColor = samouraiCheckboxBlue,
                        checkedColor = samouraiCheckboxBlue
                    ),
                    onCheckedChange = {
                        model.updateUtxoFilterModel(utxoFilterModel!!.setUnconfirmed(it))
                    }
                )
                Text(
                    text = "Unconfirmed",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
        }
    }
}

@Composable
fun FilterUtxoManagerByStatusForPostmixAccount(model: ReviewTxModel) {

    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_regular, FontWeight.Normal)
    )

    val utxoFilterModel by model.utxoFilterModel.observeAsState()

    Column (
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Status",
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = robotoMediumNormalFont
        )

        Column (
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = utxoFilterModel!!.isMixedOutputs,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.Black,
                        uncheckedColor = samouraiCheckboxBlue,
                        checkedColor = samouraiCheckboxBlue
                    ),
                    onCheckedChange = {
                        model.updateUtxoFilterModel(utxoFilterModel!!.setMixedOutputs(it))
                    }
                )
                Text(
                    text = "Mixed output",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = utxoFilterModel!!.isPostmixTransactionChange,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.Black,
                        uncheckedColor = samouraiCheckboxBlue,
                        checkedColor = samouraiCheckboxBlue
                    ),
                    onCheckedChange = {
                        model.updateUtxoFilterModel(utxoFilterModel!!.setPostmixTransactionChange(it))
                    }
                )
                Text(
                    text = "Postmix transaction change",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = utxoFilterModel!!.isUnconfirmed,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.Black,
                        uncheckedColor = samouraiCheckboxBlue,
                        checkedColor = samouraiCheckboxBlue
                    ),
                    onCheckedChange = {
                        model.updateUtxoFilterModel(utxoFilterModel!!.setUnconfirmed(it))
                    }
                )
                Text(
                    text = "Unconfirmed",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420, apiLevel = 33)
@Composable
fun DefaultPreviewFilterUtxoManager(
    @PreviewParameter(MyModelPreviewProvider::class) model: ReviewTxModel
) {
    FilterUtxoManager(model)
}
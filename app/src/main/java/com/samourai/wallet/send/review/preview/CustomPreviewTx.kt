package com.samourai.wallet.send.review.preview

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.collect.Lists
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.send.MyTransactionOutPoint
import com.samourai.wallet.send.review.MyModelPreviewProvider
import com.samourai.wallet.send.review.ReviewTxModel
import com.samourai.wallet.send.review.TxData
import com.samourai.wallet.theme.samouraiAlerts
import com.samourai.wallet.theme.samouraiLightGreyAccent
import com.samourai.wallet.theme.samouraiTextLightGrey
import com.samourai.wallet.theme.samouraiWindow
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.func.MyTransactionOutPointAmountComparator
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils.EMPTY
import org.apache.commons.lang3.StringUtils.SPACE
import org.apache.commons.lang3.StringUtils.stripEnd
import org.apache.commons.lang3.StringUtils.stripStart
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.params.TestNet3Params
import java.lang.String.format
import java.math.BigInteger

@Composable
fun CustomPreviewTx(model: ReviewTxModel, activity: SamouraiActivity?) {

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
                    CustomPreviewTxInput(model = model, activity = activity)
                }
                Column (modifier = Modifier.weight(0.15f, false)) {
                    CustomPreviewTxInputTotal(model = model, activity = activity)
                }
                if (impliedSendType!!.isBatchSpend) {
                    Column (modifier = Modifier.weight(0.5f, true)) {
                        BatchPreviewTxOutput(model = model, activity = activity)
                    }
                } else {
                    Column (modifier = Modifier.weight(0.35f, true)) {
                        SimplePreviewTxOutput(model = model, activity = activity)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomPreviewTxInput(
    model: ReviewTxModel,
    activity: SamouraiActivity?
) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )

    val verticalScroll = rememberScrollState(0)

    val txData by model.txData.observeAsState()
    val selectedUTXOPointCount = txData!!.selectedUTXOPoints.size
    val selectedUtxoPointAddresses = txData!!.selectedUTXOPointAddresses

    val allTxOutPoints by model.allSpendableUtxos.observeAsState()
    val customSelectionUtxos by model.customSelectionUtxos.observeAsState()
    if (model.isPostmixAccount && CollectionUtils.size(customSelectionUtxos) > 1) {
        // security to ensure max 1 utxo for custom postmix, should be never used
        model.autoLoadCustomSelectionUtxos(0L)
        model.refreshModel()
    }

    Box {
        Column (
            modifier = Modifier
                .background(samouraiLightGreyAccent, RoundedCornerShape(6.dp))
                .padding(all = 12.dp),
        ) {
            Column (
                modifier = Modifier
                    .padding(bottom = 16.dp),
            ) {
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
                for (utxoPoint in allTxOutPoints!!.sortedWith(MyTransactionOutPointAmountComparator(true))) {
                    DisplayUtxoOutPoint(
                        model = model,
                        activity = activity,
                        utxoOutPoint = utxoPoint,
                        selected = selectedUtxoPointAddresses.contains(TxData.txOutPointId(utxoPoint))
                    )
                }
            }
        }
    }
}

@Composable
fun CustomPreviewTxInputTotal(model: ReviewTxModel, activity: SamouraiActivity?) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )
    val robotoMediumItalicBoldFont = FontFamily(
        Font(R.font.roboto_medium_italic, FontWeight.Bold)
    )
    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Normal)
    )

    val verticalScroll = rememberScrollState(0)

    val txData by model.txData.observeAsState()
    val destinationAmount by model.impliedAmount.observeAsState()
    val feeAggregated by model.feeAggregated.observeAsState()
    val amountToLeaveWallet = (destinationAmount ?: 0L) + (feeAggregated ?: 0L)

    Column (
        modifier = Modifier
            .background(samouraiWindow)
            .padding(start = 32.dp, end = 12.dp, bottom = 12.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ){
        Row (
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total inputs selected",
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = robotoMediumBoldFont
            )
            Column(
                modifier = Modifier
                    .weight(0.35f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = FormatsUtil.formatBTC(txData!!.totalAmountInTxInput),
                    maxLines = 1,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
        }
        Column (
            modifier = Modifier
                .verticalScroll(
                    state = verticalScroll,
                    enabled = true,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (amountToLeaveWallet > txData!!.totalAmountInTxInput) {
                Row (
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Missing",
                        color = samouraiAlerts,
                        fontSize = 14.sp,
                        fontFamily = robotoMediumItalicBoldFont
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.35f),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = FormatsUtil.formatBTC(amountToLeaveWallet - txData!!.totalAmountInTxInput),
                            maxLines = 1,
                            color = samouraiAlerts,
                            fontSize = 12.sp,
                            fontFamily = robotoMediumItalicBoldFont
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayUtxoOutPoint(
    model: ReviewTxModel,
    activity: SamouraiActivity?,
    utxoOutPoint: MyTransactionOutPoint,
    selected: Boolean
) {
    
    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Normal)
    )
    val robotoMonoBoldFont = FontFamily(
        Font(R.font.roboto_mono, FontWeight.Bold)
    )

    val sendType by model.impliedSendType.observeAsState()
    val customSelectionUtxos by model.customSelectionUtxos.observeAsState()

    var textState by remember { mutableStateOf(EMPTY) }
    var displayableTextLength by remember { mutableStateOf(-1) }
    val text = TxData.getNoteOrAddress(utxoOutPoint)
    val textLength = text.length;

    if (displayableTextLength != -1 && textLength > displayableTextLength && textLength > 0) {
        val midIndex = displayableTextLength/2 + (if (displayableTextLength%2 != 0) 1 else 0)
        val firstPart = stripEnd("${text.substring(0, midIndex.coerceAtLeast(0))}", SPACE)
        val secondPart = stripStart("${text.substring((textLength-midIndex+1).coerceAtLeast(0).coerceAtMost(text.length))}", SPACE)
        textState = "${firstPart}…${secondPart}"
    } else {
        textState = text
    }


    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (sendType!!.isCustomSelection) {
            Checkbox(
                checked = selected,
                colors = CheckboxDefaults.colors(
                    checkmarkColor = Color.Black,
                    uncheckedColor = Color.White,
                    checkedColor = Color.White
                ),
                onCheckedChange = {
                    val utxo = com.samourai.wallet.send.UTXO()
                    utxo.outpoints = Lists.newArrayList(utxoOutPoint);
                    if (! selected) {
                        if (!model.isPostmixAccount || CollectionUtils.isEmpty(customSelectionUtxos)) {
                            model.addCustomSelectionUtxos(Lists.newArrayList(utxo))
                        } else {
                            Toast.makeText(activity, "Only 1 input is allowed from Postmix account", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        model.removeCustomSelectionUtxos(Lists.newArrayList(utxo))
                    }
                    model.refreshModel()

                }
            )
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        Row (
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (TxData.hasNote(utxoOutPoint)) {
                Column {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_note),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.65f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = textState,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = samouraiTextLightGrey,
                    fontSize = 12.sp,
                    fontFamily = robotoMonoBoldFont,
                    onTextLayout = { textLayoutResult ->
                        /**
                         * Here we want manage the text too long (overflow).
                         * Android SDK manage only this case (displaying first part) : "too long text is…"
                         * Here we want to display the first part and last part : "too long…also good"
                         */
                        if (textLayoutResult.hasVisualOverflow) {

                            displayableTextLength =
                                textLayoutResult.getWordBoundary(textState.length).start
                            val midIndex = displayableTextLength/2 + (if (displayableTextLength%2 != 0) 1 else 0)

                            val firstPart = stripEnd("${textState.substring(0, midIndex.coerceAtLeast(0))}", SPACE)
                            val secondPart = stripStart("${textState.substring((textLength-midIndex+1).coerceAtLeast(0).coerceAtMost(textState.length))}", SPACE)
                            textState = "${firstPart}…${secondPart}"
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.35f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = FormatsUtil.formatBTC(utxoOutPoint.value.value),
                    maxLines = 1,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = robotoMediumNormalFont
                )
            }
        }
    }
}

@Preview(heightDp = 480, widthDp = 420)
@Composable
fun DefaultCustomPreviewTx(
    @PreviewParameter(MyModelPreviewProvider::class) reviewTxModel: ReviewTxModel
) {
    Box(modifier = Modifier.background(samouraiWindow)) {
        CustomPreviewTx(model = reviewTxModel, activity = null)
    }
}

@Preview(heightDp = 100, widthDp = 420)
@Composable
fun DisplayUtxoOutPointPreview(
    @PreviewParameter(MyModelPreviewProvider::class) reviewTxModel: ReviewTxModel
) {
    Box(modifier = Modifier.background(samouraiWindow)) {
        DisplayUtxoOutPoint(
            model = reviewTxModel,
            activity = null,
            utxoOutPoint = MyTransactionOutPoint(
                TestNet3Params.get(),
                Sha256Hash.ZERO_HASH,
                0,
                BigInteger.valueOf(120000), ByteArray(0),
                "THIS IS ADDRESS WHICH IS TOO LONG TO DISPLAY IT ENTIERELY",
                0
            ),
            selected = true
        )
    }
}
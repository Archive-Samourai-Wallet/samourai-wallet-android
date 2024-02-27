package com.samourai.wallet.send.review.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.send.review.MyModelPreviewProvider
import com.samourai.wallet.send.review.ReviewTxModel
import com.samourai.wallet.send.review.ref.EnumSendType

/**
 * TODO :
 * - manage filters for deposit and posmix
 */
@Composable
fun PreviewTx(model: ReviewTxModel, activity: SamouraiActivity?) {

    val sendType by model.impliedSendType.observeAsState()

    when(sendType) {
        EnumSendType.SPEND_CUSTOM -> CustomPreviewTx(model = model, activity = activity);
        EnumSendType.SPEND_SIMPLE -> SimplePreviewTx(model = model, activity = activity);
        EnumSendType.SPEND_BOLTZMANN -> StonewallPreviewTx(model = model, activity = activity);
        EnumSendType.SPEND_BATCH -> BatchPreviewTx(model = model, activity = activity);
        EnumSendType.SPEND_CUSTOM_BATCH -> CustomPreviewTx(model = model, activity = activity);
        else -> {throw RuntimeException("not managed sendType ${model.sendType} for PreviewTx")}
    }

}

@Preview(showBackground = true, heightDp = 780, widthDp = 420)
@Composable
fun DefaultPreviewTx(
    @PreviewParameter(MyModelPreviewProvider::class) reviewTxModel: ReviewTxModel
) {
    PreviewTx(model = reviewTxModel, activity = null)
}
package com.samourai.wallet.send.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.samourai.wallet.theme.SamouraiWalletTheme
import com.samourai.wallet.tools.SignMessage

class ReviewTxBottomSheet(
    private val model: ReviewTxModel,
    private val type: ReviewTxBottomSheet.ReviewSheetType
) : BottomSheetDialogFragment() {

    enum class ReviewSheetType {
        MANAGE_FEE,
        PREVIEW_TX,
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        val compose = ComposeView(requireContext())

        compose.setContent {
            SamouraiWalletTheme {
                Surface(color = MaterialTheme.colors.background) {
                    when (type) {
                        ReviewSheetType.MANAGE_FEE -> ReviewTxFeeManager(model = model)
                        ReviewSheetType.PREVIEW_TX -> SignMessage()
                    }
                }
            }
        }

        compose.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)


        container?.addView(compose)
        return compose
    }
}
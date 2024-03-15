package com.samourai.wallet.send.review.sendbutton

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData

class SwipeSendButtonFragment(
    private var amountToLeaveWallet: LiveData<Long>,
    private var action: Runnable?,
    private var enable: LiveData<Boolean>,
    private var listener: SwipeSendButtonListener?
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        return ComposeView(requireContext()).apply {
            setContent {
                SwipeSendButtonContent(
                    amountToLeaveWallet = amountToLeaveWallet,
                    action = action,
                    enable = enable,
                    listener = listener,
                    alphaBackground = 1f
                )
            }
        }
    }
}
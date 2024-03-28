package com.samourai.wallet.util.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.samourai.wallet.theme.samouraiWindow

@SuppressLint("ValidFragment")
class CustomProgressIndicatorFragment(
    private val progress: LiveData<Float>,
    private val visible: LiveData<Boolean>,
    private val text: String
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        return ComposeView(requireContext()).apply {
            setContent {
                CustomProgressIndicator(
                    progress = progress,
                    visible = visible,
                    text = text,
                    backgroundColor = samouraiWindow
                )
            }
        }
    }
}
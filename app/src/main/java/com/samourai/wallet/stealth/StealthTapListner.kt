package com.samourai.wallet.stealth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput


fun Modifier.stealthTapListener(
    onTapCallBack: () -> Unit = {},
    click: ( () -> Unit)? = null,
    taps: Int = 5,
) = composed {
    var lastTap by remember { mutableStateOf(System.currentTimeMillis()) }
    var numberOfTaps by remember { mutableStateOf(0) }

    fun click(){
        click?.invoke()
        if (System.currentTimeMillis().minus(lastTap) < 2500) {
            numberOfTaps += 1
            lastTap = System.currentTimeMillis()
        } else if (System.currentTimeMillis().minus(lastTap) >= 3000) {
            numberOfTaps = 0
            lastTap = System.currentTimeMillis()
        } else {
            numberOfTaps = 0
        }
        if (numberOfTaps == taps) {
            onTapCallBack.invoke()
            numberOfTaps = 0
        }
    }

    if(click == null){
        this.pointerInput(Unit){
            detectTapGestures {
                click()
            }
        }
    }else{
        this.clickable {
            click()
        }
    }
}
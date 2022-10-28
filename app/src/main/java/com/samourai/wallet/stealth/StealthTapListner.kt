package com.samourai.wallet.stealth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit


fun Modifier.stealthTapListener(
    onTapCallBack: () -> Unit = {},
    click: (() -> Unit)? = null,
    taps: Int = 5,
) = composed {
    var clickListener by remember { mutableStateOf(PublishSubject.create<Long>()) }

    LaunchedEffect(key1 = clickListener){
        clickListener
            .timeout(4000,TimeUnit.MILLISECONDS) {
                clickListener = PublishSubject.create();
            }
            .throttleLatest(50,TimeUnit.MILLISECONDS)
            .buffer(taps)
            .take(taps.toLong())
            .subscribe {  onTapCallBack.invoke() }
    }
    fun click() {
        clickListener.onNext(System.currentTimeMillis())
        click?.invoke()
    }

    //If the element require click effects clickable is the suitable event listener
    if (click != null) {
        this.clickable {
            click()
        }
    } else {
        this.pointerInput(Unit) {
            detectTapGestures {
                click()
            }
        }
    }
}
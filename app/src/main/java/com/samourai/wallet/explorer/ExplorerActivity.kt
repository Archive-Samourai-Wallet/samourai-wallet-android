package com.samourai.wallet.explorer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.samourai.wallet.R

class ExplorerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explorer)
    }

    companion object{
        const val TX_URI= "tx_uri";
    }
}
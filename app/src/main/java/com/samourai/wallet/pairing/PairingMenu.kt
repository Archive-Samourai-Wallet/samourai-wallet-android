package com.samourai.wallet.pairing

import android.os.Bundle
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity

class PairingMenuActivity : SamouraiActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pairing_menu)
        setSupportActionBar(findViewById(R.id.toolbar_settings))
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        title = "Pairing code"
    }
}
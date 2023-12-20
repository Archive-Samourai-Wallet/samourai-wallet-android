package com.samourai.wallet.pairing

import android.annotation.SuppressLint
import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
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

        val fullWalletSwitch = findViewById<SwitchMaterial>(R.id.switchFullWallet)
        val watchOnlySwitch = findViewById<SwitchMaterial>(R.id.switchWatchOnly)
        val generateCodeBtn = findViewById<MaterialButton>(R.id.generateCodeButton)
        generateCodeBtn.alpha = 0.5f

        fullWalletSwitch.setOnClickListener{
            if (fullWalletSwitch.isChecked) {
                watchOnlySwitch.isChecked = false
                generateCodeBtn.alpha = 1f
            }
            else
                generateCodeBtn.alpha = .5f
        }

        watchOnlySwitch.setOnClickListener{
            if (watchOnlySwitch.isChecked) {
                fullWalletSwitch.isChecked = false
                generateCodeBtn.alpha = 1f
            }
            else
                generateCodeBtn.alpha = .5f
        }

        generateCodeBtn.setOnClickListener {
            if (generateCodeBtn.alpha == 1f) {
                val bundle = Bundle()
                bundle.putString("payload", "Whatever nigga!!")
                val showPairingPayload = ShowPairingPayload()
                showPairingPayload.arguments = bundle
                showPairingPayload.show(supportFragmentManager, showPairingPayload.tag)            }
        }
    }
}
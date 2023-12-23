package com.samourai.wallet.pairing

import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.samourai.wallet.BuildConfig
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.SamouraiWallet
import com.samourai.wallet.crypto.AESUtil
import com.samourai.wallet.hd.HD_WalletFactory
import com.samourai.wallet.network.dojo.DojoUtil
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.util.CharSequenceX
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Random

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

        val passsphrase =  HD_WalletFactory.getInstance(applicationContext).get().passphrase
        val randomWords = getRadomWords()

        val watchOnlyPayload = generateSentinelPayload(randomWords)
        val fullWalletPayload = generateFullWalletPayload(passsphrase ?: randomWords)

        var selectedPayload: String? = null
        var selectedPassword = passsphrase
        var selectedType = ""

        generateCodeBtn.alpha = 0.5f

        fullWalletSwitch.setOnClickListener{
            if (fullWalletSwitch.isChecked) {
                selectedType = "full"
                watchOnlySwitch.isChecked = false
                selectedPayload = fullWalletPayload
                selectedPassword = if (passsphrase != null) "Your BIP39 Passphrase" else randomWords
                generateCodeBtn.alpha = 1f
            }
            else
                generateCodeBtn.alpha = .5f
        }

        watchOnlySwitch.setOnClickListener{
            if (watchOnlySwitch.isChecked) {
                selectedType = "watch-only"
                fullWalletSwitch.isChecked = false
                selectedPayload = watchOnlyPayload
                selectedPassword = randomWords
                generateCodeBtn.alpha = 1f
            }
            else
                generateCodeBtn.alpha = .5f
        }

        generateCodeBtn.setOnClickListener {
            if (generateCodeBtn.alpha == 1f) {
                val bundle = Bundle()
                bundle.putString("type", selectedType)
                bundle.putString("payload", selectedPayload.toString())
                bundle.putString("password", selectedPassword)

                val showPairingPayload = ShowPairingPayload()
                showPairingPayload.arguments = bundle
                showPairingPayload.show(supportFragmentManager, showPairingPayload.tag)            }
        }
    }

    private fun getRadomWords(): String {
        val wordsList = mutableListOf<String>()

        try {
            val reader = BufferedReader(InputStreamReader(this.assets.open("LargeWordsList/eff_large_wordlist.txt")))

            var line: String? = reader.readLine()
            while (line != null) {
                val word = line.split("\t")[1]
                wordsList.add(word)
                line = reader.readLine()
            }

            return wordsList.shuffled(Random()).take(4).joinToString("-")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ""
    }

    private fun generateSentinelPayload(oneTimePassword: String): String? {
        val decrypted = PayloadUtil.getInstance(applicationContext).sentinelPairingPayload
        val encrypted = AESUtil.encrypt(
            decrypted.toString(),
            CharSequenceX(oneTimePassword),
            AESUtil.DefaultPBKDF2Iterations
        )
        val json = JSONObject()
        json.put("version", BuildConfig.VERSION_CODE)
        json.put("external", "SW export")
        json.put("payload", encrypted)

        return json.toString()
    }

    private fun generateFullWalletPayload(password: String): String {
        val pairingObj = JSONObject()
        val jsonObj = JSONObject()
        val dojoObj = JSONObject()
        try {
            if (DojoUtil.getInstance(applicationContext).dojoParams != null) {
                val params = DojoUtil.getInstance(applicationContext).dojoParams
                val url = DojoUtil.getInstance(applicationContext).getUrl(params)
                val apiKey = DojoUtil.getInstance(applicationContext).getApiKey(params)
                if (url != null && apiKey != null && url.isNotEmpty() && apiKey.isNotEmpty()) {
                    dojoObj.put("apikey", apiKey)
                    dojoObj.put("url", url)
                }
            }
            jsonObj.put("type", "whirlpool.gui")
            jsonObj.put("version", "3.0.0")
            jsonObj.put(
                "network",
                if (SamouraiWallet.getInstance().isTestNet) "testnet" else "mainnet"
            )
            val mnemonic = HD_WalletFactory.getInstance(applicationContext).get().mnemonic

            val encrypted =
                AESUtil.encrypt(mnemonic, CharSequenceX(password), AESUtil.DefaultPBKDF2Iterations)
            jsonObj.put(
                "passphrase",
                SamouraiWallet.getInstance().hasPassphrase(applicationContext)
            )
            jsonObj.put("mnemonic", encrypted)
            pairingObj.put("pairing", jsonObj)
            if (dojoObj.has("url") && dojoObj.has("apikey")) {
                val apiKey = dojoObj.getString("apikey")
                val encryptedApiKey = AESUtil.encrypt(
                    apiKey,
                    CharSequenceX(HD_WalletFactory.getInstance(applicationContext).get().passphrase)
                )
                dojoObj.put("apikey", encryptedApiKey)
                pairingObj.put("dojo", dojoObj)
            }
        } catch (_: Exception) {}

        return pairingObj.toString()
    }
}
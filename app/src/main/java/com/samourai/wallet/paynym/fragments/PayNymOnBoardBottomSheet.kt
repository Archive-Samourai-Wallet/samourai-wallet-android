package com.samourai.wallet.paynym.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.R
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.databinding.BottomsheetPaynymOnboardingBinding
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.paynym.api.PayNymApiService
import com.samourai.wallet.util.CharSequenceX
import com.samourai.wallet.util.PrefsUtil
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import kotlinx.coroutines.*
import org.json.JSONObject


class PayNymOnBoardBottomSheet : BottomSheetDialogFragment() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var binding:BottomsheetPaynymOnboardingBinding
    private var onClaim: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = BottomsheetPaynymOnboardingBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val strPaymentCode = BIP47Util.getInstance(activity?.application).paymentCode.toString()
        Picasso.get()
                .load("${PayNymApiService.PAYNYM_API}/preview/${strPaymentCode}")
            .transform(object :Transformation{
                override fun transform(source: Bitmap?): Bitmap? {
                    BIP47Util.getInstance(context)
                        .setAvatar(source)
                    return  source;
                }
                override fun key(): String {
                    return  "paynym_avatar";
                }

            }).into(binding.claimPayNymAvatarPreview)

        binding.skipClaim.setOnClickListener {
            PrefsUtil.getInstance(context?.applicationContext).setValue(PrefsUtil.PAYNYM_REFUSED, true)
            saveInstance()
            dialog?.dismiss();
        }

        binding.claimButtonPaynym.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Samourai_MaterialDialog_Rounded)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.claim_paynym_prompt)
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        dialog.dismiss()
                        this.claim()
                        binding.claimProgress.visibility = View.VISIBLE
                        this.isCancelable = false
                    }.setNegativeButton(R.string.no) { _, _ -> }
                    .show()

        }
    }

    fun setOnClaimCallBack(claimCallBack: (() -> Unit)) {
        this.onClaim = claimCallBack
    }

    fun saveInstance(){
        PayloadUtil.getInstance(requireContext()).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(requireContext()).guid + AccessFactory.getInstance(requireContext()).pin))
    }

    override fun onDestroy() {
        if (job.isActive) {
            job.cancel()
        }
        super.onDestroy()
    }

    private fun claim() {

        val strPaymentCode = BIP47Util.getInstance(activity?.application).paymentCode.toString()
        val apiPayNymApiService = PayNymApiService(strPaymentCode, requireContext())
        val job = scope.launch(Dispatchers.IO) {
            try {
                val response = apiPayNymApiService.claim()
                if (response.isSuccessful) {
                    val nymResponse = apiPayNymApiService.getNymInfo()
                    if (nymResponse.isSuccessful) {
                        try {
                            val data = JSONObject(nymResponse.body?.string())
                            PayloadUtil.getInstance(context).serializePayNyms(data)
                            val nym = if (data.has("nymName")) data.getString("nymName") else ""
                            PrefsUtil.getInstance(context).setValue(PrefsUtil.PAYNYM_CLAIMED, true)
                            PrefsUtil.getInstance(context).setValue(PrefsUtil.PAYNYM_BOT_NAME, nym)
                            saveInstance()
                            onClaim?.invoke()
                            withContext(Dispatchers.Main) {
                                isCancelable = true
                                binding.claimProgressIndicator.visibility = View.GONE
                                Toast.makeText(requireContext(), "Paynym claimed : $nym", Toast.LENGTH_LONG).show()
                                dismiss()
                            }
                        } catch (ex: Exception) {
                            throw CancellationException(ex.message)
                        }
                    }
                }
            } catch (ex: Exception) {

                throw CancellationException(ex.message)
            }
        }
        job.invokeOnCompletion {
            if (it != null) {
                scope.launch(Dispatchers.Main) {
                    isCancelable = true
                    binding.claimProgressIndicator.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error ${it.message}", Toast.LENGTH_LONG).show()
                }
                it.printStackTrace()
            }
        }

    }
}
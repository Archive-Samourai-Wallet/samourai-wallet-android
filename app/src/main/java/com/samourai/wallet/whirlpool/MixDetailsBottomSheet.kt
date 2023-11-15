package com.samourai.wallet.whirlpool

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.samourai.wallet.R
import com.samourai.wallet.databinding.FragmentMixDetailsDialogBinding
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo
import io.reactivex.disposables.CompositeDisposable


class MixDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentMixDetailsDialogBinding? = null
    private val disposable = CompositeDisposable()
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentMixDetailsDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val hash = arguments?.getString(ARG_HASH)
        val outputN = arguments?.getInt(ARG_OUTPUT_N)
        if (hash == null || outputN == null) {
            this.dismiss()
            return
        }
        if (AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet.isPresent) {
            val wallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet.get()
            val whirlpoolUtxo = wallet.utxoSupplier.findUtxo(hash, outputN)
            if (whirlpoolUtxo == null) {
                this.dismiss()
                return
            }
            setMixState(whirlpoolUtxo)
            whirlpoolUtxo.isAccountPremix
            whirlpoolUtxo.utxoState.observable
                .subscribe {
                   setMixState(whirlpoolUtxo)
                }.apply {
                    disposable.add(this)
                }
        } else {
            this.dismiss()
            return
        }
    }

    private fun setMixState(whirlpoolUtxo: WhirlpoolUtxo) {
        binding.mixAmount.text = FormatsUtil.formatBTC(whirlpoolUtxo.utxo.value)
        binding.mixPool.text = whirlpoolUtxo.utxoState.poolId
        binding.mixTxConfirmation.text = "${whirlpoolUtxo.utxo.confirmations}"
        binding.mixesDone.text = "${whirlpoolUtxo.mixsDone}"

        try {
            if (whirlpoolUtxo.utxoState != null) {
                if(whirlpoolUtxo.utxoState.mixProgress != null){
                    val mixProgress = whirlpoolUtxo.utxoState.mixProgress
                    binding.mixStepMessage.text = mixProgress.mixStep.message
                    binding.mixError.visibility = View.GONE
                    binding.mixProgressBar.setIndicatorColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.green_ui_2
                        )
                    )
                    binding.mixProgressContainer.visibility = View.VISIBLE
                    binding.mixProgressBar.setProgressCompat(
                        mixProgress.mixStep.progressPercent,
                        true
                    )
                }else{
                    binding.mixProgressContainer.visibility = View.GONE
                }
                if (whirlpoolUtxo.utxoState.hasError()) {
                    binding.mixError.visibility = View.VISIBLE
                    binding.mixError.text = whirlpoolUtxo.utxoState.error
                    binding.mixProgressBar.setIndicatorColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.red
                        )
                    )
                    binding.mixProgressBar.setProgressCompat(20, true)
                    binding.mixStepMessage.text = getString(R.string.mix_error)
                }
            } else {
                binding.mixProgressContainer.visibility = View.GONE
            }
        } catch (ex: Exception) {
            binding.mixError.text = "${ex.message}"
        }
    }


    companion object {
        private const val ARG_OUTPUT_N = "output_n"
        private const val ARG_HASH = "hash"

        fun newInstance(hash: String, outPutN: Int): MixDetailsBottomSheet =
            MixDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_OUTPUT_N, outPutN)
                    putString(ARG_HASH, hash)
                }
            }
    }

    override fun onDestroyView() {
        disposable.dispose()
        super.onDestroyView()
        _binding = null
    }
}
package com.samourai.wallet.whirlpool.newPool.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import com.samourai.wallet.api.backend.beans.UnspentOutput
import com.samourai.wallet.databinding.FragmentWhirlpoolReviewBinding
import com.samourai.wallet.util.FormatsUtil
import com.samourai.wallet.whirlpool.WhirlpoolTx0
import com.samourai.whirlpool.client.tx0.Tx0Preview
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import com.samourai.whirlpool.client.wallet.WhirlpoolUtils
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount
import com.samourai.whirlpool.client.whirlpool.beans.Pool
import kotlinx.coroutines.*

class ReviewPoolFragment : Fragment() {


    private val coroutineContext = CoroutineScope(Dispatchers.IO)
    private lateinit var binding: FragmentWhirlpoolReviewBinding

    private val account = 0
    private var tx0: WhirlpoolTx0? = null;
    private var tx0FeeTarget: Tx0FeeTarget? = null
    private var mixFeeTarget: Tx0FeeTarget? = null
    private var pool: Pool? = null
    private var onLoading: (Boolean, Exception?) -> Unit = { _: Boolean, _: Exception? -> }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.entropyBar.setMaxBars(4)
        binding.entropyBar.setRange(4)
        binding.previewRetryButton.setOnClickListener {
            makeTxoPreview()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWhirlpoolReviewBinding.inflate(inflater, container, false);
        return binding.root
    }

    fun showProgress(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun setLoadingListener(listener: (Boolean, Exception?) -> Unit) {
        this.onLoading = listener
    }

    fun setTx0(
        tx0: WhirlpoolTx0,
        tx0FeeTarget: Tx0FeeTarget?,
        mixFeeTarget: Tx0FeeTarget?,
        pool: Pool?
    ) {
        this.pool = pool
        this.tx0 = tx0
        this.tx0FeeTarget = tx0FeeTarget
        this.mixFeeTarget = mixFeeTarget
        makeTxoPreview()
        binding.minerFees.text = ""
        binding.feePerUtxo.text = ""
        binding.poolFees.text = ""
        binding.uncycledAmount.text = ""
        binding.amountToCycle.text = ""
        binding.poolTotalFees.text = ""
        binding.totalPoolAmount.text = ""
        binding.poolAmount.text = getBTCDisplayAmount(tx0.pool)
        binding.totalUtxoCreated.text = "${tx0.premixRequested}";
    }

    private fun makeTxoPreview() {
        if (tx0 == null) {
            return;
        }
        showLoadingProgress(true)
        onLoading.invoke(true, null)
        val whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWalletOrNull
        val spendFroms: MutableCollection<UnspentOutput> =
            WhirlpoolUtils.getInstance().toUnspentOutputs(this.tx0!!.outpoints)
        coroutineContext.launch(Dispatchers.IO) {
            val tx0Config = whirlpoolWallet.getTx0Config(tx0FeeTarget, mixFeeTarget)
            tx0Config.changeWallet = WhirlpoolAccount.DEPOSIT
            try {
                val tx0Previews = whirlpoolWallet.tx0Previews(tx0Config, spendFroms)
                val tx0Preview = tx0Previews.getTx0Preview(pool?.poolId)
                withContext(Dispatchers.Main) {
                    showLoadingProgress(false)
                    setFees(tx0Preview);
                    onLoading.invoke(false, null);
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error ${e.message}", Snackbar.LENGTH_LONG).show()
                    onLoading.invoke(false, e)
                    showLoadingProgress(false)
                    binding.previewRetryButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showLoadingProgress(loading: Boolean) {
        if (loading) {
            binding.previewRetryButton.visibility = View.GONE
            binding.loadingFeeDetails.show()
        } else {
            binding.loadingFeeDetails.hide()
        }
    }

    private fun setFees(tx0Preview: Tx0Preview?) {
        tx0Preview?.let {
            TransitionManager.beginDelayedTransition(binding.root, Fade())
            val embeddedTotalFees = tx0Preview.mixMinerFee;
            binding.minerFees.text = getBTCDisplayAmount(it.tx0MinerFee);
            val totalFees = embeddedTotalFees + tx0Preview.feeValue + tx0Preview.tx0MinerFee;
            binding.poolTotalFees.text = getBTCDisplayAmount(totalFees);
            binding.poolFees.text = getBTCDisplayAmount(tx0Preview.feeValue)
            if (it.feeDiscountPercent != 0) {
                val scodeMessage = "SCODE Applied, ${it.feeDiscountPercent}% Discount"
                binding.discountText.text = scodeMessage
            }
            this.tx0?.let { tx0 ->
                binding.totalPoolAmount.text = "${getBTCDisplayAmount(tx0.amountSelected)}"
                binding.amountToCycle.text =
                    getBTCDisplayAmount((tx0.amountSelected - totalFees) - tx0Preview.changeValue)
            }
            binding.uncycledAmount.text = getBTCDisplayAmount((tx0Preview.changeValue));
            binding.feePerUtxo.text = getBTCDisplayAmount(embeddedTotalFees);
            binding.totalUtxoCreated.text = "${it.nbPremix}";
        }
    }

    override fun onDestroy() {
        coroutineContext.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SelectPoolFragment"
    }


    private fun getBTCDisplayAmount(value: Long): String? {
        return "${FormatsUtil.getBTCDecimalFormat(value)} BTC"
    }

}
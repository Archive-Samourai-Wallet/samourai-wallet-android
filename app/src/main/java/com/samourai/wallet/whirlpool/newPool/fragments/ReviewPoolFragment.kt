package com.samourai.wallet.whirlpool.newPool.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.samourai.wallet.databinding.FragmentWhirlpoolReviewBinding
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.whirlpool.WhirlpoolTx0
import com.samourai.wallet.whirlpool.newPool.NewPoolViewModel
import com.samourai.whirlpool.client.tx0.Tx0Preview
import kotlinx.coroutines.*

class ReviewPoolFragment : Fragment() {


    private val coroutineContext = CoroutineScope(Dispatchers.IO)
    private lateinit var binding: FragmentWhirlpoolReviewBinding
    private val newPoolViewModel: NewPoolViewModel by activityViewModels()


    private var tx0: WhirlpoolTx0? = null;
    private var onLoading: (Boolean, Exception?) -> Unit = { _: Boolean, _: Exception? -> }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.entropyBar.setMaxBars(4)
        binding.entropyBar.setRange(4)
        newPoolViewModel.getPool.observe(this.viewLifecycleOwner, { pool ->
            setFees(pool?.tx0Preview)
        })
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentWhirlpoolReviewBinding.inflate(inflater, container, false);
        return binding.root
    }

    fun setLoadingListener(listener: (Boolean, Exception?) -> Unit) {
        this.onLoading = listener
    }

    private fun setFees(tx0Preview: Tx0Preview?) {
        tx0Preview?.let {
            binding.poolAmount.text = getBTCDisplayAmount(it.pool.denomination)
            binding.totalUtxoCreated.text = "${it.nbPremix}";
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
            try {
                tx0 = WhirlpoolTx0(tx0Preview.pool.denomination, tx0Preview.tx0MinerFee, 0, newPoolViewModel.getUtxos.value)
            } catch (ex: java.lang.Exception) {

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
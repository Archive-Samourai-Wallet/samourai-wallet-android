package com.samourai.wallet.whirlpool.fragments

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.samourai.wallet.R
import com.samourai.wallet.databinding.ItemMixUtxoBinding
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.tech.LogUtil
import com.samourai.whirlpool.client.wallet.beans.MixableStatus
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoStatus
import kotlinx.coroutines.*


class MixListAdapter : RecyclerView.Adapter<MixListAdapter.ViewHolder>() {

    private lateinit var itemMixUtxoBinding: ItemMixUtxoBinding
    var onClick: (utxo: WhirlpoolUtxo) -> Unit = {}
    private var displaySats = false
    var onMixingButtonClickListener: (utxo: WhirlpoolUtxo) -> Unit = {}
    private val mDiffer = AsyncListDiffer(this, DIFF_CALLBACK)
    private val scope = CoroutineScope(Dispatchers.Default) + SupervisorJob()

    inner class ViewHolder(private val viewBinding: ItemMixUtxoBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(utxo: WhirlpoolUtxo) {
            val utxoState = utxo.utxoState
            val output = utxo.utxo

            val progressbar = viewBinding.mixProgressBar
            viewBinding.mixAmount.text =  if(displaySats) FormatsUtil.formatSats(output.value) else  FormatsUtil.formatBTC(output.value)
            if (utxoState != null && utxoState.mixProgress != null) {
                viewBinding.mixProgressMessage.visibility = View.VISIBLE
                viewBinding.mixProgressMessage.text = utxoState.mixProgress.mixStep.message
                progressbar.visibility = View.VISIBLE
                progressbar.progress = utxoState.mixProgress.mixStep.progressPercent
            } else {
                viewBinding.mixingButton.setIconResource(R.drawable.ic_timer_white_24dp)
                progressbar.visibility = View.GONE
                viewBinding.mixProgressMessage.visibility = View.GONE
            }
            viewBinding.mixStatus.text = "${utxo.mixsDone} ${viewBinding.root.context.getString(R.string.mixes_complete)}"
            viewBinding.mixingButton.setIconTintResource(R.color.white)
            if(utxoState.hasError()){
                viewBinding.mixStatus.text = "${utxoState.error}";
            }else  if(utxoState.mixableStatus == MixableStatus.UNCONFIRMED ){
                viewBinding.mixingButton.setIconResource(R.drawable.ic_timer_white_24dp)
                return
            }
            when (utxoState.status) {
                WhirlpoolUtxoStatus.READY -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_baseline_play_arrow_24)
                }
                WhirlpoolUtxoStatus.STOP -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_baseline_play_arrow_24)
                }
                WhirlpoolUtxoStatus.TX0 -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_timer_white_24dp)
                }

                WhirlpoolUtxoStatus.TX0_FAILED -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_baseline_problem_24)
                }
                WhirlpoolUtxoStatus.TX0_SUCCESS -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_timer_white_24dp)
                }
                WhirlpoolUtxoStatus.MIX_QUEUE -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_baseline_play_arrow_24)
                }
                WhirlpoolUtxoStatus.MIX_STARTED -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_baseline_pause_24)
                }
                WhirlpoolUtxoStatus.MIX_SUCCESS -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_check_white)
                    progressbar.setProgressCompat(100, true)
                }
                WhirlpoolUtxoStatus.MIX_FAILED -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_baseline_problem_24)
                }
                else -> {
                    viewBinding.mixingButton.setIconResource(R.drawable.ic_timer_white_24dp)
                }
            }

        }
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setDisplaySats(sat:Boolean){
        displaySats = sat
        this.notifyDataSetChanged()
    }
    fun updateList(utxos: List<WhirlpoolUtxo>) {
        scope.launch (Dispatchers.Default){
            try {
                val sorted = utxos
                    .sortedBy { it.utxoState.status != WhirlpoolUtxoStatus.READY }
                    .sortedBy { it.utxoState.status != WhirlpoolUtxoStatus.MIX_QUEUE }
                    .sortedBy { it.utxoState.status != WhirlpoolUtxoStatus.MIX_STARTED }
                    .sortedBy { it.utxoState.status != WhirlpoolUtxoStatus.MIX_SUCCESS }
                withContext(Dispatchers.Main) {
                    mDiffer.submitList(
                        sorted
                    )
                }
            } catch (e: Exception) {
                LogUtil.error("updateList ", e)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        itemMixUtxoBinding =
            ItemMixUtxoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemMixUtxoBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val whirlpoolUtxo = mDiffer.currentList[position];
            holder.bind(whirlpoolUtxo)
            utxoStateMap["${whirlpoolUtxo.utxo.tx_hash}:${whirlpoolUtxo.utxo.tx_output_n}"] =
                whirlpoolUtxo.utxoState.toString()
        } catch (e: Exception) {
        }
        holder.itemView.findViewById<MaterialButton>(R.id.mixingButton)
            .setOnClickListener {
                this.onMixingButtonClickListener.invoke(mDiffer.currentList[position])
            }
        holder.itemView.setOnClickListener {
            this.onClick.invoke(mDiffer.currentList[position])
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        utxoStateMap.clear()
        scope.cancel()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun setOnClickListener(handler: (utxo: WhirlpoolUtxo) -> Unit) {
        this.onClick = handler;
    }

    @JvmName("setOnMixingButtonClickListener1")
    fun setOnMixingButtonClickListener(handler: (utxo: WhirlpoolUtxo) -> Unit) {
        this.onMixingButtonClickListener = handler;
    }

    override fun getItemCount(): Int {
        return mDiffer.currentList.size
    }

    companion object {
        private var utxoStateMap = hashMapOf<String, String>()

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WhirlpoolUtxo>() {
            override fun areItemsTheSame(oldItem: WhirlpoolUtxo, newItem: WhirlpoolUtxo): Boolean {
                return oldItem.utxo.tx_hash == newItem.utxo.tx_hash &&
                        oldItem.utxo.tx_output_n == newItem.utxo.tx_output_n
            }

            /**
             * [WhirlpoolUtxo] is not an immutable object. change detection is not possible
             * [utxoStateMap] will map viewHolder utxo progress,
             * this will be used for change detection
             */
            override fun areContentsTheSame(
                oldItem: WhirlpoolUtxo,
                newItem: WhirlpoolUtxo
            ): Boolean {
                return try {
                    val key = "${newItem.utxo.tx_hash}:${newItem.utxo.tx_output_n}"
                    if (utxoStateMap.containsKey(key)) {
                        return utxoStateMap[key] == newItem.utxoState.toString();
                    }
                    return false
                } catch (ex: Exception) {
                    false;
                }
            }

        }
    }
}
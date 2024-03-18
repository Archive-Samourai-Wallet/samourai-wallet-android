package com.samourai.wallet.send.batch;

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.R
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.databinding.BatchSpendComposeBinding
import com.samourai.wallet.util.func.BatchSendUtil
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.func.WalletUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils.isNotBlank
import java.util.Objects.nonNull

class ComposeFragment : Fragment() {

    private val TAG = "ComposeFragment"

    private val viewModel: BatchSpendViewModel by activityViewModels()
    private val batchListAdapter = BatchListAdapter()
    private lateinit var batchRecyclerView: RecyclerView;
    private var reviewButton: ImageButton? = null
    private var onListItemClick: ((item: BatchSendUtil.BatchSend) -> Unit)? = null;
    private var onReviewClick: View.OnClickListener? = null
    private lateinit var binding: BatchSpendComposeBinding

    protected var compositeDisposable = CompositeDisposable()

    override fun onDestroy() {
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (compositeDisposable.isDisposed) {// useful for the return back button
            compositeDisposable = CompositeDisposable();
        }

        batchRecyclerView = binding.composeView.findViewById(R.id.batchListRecyclerView)
        reviewButton = binding.composeView.findViewById(R.id.reviewButtonBatch)
        batchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        batchRecyclerView.adapter = batchListAdapter
        reviewButton?.setOnClickListener {
            this.onReviewClick?.onClick(it)
        }

        enableReview(viewModel.isValidBatchSpend())

        viewModel.getBatchListLive().observe(viewLifecycleOwner) {


            WalletUtil.saveWallet(requireContext())

            val needsUpdateConnectedPaynymsList = isNeedsUpdateConnectedPaynymsList()
            compositeDisposable.add(

                Observable.fromCallable {
                    if (needsUpdateConnectedPaynymsList) {
                        BIP47Util.getInstance(this@ComposeFragment.activity)
                            .updateOutgoingStatusForNewPayNymConnections()
                    }
                    true
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ success: Boolean ->
                        batchListAdapter.submitList(it)
                        enableReview(viewModel.isValidBatchSpend())
                    }) { error: Throwable ->
                        Log.e(TAG, "exception on BatchSpend Activity: " + error.message, error)
                    }
            )
        }
        batchListAdapter.setOnDeleteClick {
            viewModel.remove(it)
        }
        batchListAdapter.setViewOnClick {
            onListItemClick?.invoke(it)
        }
    }

    private fun isNeedsUpdateConnectedPaynymsList(): Boolean {

        val batchList = viewModel.getBatchListLive().value ?: emptyList()
        var i = 0;
        while (i < batchList!!.size && !BatchSpendActivity.isANotConnectedPayNym(batchList!!.get(i).pcode)) {
            ++i;
        }
        return i < batchList!!.size
    }

    fun setOnItemClickListener(listener: ((batch: BatchSendUtil.BatchSend) -> Unit)) {
        this.onListItemClick = listener
    }

    fun setOnReviewClickListener(listener: View.OnClickListener) {
        this.onReviewClick = listener
    }

    private fun enableReview(enable: Boolean) {
        reviewButton?.isEnabled = enable;
        val colorStateList = ContextCompat.getColorStateList(
            requireContext(),
            if (enable) R.color.white else R.color.disabled_grey)
        reviewButton?.setBackgroundTintList(colorStateList)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BatchSpendComposeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    class BatchListAdapter() : RecyclerView.Adapter<BatchListAdapter.BatchViewHolder>() {

        private val mDiffer: AsyncListDiffer<BatchSendUtil.BatchSend> =
            AsyncListDiffer(this, callBack)

        private var viewOnClick: ((batch: BatchSendUtil.BatchSend) -> Unit)? = null
        private var onDeleteClick: ((batch: BatchSendUtil.BatchSend) -> Unit)? = null
        private var context : Context? = null

        fun setViewOnClick(listener: ((batch: BatchSendUtil.BatchSend) -> Unit)) {
            viewOnClick = listener
        }

        fun setOnDeleteClick(listener: (batch: BatchSendUtil.BatchSend) -> Unit) {
            onDeleteClick = listener
        }

        data class BatchViewHolder(
            val v: View,
            val amount: TextView,
            val to: TextView,
            val deleteButton: MaterialButton,
            val needConnectionStatus: View
        ) : RecyclerView.ViewHolder(v)


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_batch_spend, parent, false)
            return BatchViewHolder(
                view,
                amount = view.findViewById(R.id.batchItemAmount),
                deleteButton = view.findViewById(R.id.batchDeleteBtn),
                to = view.findViewById(R.id.batchItemToAddress),
                needConnectionStatus = view.findViewById(R.id.PCodeNotConnectedGroup)
            )
        }

        override fun onBindViewHolder(holder: BatchViewHolder, position: Int) {

            val item = mDiffer.currentList[position]
            holder.itemView.setOnClickListener {
                viewOnClick?.invoke(item)
            }

            val bip47Meta = BIP47Meta.getInstance()

            holder.itemView.setOnLongClickListener {

                var message = "${holder.itemView.context.getString(R.string.amount)}: ${
                    FormatsUtil.getBTCDecimalFormat(item.amount)
                } BTC"
                if (nonNull(item.paynymCode)) {
                    message = message + "\nPayNym: ${item.paynymCode}"
                } else if (isNotBlank(BIP47Meta.getInstance().getLabel(item.pcode))) {
                    message = message + "\nPayNym: ${BIP47Meta.getInstance().getLabel(item.pcode)}"
                }
                if (nonNull(item.pcode)) {
                    message =
                        message + "\n${holder.itemView.context.getString(R.string.payment_code)}: ${item.pcode}"
                }
                MaterialAlertDialogBuilder(holder.itemView.context)
                    .setTitle(R.string.batch_item_details)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
                true
            }
            holder.amount.text = "${FormatsUtil.getBTCDecimalFormat(item.amount)} BTC"
            holder.to.text = item.getAddr(context)
            if (nonNull(item.pcode)) {
                holder.to.text =
                    if (nonNull(item.paynymCode)) item.paynymCode else bip47Meta.getDisplayLabel(
                        item.pcode
                    );
                holder.needConnectionStatus.visibility =
                    if (bip47Meta.getOutgoingStatus(item.pcode) != BIP47Meta.STATUS_SENT_CFM)
                        View.VISIBLE
                    else View.INVISIBLE
            } else {
                holder.needConnectionStatus.visibility = View.INVISIBLE
            }
            holder.deleteButton.setOnClickListener {
                onDeleteClick?.invoke(item)
            }
        }

        override fun getItemCount(): Int {
            return mDiffer.currentList.size
        }

        fun submitList(list: List<BatchSendUtil.BatchSend>) {
            mDiffer.submitList(list)
        }

        companion object {

            val callBack = object : DiffUtil.ItemCallback<BatchSendUtil.BatchSend>() {

                override fun areItemsTheSame(
                    oldItem: BatchSendUtil.BatchSend,
                    newItem: BatchSendUtil.BatchSend
                ): Boolean {
                    return oldItem.UUID == newItem.UUID
                }

                override fun areContentsTheSame(
                    oldItem: BatchSendUtil.BatchSend,
                    newItem: BatchSendUtil.BatchSend
                ): Boolean {
                    return newItem.rawAddr == oldItem.rawAddr
                            && newItem.amount == oldItem.amount
                            && newItem.UUID == oldItem.UUID
                            && newItem.pcode == oldItem.pcode
                }
            }
        }
    }
}
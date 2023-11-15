package com.samourai.wallet.whirlpool.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samourai.wallet.R
import com.samourai.wallet.api.Tx
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.send.BlockedUTXO
import com.samourai.wallet.util.func.FormatsUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MixTxAdapter(private val mContext: Context) :
    RecyclerView.Adapter<MixTxAdapter.TxViewHolder>() {
    private val VIEW_ITEM = 1
    private var postMixTxs: List<Tx> = listOf()
    private var preMixTxs: List<Tx> = listOf()
    private val VIEW_SECTION = 0
    var displaySats= false
    private val disposables = CompositeDisposable()
    private var listener: ((tx:Tx)->Unit )? = null
    private val mDiffer = AsyncListDiffer(this, DIFF_CALLBACK)
    private val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    interface OnClickListener {
        fun onClick(position: Int, tx: Tx?)
    }

    fun setClickListener(listener:((tx:Tx)->Unit )?) {
        this.listener = listener
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        disposables.dispose()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxViewHolder {
        val view: View = if (viewType == VIEW_ITEM) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.tx_item_layout_, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.tx_item_section_layout, parent, false)
        }
        return TxViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: TxViewHolder, position: Int) {

        val tx = mDiffer.currentList[position]
        val isPremix =   this.preMixTxs.contains(tx)
        if (tx!!.section == null) {
            var _amount = 0L
            _amount = if (tx.amount < 0.0) {
                abs(tx.amount.toLong())
            } else {
                tx.amount.toLong()
            }
            val sdf = SimpleDateFormat("H:mm", Locale.US)
            sdf.timeZone = TimeZone.getDefault()
            holder.tvDateView!!.text = sdf.format(tx.ts * 1000L)
            if (tx.paymentCode != null) {
                holder.txSubText!!.visibility = View.VISIBLE
                holder.txSubText!!.text = BIP47Meta.getInstance().getDisplayLabel(tx.paymentCode)
            } else {
                holder.txSubText!!.visibility = View.GONE
            }

            if (listener != null) holder.itemView.setOnClickListener { view: View? ->
                listener?.invoke(
                    tx
                )
            }
            if (tx.amount < 0.0) {
                holder.tvDirection?.setImageDrawable(
                    ContextCompat.getDrawable(
                        mContext,
                        R.drawable.out_going_tx_whtie_arrow
                    )
                )
                holder.tvAmount?.setTextColor(ContextCompat.getColor(mContext, R.color.white))
                val amountStr = "-${if (displaySats) FormatsUtil.formatSats(_amount) else FormatsUtil.formatBTC(
                    _amount
                )}"
                holder.tvAmount?.text = amountStr

                    holder.txSubText?.visibility = View.VISIBLE
                    holder.txSubText?.setText(R.string.postmix_spend)

            } else {
                TransitionManager.beginDelayedTransition(
                    holder.tvAmount?.rootView as ViewGroup,
                    ChangeBounds()
                )
                holder.tvDirection!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        mContext,
                        R.drawable.incoming_tx_green
                    )
                )
                val amount =
                    if (displaySats) FormatsUtil.formatSats(_amount) else FormatsUtil.formatBTC(
                        _amount
                    )
                holder.tvAmount?.text = amount
                holder.tvAmount?.setTextColor(ContextCompat.getColor(mContext, R.color.green_ui_2))
                    if (_amount == 0L) {
                        holder.txSubText?.visibility = View.VISIBLE
                        holder.txSubText?.setText(R.string.remix_note_tag)
                    } else if (BlockedUTXO.BLOCKED_UTXO_THRESHOLD >= _amount) {
                        holder.txSubText?.visibility = View.VISIBLE
                        holder.txSubText?.setText(R.string.dust)
                    } else {
                        if(!isPremix) {
                            holder.txSubText?.visibility = View.VISIBLE
                            holder.txSubText?.setText(R.string.mixed)
                            holder.tvDirection?.setImageDrawable(
                                ContextCompat.getDrawable(
                                    mContext,
                                    R.drawable.ic_whirlpool
                                )
                            )
                        } else {
                            holder.txSubText?.visibility = View.VISIBLE
                            holder.txSubText?.text =  holder.txSubText?.context?.getString(R.string.transfer_to_whirlpool)
                                holder.tvDirection?.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        mContext,
                                        R.drawable.incoming_tx_green
                                    )
                                )
                        }
                }
            }
            if(!isPremix && _amount ==0L)
                holder.tvDirection!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        mContext,
                        R.drawable.ic_repeat_24dp
                    )
                )
                holder.txNoteGroup!!.visibility = View.GONE
        } else {
            val date = Date(tx.ts)
            if (tx.ts == -1L) {
                holder.tvSection!!.text = holder.itemView.context.getString(R.string.pending)
                holder.tvSection!!.setTextColor(
                    ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.white
                    )
                )
            } else {
                holder.tvSection!!.setTextColor(
                    ContextCompat.getColor(
                        holder.tvSection!!.context,
                        R.color.text_primary
                    )
                )
                if (DateUtils.isToday(tx.ts)) {
                    holder.tvSection!!.text =
                        holder.itemView.context.getString(R.string.timeline_today)
                } else {
                    holder.tvSection!!.text = fmt.format(date)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return mDiffer.currentList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (mDiffer.currentList[position]!!.section != null) VIEW_SECTION else VIEW_ITEM
    }

    fun setTx(postMix: List<Tx>,premix:List<Tx>) {
        this.postMixTxs = postMix;
        this.preMixTxs = premix;
        val list =  arrayListOf<Tx>()
        list.addAll(postMix)
        list.addAll(premix)
        updateList(list)
    }

    private fun updateList(txes: List<Tx>) {
        val disposable = makeSectionedDataSet(txes)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { newList: List<Tx?>? -> mDiffer.submitList(newList) }
        disposables.add(disposable)
    }

    inner class TxViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {
        var tvSection: TextView? = null
        var tvDateView: TextView? = null
        var tvAmount: TextView? = null
        var txSubText: TextView? = null
        var tvNoteView: TextView? = null
        var tvDirection: ImageView? = null
        var txNoteGroup: LinearLayout? = null

        init {
            if (viewType == VIEW_SECTION) {
                tvSection = itemView.findViewById(R.id.section_title)
            } else {
                tvDateView = itemView.findViewById(R.id.tx_time)
                tvDirection = itemView.findViewById(R.id.TransactionDirection)
                tvAmount = itemView.findViewById(R.id.tvAmount)
                txSubText = itemView.findViewById(R.id.txSubText)
                txNoteGroup = itemView.findViewById(R.id.tx_note_group)
                tvNoteView = itemView.findViewById(R.id.tx_note_view)
            }
        }
    }

    @Synchronized
    private fun makeSectionedDataSet(txes: List<Tx>): Observable<List<Tx?>> {
        return Observable.fromCallable {
            Collections.sort(txes) { tx: Tx, t1: Tx -> java.lang.Long.compare(tx.ts, t1.ts) }
            val sectionDates = ArrayList<Long>()
            val sectioned: MutableList<Tx?> = ArrayList()
            // for pending state
            var contains_pending = false
            var containsNonPendingTxForTodaySection = false
            for (i in txes.indices) {
                val tx = txes[i]
                if (tx.confirmations < MAX_CONFIRM_COUNT) {
                    contains_pending = true
                }
                if (tx.confirmations >= MAX_CONFIRM_COUNT && DateUtils.isToday(tx.ts * 1000)) {
                    containsNonPendingTxForTodaySection = true
                }
            }
            for (tx in txes) {
                val date = Date()
                date.time = tx.ts * 1000
                val calendarDM = Calendar.getInstance()
                calendarDM.timeZone = TimeZone.getDefault()
                calendarDM.time = date
                calendarDM[Calendar.HOUR_OF_DAY] = 0
                calendarDM[Calendar.MINUTE] = 0
                calendarDM[Calendar.SECOND] = 0
                calendarDM[Calendar.MILLISECOND] = 0
                if (!sectionDates.contains(calendarDM.time.time)) {
                    if (DateUtils.isToday(calendarDM.time.time)) {
                        if (containsNonPendingTxForTodaySection) {
                            sectionDates.add(calendarDM.time.time)
                        }
                    } else {
                        sectionDates.add(calendarDM.time.time)
                    }
                }
            }
            sectionDates.sortWith { x: Long, y: Long ->
                (x).compareTo(y)
            }
            if (contains_pending) sectionDates.add(-1L)
            for (key in sectionDates) {
                val section = Tx(JSONObject())
                if (key != -1L) {
                    section.section = Date(key).toString()
                } else {
                    section.section = mContext.getString(R.string.pending)
                }
                section.ts = key
                for (tx in txes) {
                    val date = Date()
                    date.time = tx.ts * 1000
                    val fmt = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
                    fmt.timeZone = TimeZone.getDefault()
                    if (key == -1L) {
                        if (tx.confirmations < MAX_CONFIRM_COUNT) {
                            sectioned.add(tx)
                        }
                    } else if (fmt.format(key) == fmt.format(date)) {
                        if (tx.confirmations >= MAX_CONFIRM_COUNT) {
                            sectioned.add(tx)
                        }
                    }
                }
                sectioned.add(section)
            }
            sectioned.reverse()
            sectioned
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun displaySats(it: Boolean?) {
        it?.let {
            this.displaySats = it
            this.notifyDataSetChanged()
        }
    }

    companion object {
        private const val MAX_CONFIRM_COUNT = 3
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Tx?> = object : DiffUtil.ItemCallback<Tx?>() {
            override fun areItemsTheSame(oldItem: Tx, newItem: Tx): Boolean {
                return oldItem.ts == newItem.ts
            }

            override fun areContentsTheSame(oldItem: Tx, newItem: Tx): Boolean {
                if (oldItem.section != null || newItem.section != null) {
                    return true
                }
                var reRender = false
                if (oldItem.confirmations != newItem.confirmations) {
                    reRender = true
                }
                if (oldItem.hash != newItem.hash) {
                    reRender = true
                }
                return reRender
            }
        }
    }

    init {
        fmt.timeZone = TimeZone.getDefault()
    }
}
package com.samourai.wallet.whirlpool.newPool.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.samourai.wallet.whirlpool.newPool.NewPoolViewModel.setPoolPriority
import com.samourai.wallet.whirlpool.newPool.NewPoolViewModel.getTx0PoolPriority
import com.samourai.wallet.whirlpool.newPool.NewPoolViewModel.setPool
import com.samourai.wallet.whirlpool.newPool.NewPoolViewModel.getPool
import androidx.recyclerview.widget.RecyclerView
import com.samourai.wallet.whirlpool.adapters.PoolsAdapter
import com.samourai.wallet.whirlpool.models.PoolViewModel
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import com.samourai.wallet.whirlpool.models.PoolCyclePriority
import com.samourai.wallet.whirlpool.newPool.fragments.SelectPoolFragment.OnPoolSelectionComplete
import io.reactivex.disposables.CompositeDisposable
import com.samourai.wallet.whirlpool.newPool.NewPoolViewModel
import com.samourai.wallet.utxos.models.UTXOCoin
import android.os.Bundle
import com.samourai.wallet.R
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import com.samourai.wallet.whirlpool.newPool.fragments.SelectPoolFragment.SeparatorDecoration
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.samourai.wallet.whirlpool.adapters.PoolsAdapter.OnItemsSelected
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import android.util.TypedValue
import android.view.View
import androidx.fragment.app.Fragment
import com.samourai.whirlpool.client.whirlpool.beans.Pool
import io.reactivex.Single
import java.util.ArrayList

class SelectPoolFragment : Fragment() {
    private var recyclerView: RecyclerView? = null
    private var poolsAdapter: PoolsAdapter? = null
    private val poolViewModels = ArrayList<PoolViewModel>()
    private var feeNormalBtn: MaterialButton? = null
    private var feeLowBtn: MaterialButton? = null
    private var feeHighBtn: MaterialButton? = null
    private var poolFee: TextView? = null
    private var poolCyclePriority = PoolCyclePriority.NORMAL
    private var onPoolSelectionComplete: OnPoolSelectionComplete? = null
    private var fees = ArrayList<Long>()
    private val compositeDisposable = CompositeDisposable()
    private var newPoolViewModel: NewPoolViewModel? = null
    private var tx0Amount = 0L
    private var coins: List<UTXOCoin> = ArrayList()
    fun setOnPoolSelectionComplete(onPoolSelectionComplete: OnPoolSelectionComplete?) {
        this.onPoolSelectionComplete = onPoolSelectionComplete
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        feeNormalBtn = view.findViewById(R.id.pool_fee_normal_btn)
        feeHighBtn = view.findViewById(R.id.pool_fee_high_btn)
        feeLowBtn = view.findViewById(R.id.pool_fee_low_btn)
        newPoolViewModel = ViewModelProvider(requireActivity()).get(NewPoolViewModel::class.java)
        recyclerView = view.findViewById(R.id.pool_recycler_view)
        recyclerView.setItemAnimator(DefaultItemAnimator())
        recyclerView.addItemDecoration(SeparatorDecoration(requireContext(), ContextCompat.getColor(requireContext(), R.color.item_separator_grey), 1))
        recyclerView.setLayoutManager(LinearLayoutManager(context))
        poolsAdapter = PoolsAdapter(context, poolViewModels)
        recyclerView.setAdapter(poolsAdapter)
        poolFee = view.findViewById(R.id.pool_fee_txt)
        feeLowBtn.setOnClickListener(View.OnClickListener { view1: View? -> newPoolViewModel!!.setPoolPriority(PoolCyclePriority.LOW) })
        feeNormalBtn.setOnClickListener(View.OnClickListener { view1: View? -> newPoolViewModel!!.setPoolPriority(PoolCyclePriority.NORMAL) })
        feeHighBtn.setOnClickListener(View.OnClickListener { view1: View? -> newPoolViewModel!!.setPoolPriority(PoolCyclePriority.HIGH) })
        newPoolViewModel!!.getTx0PoolPriority.observe(this.viewLifecycleOwner, { poolCyclePriority: PoolCyclePriority -> setPoolCyclePriority(poolCyclePriority) })
        if (fees.size >= 2) poolFee.setText(fees[1].toString() + " " + getString(R.string.sat_b))
        poolsAdapter!!.setOnItemsSelectListener { position: Int ->
            for (i in poolViewModels.indices) {
                if (i == position) {
                    val selected = !poolViewModels[position].isSelected
                    poolViewModels[i].isSelected = selected
                    if (selected) {
                        newPoolViewModel!!.setPool(poolViewModels[i])
                    } else {
                        if (onPoolSelectionComplete != null) newPoolViewModel!!.setPool(null)
                    }
                } else {
                    poolViewModels[i].isSelected = false
                }
            }
            poolsAdapter!!.update(poolViewModels)
        }
    }

    fun setFees(fees: ArrayList<Long>) {
        this.fees = fees
    }

    private fun loadPools() {
        poolViewModels.clear()
        tx0Amount = NewPoolActivity.getCycleTotalAmount(coins)
        val whirlpoolWallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWalletOrNull
                ?: return
        if (poolsAdapter == null) {
            poolsAdapter = PoolsAdapter(context, poolViewModels)
        }
        val disposable = Single.fromCallable { whirlpoolWallet.poolSupplier.pools }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ whirlpoolPools: Collection<Pool> ->
                    for (whirlpoolPool in whirlpoolPools) {
                        val poolViewModel = PoolViewModel(whirlpoolPool)
                        var fee = fees[1]
                        if (newPoolViewModel!!.getTx0PoolPriority.value != null) {
                            when (newPoolViewModel!!.getTx0PoolPriority.value) {
                                PoolCyclePriority.HIGH -> fee = fees[2]
                                PoolCyclePriority.NORMAL -> fee = fees[1]
                                PoolCyclePriority.LOW -> {
                                    fee = fees[0]
                                }
                            }
                        }
                        if (newPoolViewModel!!.getPool.value != null) {
                            poolViewModel.isSelected = newPoolViewModel!!.getPool.value!!.poolId == poolViewModel.poolId
                        }
                        poolViewModel.setMinerFee(fee, coins)
                        poolViewModels.add(poolViewModel)
                        if (poolViewModel.denomination + poolViewModel.feeValue + poolViewModel.minerFee > tx0Amount) {
                            poolViewModel.isDisabled = true
                        } else {
                            poolViewModel.isDisabled = false
                        }
                    }
                    poolsAdapter!!.notifyDataSetChanged()
                }) { obj: Throwable -> obj.printStackTrace() }
        compositeDisposable.add(disposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_choose_pools, container, false)
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    private fun setPoolCyclePriority(poolCyclePriority: PoolCyclePriority) {
        this.poolCyclePriority = poolCyclePriority
        when (poolCyclePriority) {
            PoolCyclePriority.LOW -> {
                setButtonColor(feeNormalBtn, R.color.window)
                setButtonColor(feeHighBtn, R.color.window)
                setButtonColor(feeLowBtn, R.color.blue_ui_2)
                if (fees.size >= 1) poolFee!!.text = fees[0].toString() + " " + getString(R.string.sat_b)
                if (fees.size >= 1) loadPools()
            }
            PoolCyclePriority.NORMAL -> {
                setButtonColor(feeLowBtn, R.color.window)
                setButtonColor(feeHighBtn, R.color.window)
                setButtonColor(feeNormalBtn, R.color.blue_ui_2)
                if (fees.size >= 2) poolFee!!.text = fees[1].toString() + " " + getString(R.string.sat_b)
                if (fees.size >= 2) loadPools()
            }
            PoolCyclePriority.HIGH -> {
                setButtonColor(feeLowBtn, R.color.window)
                setButtonColor(feeNormalBtn, R.color.window)
                setButtonColor(feeHighBtn, R.color.blue_ui_2)
                if (fees.size >= 2) poolFee!!.text = fees[2].toString() + " " + getString(R.string.sat_b)
                if (fees.size >= 2) loadPools()
            }
        }
    }

    fun setButtonColor(button: MaterialButton?, color: Int) {
        button!!.backgroundTintList = ContextCompat.getColorStateList(context!!, color)
    }

    override fun onDetach() {
        onPoolSelectionComplete = null
        super.onDetach()
    }

    fun setTX0(coins: List<UTXOCoin>) {
        this.coins = coins
        loadPools()
    }

    interface OnPoolSelectionComplete {
        fun onSelect(poolViewModel: PoolViewModel?, priority: PoolCyclePriority?)
    }

    // RV decorator that sets custom divider for the list
    private inner class SeparatorDecoration internal constructor(context: Context, @ColorInt color: Int,
                                                                 @FloatRange(from = 0, fromInclusive = false) heightDp: Float) : ItemDecoration() {
        private val mPaint: Paint
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val params = view.layoutParams as RecyclerView.LayoutParams
            val position = params.viewAdapterPosition
            if (position < state.itemCount) {
                outRect[0, 0, 0] = mPaint.strokeWidth.toInt() // left, top, right, bottom
            } else {
                outRect.setEmpty() // 0, 0, 0, 0
            }
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val offset = (mPaint.strokeWidth / 2).toInt()
            for (i in 0 until parent.childCount) {
                // get the view
                val view = parent.getChildAt(i)
                val params = view.layoutParams as RecyclerView.LayoutParams

                // get the position
                val position = params.viewAdapterPosition
                // draw top separator
                c.drawLine(view.left.toFloat(), (view.top + offset).toFloat(), view.right.toFloat(), (view.top + offset).toFloat(), mPaint)
                if (position == state.itemCount - 1) {
                    // draw bottom line for the last one
                    c.drawLine(view.left.toFloat(), (view.bottom + offset).toFloat(), view.right.toFloat(), (view.bottom + offset).toFloat(), mPaint)
                }
            }
        }

        init {
            mPaint = Paint()
            mPaint.color = color
            val thickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    heightDp, context.resources.displayMetrics)
            mPaint.strokeWidth = thickness
        }
    }

    companion object {
        private const val TAG = "SelectPoolFragment"
    }
}
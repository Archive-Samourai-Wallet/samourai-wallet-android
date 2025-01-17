package com.samourai.wallet.whirlpool.newPool.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.material.button.MaterialButton
import com.samourai.wallet.R
import com.samourai.wallet.api.backend.MinerFeeTarget
import com.samourai.wallet.databinding.FragmentChoosePoolsBinding
import com.samourai.wallet.send.FeeUtil
import com.samourai.wallet.utxos.models.UTXOCoin
import com.samourai.wallet.whirlpool.adapters.PoolsAdapter
import com.samourai.wallet.whirlpool.models.PoolCyclePriority
import com.samourai.wallet.whirlpool.models.PoolViewModel
import com.samourai.wallet.whirlpool.newPool.NewPoolViewModel
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import io.reactivex.disposables.CompositeDisposable

class SelectPoolFragment : Fragment() {
    private lateinit var poolsAdapter: PoolsAdapter
    private var poolViewModels = ArrayList<PoolViewModel>()
    private var poolCyclePriority = PoolCyclePriority.NORMAL
    private var onPoolSelectionComplete: OnPoolSelectionComplete? = null
    private val compositeDisposable = CompositeDisposable()
    private val newPoolViewModel: NewPoolViewModel by activityViewModels()
    private var tx0Amount = 0L
    private lateinit var binding: FragmentChoosePoolsBinding
    private var coins: List<UTXOCoin> = ArrayList()
    private val fees :ArrayList<Long> = arrayListOf()

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.poolRecyclerView.itemAnimator = DefaultItemAnimator()
        binding.poolRecyclerView.addItemDecoration(SeparatorDecoration(requireContext(), ContextCompat.getColor(requireContext(), R.color.item_separator_grey), 1f))
        binding.poolRecyclerView.layoutManager = LinearLayoutManager(context)
        poolsAdapter = PoolsAdapter(requireContext(), poolViewModels)
        binding.poolRecyclerView.adapter = poolsAdapter
        binding.poolRecyclerView.itemAnimator = null
        binding.feeLowBtn.setOnClickListener {
            newPoolViewModel.setPoolPriority(PoolCyclePriority.LOW)
            newPoolViewModel.loadPools()
        }
        binding.feeNormalBtn.setOnClickListener {
            newPoolViewModel.setPoolPriority(PoolCyclePriority.NORMAL)
            newPoolViewModel.loadPools()
        }
        binding.feeHighBtn.setOnClickListener {
            newPoolViewModel.setPoolPriority(PoolCyclePriority.HIGH)
            newPoolViewModel.loadPools()
        }
        newPoolViewModel.getTx0PoolPriority.observe(this.viewLifecycleOwner, { poolCyclePriority: PoolCyclePriority -> setPoolCyclePriority(poolCyclePriority) })
        val wallet = AndroidWhirlpoolWalletService.getInstance().whirlpoolWallet()
        if (wallet != null) {
            fees.add(wallet.minerFeeSupplier.getFee(MinerFeeTarget.BLOCKS_24).toLong())
            fees.add(wallet.minerFeeSupplier.getFee(MinerFeeTarget.BLOCKS_6).toLong())
            fees.add(wallet.minerFeeSupplier.getFee(MinerFeeTarget.BLOCKS_2).toLong())
        }

        if (FeeUtil.getInstance().feeRepresentation.is1DolFeeEstimator) {
            binding.feeHighBtn.setText("Next Block")
        } else {
            binding.feeHighBtn.setText("HIGH")
        }

        if (fees.size >= 2) {
            binding.poolFee.text = fees[1].toString() + " " + getString(R.string.sat_b)
        }

        newPoolViewModel.getPools.observe(this.viewLifecycleOwner, {
            binding.fetchPoolRetryButton.visibility = View.GONE;
            poolsAdapter.update(it)
        });

        newPoolViewModel.getLoadingPools.observe(this.viewLifecycleOwner, {
            binding.poolLoadingProgress.visibility = if (it) View.VISIBLE else View.GONE
            binding.poolRecyclerView.visibility = if (it) View.GONE else View.VISIBLE
        })

        newPoolViewModel.getPoolLoadError.observe(this.viewLifecycleOwner, {
          if(it?.message != null){
              binding.fetchPoolRetryButton.visibility = View.VISIBLE;
          }
        })
        poolsAdapter.setOnItemsSelectListener { position: Int ->
            newPoolViewModel.setSelectedPool(position)
        }
        binding.fetchPoolRetryButton.setOnClickListener {
            newPoolViewModel.loadPools()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentChoosePoolsBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    private fun setPoolCyclePriority(poolCyclePriority: PoolCyclePriority) {
        this.poolCyclePriority = poolCyclePriority

        when (poolCyclePriority) {
            PoolCyclePriority.LOW -> {
                setButtonColor(binding.feeNormalBtn, R.color.window)
                setButtonColor(binding.feeHighBtn, R.color.window)
                setButtonColor(binding.feeLowBtn, R.color.blue_ui_2)
                if (fees.size >= 1) binding.poolFee.text = fees[0].toString() + " " + getString(R.string.sat_b)
            }
            PoolCyclePriority.NORMAL -> {
                setButtonColor(binding.feeLowBtn, R.color.window)
                setButtonColor(binding.feeHighBtn, R.color.window)
                setButtonColor(binding.feeNormalBtn, R.color.blue_ui_2)
                if (fees.size >= 2) binding.poolFee.text = fees[1].toString() + " " + getString(R.string.sat_b)
            }
            PoolCyclePriority.HIGH -> {
                setButtonColor(binding.feeLowBtn, R.color.window)
                setButtonColor(binding.feeNormalBtn, R.color.window)
                setButtonColor(binding.feeHighBtn, R.color.blue_ui_2)
                if (fees.size >= 2) binding.poolFee.text = fees[2].toString() + " " + getString(R.string.sat_b)
            }
        }
    }

    fun setButtonColor(button: MaterialButton?, color: Int) {
        button!!.backgroundTintList = ContextCompat.getColorStateList(requireContext(), color)
    }

    override fun onDetach() {
        onPoolSelectionComplete = null
        super.onDetach()
    }

    fun setTX0(coins: List<UTXOCoin>) {
        this.coins = coins
    }

    interface OnPoolSelectionComplete {
        fun onSelect(poolViewModel: PoolViewModel?, priority: PoolCyclePriority?)
    }

    // RV decorator that sets custom divider for the list
    private inner class SeparatorDecoration internal constructor(context: Context, @ColorInt color: Int,
                                                                 @FloatRange(from = 0.0, fromInclusive = false) heightDp: Float) : ItemDecoration() {
        private val mPaint: Paint = Paint()
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
package com.samourai.wallet.whirlpool.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.samourai.wallet.R
import com.samourai.wallet.api.Tx
import com.samourai.wallet.constants.SamouraiAccount
import com.samourai.wallet.constants.SamouraiAccountIndex
import com.samourai.wallet.databinding.WhirlpoolIntroViewBinding
import com.samourai.wallet.home.BalanceActivity
import com.samourai.wallet.tx.TxDetailsActivity
import com.samourai.wallet.util.PrefsUtil
import com.samourai.wallet.whirlpool.WhirlPoolHomeViewModel
import com.samourai.wallet.whirlpool.WhirlpoolHome.Companion.NEWPOOL_REQ_CODE
import com.samourai.wallet.whirlpool.adapters.MixTxAdapter
import com.samourai.wallet.whirlpool.newPool.NewPoolActivity

class TransactionsFragment : Fragment() {

    private val whirlPoolHomeViewModel: WhirlPoolHomeViewModel by activityViewModels()
    private var adapter: MixTxAdapter? = null;
    lateinit var recyclerView: RecyclerView
    lateinit var containerLayout: FrameLayout
    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (BalanceActivity.DISPLAY_INTENT == intent.action) {
                    if (swipeRefreshLayout != null) {
                        if (swipeRefreshLayout!!.isRefreshing) {
                            swipeRefreshLayout?.isRefreshing = false
                            loadFromRepo()
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val filter = IntentFilter(BalanceActivity.DISPLAY_INTENT)
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(whirlPoolHomeViewModel.broadCastReceiver, filter)
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(broadCastReceiver, filter)
        whirlPoolHomeViewModel.loadOfflineTxData(requireContext())
        whirlPoolHomeViewModel.onboardStatus.observe(viewLifecycleOwner, { showOnBoarding ->
            if (showOnBoarding) {
                showIntroView()
            } else {
                adapter = MixTxAdapter(
                    view.context,
                )
                val displaySats =
                    PrefsUtil.getInstance(requireActivity()).getValue(PrefsUtil.IS_SAT, false)
                adapter?.displaySats = displaySats
                adapter?.setClickListener {  tx ->
                    val txIntent = Intent(requireContext(), TxDetailsActivity::class.java)
                    txIntent.putExtra("TX", tx.toJSON().toString())
                    txIntent.putExtra("_account", SamouraiAccountIndex.POSTMIX)
                    startActivity(txIntent)
                }
                recyclerView.adapter = adapter
                whirlPoolHomeViewModel.mixTransactionsList.observe(viewLifecycleOwner, { mix ->
                    val postMix = mutableListOf<Tx>()
                    val preMix = mutableListOf<Tx>()
                    if(mix.containsKey(SamouraiAccount.POSTMIX)){
                        postMix.clear()
                        mix[SamouraiAccount.POSTMIX]?.let {
                            postMix.addAll(it)
                        }
                    }
                    if(mix.containsKey(SamouraiAccount.PREMIX)){
                        preMix.clear()
                        mix[SamouraiAccount.PREMIX]?.let {
                            // Filter duplicates
                            val filteredPremix=  it.filter { tx->
                                postMix.find { tx2-> tx2.hash==tx.hash } ==null
                            }
                            preMix.addAll(filteredPremix)
                        }
                    }
                    adapter?.setTx(postMix,preMix)
                })
            }
        })

        loadFromRepo()
    }

    private fun showIntroView() {
        val binding =
            WhirlpoolIntroViewBinding.inflate(layoutInflater, containerLayout, true)
        binding.whirlpoolIntroTopText.text = getString(R.string.whirlpool_completely_breaks_the)
        binding.whirlpoolIntroSubText.text = getString(R.string.whirlpool_is_entirely)
        binding.whirlpoolIntroImage.setImageResource(R.drawable.ic_nue_transactions_graphic)
        binding.whirlpoolIntroGetStarted.setOnClickListener {
            activity?.startActivityForResult(
                Intent(activity, NewPoolActivity::class.java),
                NEWPOOL_REQ_CODE
            )
        }
    }

    private fun loadFromRepo() {
        whirlPoolHomeViewModel.loadTransactions(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        containerLayout = FrameLayout(container!!.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        val drawable = ContextCompat.getDrawable(container.context, R.drawable.divider_grey)
        recyclerView = RecyclerView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            layoutManager = LinearLayoutManager(container.context)
            addItemDecoration(
                DividerItemDecoration(
                    container.context,
                    LinearLayoutManager(container.context).orientation
                ).apply {
                    drawable?.let { this.setDrawable(it) }
                })
        }
        swipeRefreshLayout = SwipeRefreshLayout(container.context)
            .apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                whirlPoolHomeViewModel.refresh()
                this.addView(recyclerView)
                setOnRefreshListener {
                    whirlPoolHomeViewModel.refreshList(requireContext())
                }
            }

        whirlPoolHomeViewModel.listRefreshStatus.observe(viewLifecycleOwner, {
            swipeRefreshLayout?.isRefreshing = it
        })
        whirlPoolHomeViewModel.displaySatsLive.observe(viewLifecycleOwner, { satPref->
            adapter?.displaySats(satPref)
        })
        containerLayout.addView(swipeRefreshLayout)
        return containerLayout
    }

    companion object {
        @JvmStatic
        fun newInstance(): TransactionsFragment {
            return TransactionsFragment().apply {
                arguments = Bundle().apply {
                }
            }
        }
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadCastReceiver)
        super.onDestroyView()
    }
}
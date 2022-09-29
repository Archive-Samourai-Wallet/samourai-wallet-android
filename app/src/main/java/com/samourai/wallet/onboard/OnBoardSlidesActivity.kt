package com.samourai.wallet.onboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.samourai.wallet.R
import com.samourai.wallet.databinding.ActivityOnBoardSlidesBinding
import com.samourai.wallet.databinding.ItemOnboardSlideBinding


class OnBoardSlidesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnBoardSlidesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardSlidesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.onBoardViewPager.adapter = ScreenSlidePagerAdapter(this)
        window.statusBarColor = ContextCompat.getColor(this, R.color.window);
        binding.sliderIndicator.setViewPager2(binding.onBoardViewPager)
        binding.getStarted.setOnClickListener {
            startActivity(Intent(this, SetUpWalletActivity::class.java))
        }
    }
    class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 4
        override fun createFragment(position: Int) = OnBoardSliderItem.newInstance(position)
    }

    class OnBoardSliderItem() : Fragment() {
        private lateinit var binding: ItemOnboardSlideBinding

        private val images = arrayListOf(
                R.drawable.ic_break_free,
                R.drawable.ic_toolbox,
                R.drawable.ic_offline_slider,
                R.drawable.ic_scan_slider,
        )
        private val messages = arrayListOf(
                R.string.break_free,
                R.string.perfect_toolbox,
                R.string.offline_mode_allows,
                R.string.connect_your_wallet_to_your_own
        )

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            binding = ItemOnboardSlideBinding.inflate(inflater,container,false);
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            arguments?.takeIf { it.containsKey(POSITION) }?.apply {
                binding.sliderImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), images[this.getInt(POSITION)]))
                binding.sliderMessage.text = getText(messages[this.getInt(POSITION)])
            }
        }

        companion object {
            const val POSITION = "POSITION"
            fun newInstance(position: Int): OnBoardSliderItem {
                return OnBoardSliderItem().apply {
                    this.arguments = Bundle().apply {
                        putInt(POSITION, position)
                    }
                }
            }
        }
    }

}
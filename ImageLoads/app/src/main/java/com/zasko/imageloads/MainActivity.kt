package com.zasko.imageloads

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.insets.GradientProtection
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.zasko.imageloads.adapter.MainTitleAdapter
import com.zasko.imageloads.adapter.MainViewPagerAdapter
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.databinding.ActivityMainBinding
import com.zasko.imageloads.fragment.MainLoadFragment
import com.zasko.imageloads.fragment.main.HeiSiFragment
import com.zasko.imageloads.fragment.main.XiuRenFragment
import com.zasko.imageloads.viewmodel.MainViewModel

class MainActivity : BaseActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }


    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    private var viewPagerAdapter: MainViewPagerAdapter? = null
    private var titleAdapter: MainTitleAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initBindLife(this)
        binding.protectionLayout.setProtections(listOf(GradientProtection(WindowInsetsCompat.Side.TOP, Color.WHITE)))
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        setContentView(binding.root)


        val fragments = ArrayList<MainLoadFragment>()
        addFragments(fragments = fragments)
        viewPagerAdapter = MainViewPagerAdapter(this)
        binding.viewpager.let {
            it.isSaveEnabled = false
            it.adapter = viewPagerAdapter
            viewPagerAdapter?.setData(fragments)
            it.offscreenPageLimit = fragments.size
            it.isUserInputEnabled = false

        }

        initTitleView()


    }

    private fun addFragments(fragments: ArrayList<MainLoadFragment>) {
        MainViewModel.DRAWER_ITEMS.forEach {
            when (it) {
                R.string.xiuren -> {
                    fragments.add(XiuRenFragment())
                }

                R.string.heisi -> {
                    fragments.add(HeiSiFragment())
                }

            }
        }
    }

    private fun initTitleView() {
        titleAdapter = MainTitleAdapter { info ->
            val index = MainViewModel.DRAWER_ITEMS.indexOf(info.id)

            if (index >= 0 && binding.viewpager.currentItem != index) {
                binding.viewpager.setCurrentItem(index, false)
            }
        }
        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            it.adapter = titleAdapter
        }
        titleAdapter?.submitList(MainViewModel.DRAWER_ITEMS.mapIndexed { index, i -> MainTitleAdapter.ItemInfo(id = i, isSelect = index == 0) })

    }
}
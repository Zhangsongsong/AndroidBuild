package com.zasko.imageloads

import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.insets.GradientProtection
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.zasko.imageloads.activity.PersonListActivity
import com.zasko.imageloads.adapter.HomeAdapter
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.databinding.ActivityMainBinding
import com.zasko.imageloads.ui.xiuren.XiuRenHasDownloadActivity
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.viewmodel.MainViewModel

class MainActivity : BaseActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }


    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private var mHomeAdapter: HomeAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.initBindLife(this)
        binding.protectionLayout.setProtections(listOf(GradientProtection(WindowInsetsCompat.Side.TOP, getColor(R.color.teal_700))))
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentCons) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.root)
        initView()
        addListData()
    }

    private fun initView() {
        binding.fragmentRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = HomeAdapter { view, info ->
                when (info.theme) {
                    Constants.THEME_TYPE_XIUREN -> {
                        when (view.id) {
                            R.id.coverCardView -> PersonListActivity.start(context = this@MainActivity, info)
                            R.id.hasDownloadTv -> XiuRenHasDownloadActivity.start(context = this@MainActivity)
                        }
                    }
                }
            }.apply {
                mHomeAdapter = this

            }
        }
    }

    private fun addListData() {
        mHomeAdapter?.setData(
            arrayListOf(
                MainThemeSelectInfo(
                    cover = "https://i.xiutaku.com/photo/uploadfile/202505/22/9810543470.jpg",
                    this@MainActivity.getString(R.string.xiuren),
                    dataUseFrom = DataUseFrom.PRIVATE_FILE.value,
                    theme = Constants.THEME_TYPE_XIUREN
                )
            )
        )
    }


}
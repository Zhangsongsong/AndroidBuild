package com.zasko.imageloads.fragment.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zasko.imageloads.activity.CoverDetailActivity
import com.zasko.imageloads.adapter.MainLoadsAdapter
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.databinding.FragmentNormalBinding
import com.zasko.imageloads.fragment.ThemePagerFragment
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.switchThread
import com.zasko.imageloads.viewmodel.XiuRenViewModel
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class XiuRenFragment : ThemePagerFragment() {

    companion object {
        private const val TAG = "XiuRenFragment"

        private const val DOMAIN = "https://xiutaku.com"
    }


    private lateinit var viewModel: XiuRenViewModel
    private lateinit var binding: FragmentNormalBinding
    private lateinit var adapter: MainLoadsAdapter


    private var loadStarIndex = 0

    private val LOAD_MAX_SIZE = 20


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this)[XiuRenViewModel::class.java]
        binding = FragmentNormalBinding.inflate(inflater)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.refreshLayout.setOnRefreshListener {
            loadNewData()
        }

        adapter = MainLoadsAdapter(loadMore = {
            if (adapter.itemCount > 5) {
//                getLoadMoreData()
            }
        }) { itemInfo ->
            activity?.let { act ->
                CoverDetailActivity.start(activity = act, data = itemInfo.apply {
                    fromType = Constants.THEME_TYPE_XIUREN
                })
            }
        }
        binding.recyclerView.apply {
            itemAnimator = null
            layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply {
                this.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }
            adapter = this@XiuRenFragment.adapter
        }
    }

    override fun initByResume() {
        super.initByResume()
        binding.refreshLayout.isRefreshing = true
        loadNewData()
    }

    override fun loadNewData() {
        super.loadNewData()
        checkRunAfterLoadingMore(moreLockBack = {
            binding.refreshLayout.isRefreshing = false
        }) {
            loadStarIndex = 0
            viewModel.getLocalData().switchThread().doOnSuccess {
                loadStarIndex += LOAD_MAX_SIZE
                setAdapterData(list = it)
            }.doFinally { binding.refreshLayout.isRefreshing = false }.bindLife()
        }
    }

    override fun loadMoreData() {
        super.loadMoreData()
    }


    private fun setAdapterData(list: List<ImageLoadsInfo>, isAdd: Boolean = false) {
        if (isAdd) {
            adapter.addData(list)
        } else {
            adapter.setData(list)
        }
    }

    private fun getRandomData(): Single<MutableList<ImageLoadsInfo>> {
        return Single.just(true).map {
            //"https://i.xiutaku.com/photo/uploadfile/pic/17383.webp"
            val baseUrl = "https://i.xiutaku.com/photo/uploadfile/pic/"
            val list = mutableListOf<ImageLoadsInfo>()
            repeat(20) {
                val url = "${baseUrl}${Random.nextInt(from = 16000, until = 17383)}.webp"
                list.add(ImageLoadsInfo(url = url, width = 1024, height = 1535))
            }
            list
        }.delay(3, TimeUnit.SECONDS)
    }

}
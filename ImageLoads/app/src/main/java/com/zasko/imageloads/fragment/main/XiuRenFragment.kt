package com.zasko.imageloads.fragment.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zasko.imageloads.activity.CoverDetailActivity
import com.zasko.imageloads.adapter.MainLoadsAdapter
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.FragmentNormalBinding
import com.zasko.imageloads.fragment.MainLoadFragment
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.BuildConfig
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class XiuRenFragment : MainLoadFragment() {

    companion object {
        private const val TAG = "XiuRenFragment"

        private const val DOMAIN = "https://xiutaku.com/"
    }


    private lateinit var binding: FragmentNormalBinding
    private lateinit var adapter: MainLoadsAdapter


    private var loadStarIndex = 0

    private val LOAD_MAX_SIZE = 20


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNormalBinding.inflate(inflater)
        init()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun init() {
        adapter = MainLoadsAdapter(loadMore = {
            if (adapter.itemCount > 5) {
//                getLoadMoreData()
            }
        }) { itemInfo ->
            activity?.let { act ->
                CoverDetailActivity.start(activity = act, data = itemInfo)
            }
        }
        binding.refreshLayout.setOnRefreshListener {
            getNewData()
        }
        binding.recyclerView.let {
            it.layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply {
                this.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }
            it.adapter = adapter
        }

    }


    override fun initLoad() {
        super.initLoad()
        binding.refreshLayout.isRefreshing = true
        getNewData()
    }

    private fun setAdapterData(list: List<MainLoadsInfo>, isAdd: Boolean = false) {
        if (isAdd) {
            adapter.addData(list)
        } else {
            adapter.setData(list)
        }
    }

    private fun getNewData() {

        if (isLoadMore.get()) {
            binding.refreshLayout.isRefreshing = false
            return
        }
        loadStarIndex = 0

//        if (BuildConfig.isUseLocal) {
//            getRandomData().switchThread().doOnSuccess { data ->
//                setAdapterData(list = data)
//            }.doFinally { binding.refreshLayout.isRefreshing = false }.bindLife()
//
//        } else {
        ImageLoadsManager.getXiuRenData(loadStarIndex, domain = DOMAIN).switchThread().doOnSuccess { data ->
            loadStarIndex += LOAD_MAX_SIZE
            setAdapterData(list = data)
        }.doFinally { binding.refreshLayout.isRefreshing = false }.bindLife()
//        }
    }

    private fun getLoadMoreData() {
        LogComponent.printD(tag = TAG, message = "getLoadMoreData isLoadMore:${isLoadMore.get()}")
        if (isLoadMore.get()) {
            return
        }
        isLoadMore.set(true)

        if (BuildConfig.isUseLocal) {
            getRandomData().switchThread().doOnSuccess { data ->
                setAdapterData(list = data, isAdd = true)
            }.doFinally { isLoadMore.set(false) }.bindLife()
        } else {
            ImageLoadsManager.getXiuRenData(start = loadStarIndex, domain = DOMAIN).switchThread().doOnSuccess { data ->
                setAdapterData(list = data, isAdd = true)
            }.doFinally {
                isLoadMore.set(false)
            }.bindLife()
        }
    }

    private fun getRandomData(): Single<MutableList<MainLoadsInfo>> {
        return Single.just(true).map {
            //"https://i.xiutaku.com/photo/uploadfile/pic/17383.webp"
            val baseUrl = "https://i.xiutaku.com/photo/uploadfile/pic/"
            val list = mutableListOf<MainLoadsInfo>()
            repeat(20) {
                val url = "${baseUrl}${Random.nextInt(from = 16000, until = 17383)}.webp"
                list.add(MainLoadsInfo(url = url, width = 1024, height = 1535))
            }
            list
        }.delay(3, TimeUnit.SECONDS)
    }

}
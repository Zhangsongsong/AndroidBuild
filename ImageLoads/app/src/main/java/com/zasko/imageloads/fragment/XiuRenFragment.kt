package com.zasko.imageloads.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zasko.imageloads.adapter.MainLoadsAdapter
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.databinding.FragmentNormalBinding
import com.zasko.imageloads.manager.DownloadManager
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.BuildConfig
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.getUrlToName
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.util.Collections
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class XiuRenFragment : MainLoadFragment() {

    companion object {
        private const val TAG = "XiuRenFragment"
    }


    private lateinit var binding: FragmentNormalBinding
    private lateinit var adapter: MainLoadsAdapter


    private var loadStarIndex = 0

    private val LOAD_MAX_SIZE = 20


//    private val downloadList = Collections.synchronizedList(LinkedList<MainLoadsInfo>())
//    private val isDownloadRunning = AtomicBoolean(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNormalBinding.inflate(inflater)
        init()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun init() {
        adapter = MainLoadsAdapter {
            if (adapter.itemCount > 5) {
                getLoadMoreData()
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
//        downloadList.addAll(list)
//        if (!isDownloadRunning.get()) {
//            isDownloadRunning.set(true)
//            binding.recyclerView.removeCallbacks(downloadRunnable)
//            binding.recyclerView.post(downloadRunnable)
//        }
    }

    private fun getNewData() {

        if (isLoadMore.get()) {
            binding.refreshLayout.isRefreshing = false
            return
        }
        loadStarIndex = 0

        if (BuildConfig.isUseLocal) {
            getRandomData().switchThread().doOnSuccess { data ->
                setAdapterData(list = data)
            }.doFinally { binding.refreshLayout.isRefreshing = false }.bindLife()

        } else {
            ImageLoadsManager.getXiuRenData(loadStarIndex).switchThread().doOnSuccess { data ->
                loadStarIndex += LOAD_MAX_SIZE
                setAdapterData(list = data)
            }.doFinally { binding.refreshLayout.isRefreshing = false }.bindLife()
        }
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
            ImageLoadsManager.getXiuRenData(start = loadStarIndex).switchThread().doOnSuccess { data ->
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


//    private val downloadRunnable = object : Runnable {
//        override fun run() {
//            if (downloadList.size > 0) {
//                downloadList.firstOrNull()?.let {
//                        DownloadManager.downloadImage(url = )
//                }
//            } else {
//                isDownloadRunning.set(false)
//            }
//        }
//    }

}
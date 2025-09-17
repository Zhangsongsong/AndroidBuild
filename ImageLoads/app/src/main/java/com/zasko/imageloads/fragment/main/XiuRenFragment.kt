package com.zasko.imageloads.fragment.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zasko.imageloads.activity.PersonDetailActivity
import com.zasko.imageloads.adapter.MainLoadsAdapter
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.data.DataUseFrom
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


    }


    private lateinit var viewModel: XiuRenViewModel
    private lateinit var binding: FragmentNormalBinding


    private lateinit var adapter: MainLoadsAdapter
    private var dataInfo: MainThemeSelectInfo? = null

    private var loadStarIndex = 0
    private val LOAD_MAX_SIZE = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataInfo = arguments?.getSerializable(KEY_DATA) as? MainThemeSelectInfo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this)[XiuRenViewModel::class.java].apply {
            this.initBindLife(this@XiuRenFragment)
        }
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
                loadMoreData()
            }
        }) { itemInfo ->
            activity?.let { act ->
                PersonDetailActivity.start(activity = act, data = itemInfo.apply {
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
            getData(start = loadStarIndex).switchThread().doOnSuccess {
                loadStarIndex += LOAD_MAX_SIZE
                setAdapterData(list = it)
            }.doFinally { binding.refreshLayout.isRefreshing = false }.bindLife()
        }
    }

    override fun loadMoreData() {
        super.loadMoreData()
        if (binding.refreshLayout.isRefreshing || isLoadingMore.get()) {
            return
        }
        isLoadingMore.set(true)
        getData(start = loadStarIndex).switchThread().doOnSuccess {
            loadStarIndex += LOAD_MAX_SIZE
            setAdapterData(list = it, isAdd = true)
        }.doFinally { isLoadingMore.set(false) }.bindLife()
    }

    private fun getData(start: Int): Single<List<ImageLoadsInfo>> {
        return when (dataInfo?.dataUseFrom) {
            DataUseFrom.PRIVATE_FILE.value -> viewModel.getLocalData(start = start)
            else -> viewModel.getNetworkData(start = start)
        }
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
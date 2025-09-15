package com.zasko.imageloads.fragment

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.zasko.imageloads.R
import com.zasko.imageloads.adapter.DetailImagesAdapter
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.databinding.FragmentDetailImageBinding
import com.zasko.imageloads.detail.DownloadListener
import com.zasko.imageloads.detail.DownloadListenerAbs
import com.zasko.imageloads.detail.GettingImageListener
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.PermissionUtil
import com.zasko.imageloads.utils.loadImageWithInside
import com.zasko.imageloads.utils.onClick
import com.zasko.imageloads.utils.switchThread
import com.zasko.imageloads.viewmodel.ImageDetailViewModel
import com.zasko.imageloads.viewmodel.XiuRenViewModel
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ImageDetailFragment : ThemePagerFragment() {


    companion object {
        const val KEY_DATA = "key_data"
        private const val TAG = "ImageDetailFragment"
    }

    private lateinit var viewModel: ImageDetailViewModel
    private lateinit var imageLoadsInfo: ImageLoadsInfo
    private lateinit var binding: FragmentDetailImageBinding
    private lateinit var mAdapter: DetailImagesAdapter


    private var loadMoreIndex = 1

    private val isDownloading = AtomicBoolean(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageLoadsInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(KEY_DATA, ImageLoadsInfo::class.java)!!
            } else {
                it.getSerializable(KEY_DATA) as ImageLoadsInfo
            }
            LogComponent.printD(tag = TAG, message = "mainLoadInfo:${imageLoadsInfo} ${Environment.getExternalStorageDirectory()}")
        }
        viewModel = ViewModelProvider(this)[ImageDetailViewModel::class.java].apply {
            this.initBindLife(this@ImageDetailFragment)
            setLoadsInfo(imageLoadsInfo)
        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDetailImageBinding.inflate(inflater)
        initView()
        loadNewData()
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentCons) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        return binding.root
    }

    private fun initView() {
        binding.backIv.onClick {
            activity?.finish()
        }
        binding.downloadIv.onClick {
            startDownload()
        }

        mAdapter = DetailImagesAdapter(loadMore = {
            if (!binding.bufferLoadingView.isVisible) {
                binding.bufferLoadingView.startAni()
            }
        })

        binding.coverIv.loadImageWithInside(url = imageLoadsInfo.url)
        binding.pictureRecycler.layoutManager = GridLayoutManager(context, 2)
        binding.pictureRecycler.adapter = mAdapter
    }

    override fun loadNewData() {
        super.loadNewData()
        viewModel.getDetailInfo().switchThread().doOnSuccess { info ->
            binding.loadingBar.isVisible = false
            binding.nameTv.text = info.name
            binding.descTv.text = info.desc
            binding.timeTv.text = info.time
            mAdapter.setData(list = info.pictures ?: emptyList())
        }.bindLife()
    }

    override fun loadMoreData() {
        super.loadMoreData()
        if (isLoadEnd.get()) {
            return
        }
        if (isLoadingMore.get()) {
            return
        }
        isLoadingMore.set(true)
        LogComponent.printD(tag = TAG, message = "loadMore ${imageLoadsInfo.href} loadMoreIndex:${loadMoreIndex}")

        val nextIndex = viewModel.getNextPageIndex(currentIndex = loadMoreIndex)
        viewModel.getDetailMore(pageIndex = nextIndex).switchThread().doOnSuccess { list ->
            //如果是重复数据，直接判定end
            if (list.isEmpty() || mAdapter.getData().lastOrNull()?.url == list.lastOrNull()?.url) {
                isLoadEnd.set(true)
            } else {
                mAdapter.addData(list)
            }
        }.doFinally {
            isLoadingMore.set(false)
        }.bindLife()

    }


    private fun startDownload() {
        FileUtil.createExternalDir()
        PermissionUtil.getReadAndWriteExternal(context = requireActivity())
        if (!File(FileUtil.getDownloadPath()).exists() || isDownloading.get()) {
            return
        }
        binding.downloadTipTv.isVisible = true
        viewModel.downloadPic(context = binding.downloadCountTv.context, gettingListener = object : GettingImageListener {
            override fun onGettingPage(page: Int) {
                super.onGettingPage(page)
                LogComponent.printD(TAG, "startDownload onGettingPage page:${page}")
            }
        }, listener = object : DownloadListenerAbs() {

            override fun onStartGettingMaxPage() {
                super.onStartGettingMaxPage()
                isDownloading.set(true)
                binding.downloadTipTv.apply {
                    text = context.getString(R.string.getting_max_page_list)
                }
            }

            override fun onStartDownload(all: Int, dir: String) {
                super.onStartDownload(all, dir)
                binding.downloadTipTv.apply {
                    text = context.getString(R.string.downloading_tip)
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onOneEndDownLoad(index: Int, all: Int, dir: String, fileName: String) {
                super.onOneEndDownLoad(index, all, dir, fileName)
                binding.downloadCountTv.text = "${index}/${all}"
            }

            override fun onEndDownload(all: Int, dir: String) {
                super.onEndDownload(all, dir)
                isDownloading.set(false)
                binding.downloadTipTv.isVisible = false
            }
        })

    }
}
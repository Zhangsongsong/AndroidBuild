package com.zasko.imageloads.ui.xiuren

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.zasko.imageloads.R
import com.zasko.imageloads.adapter.DetailImagesAdapter
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.listener.DownloadListenerAbs
import com.zasko.imageloads.listener.GettingImageListener
import com.zasko.imageloads.dialog.CenterDefaultDialog
import com.zasko.imageloads.dialog.DownloadTipDialog
import com.zasko.imageloads.dialog.WarningDialog
import com.zasko.imageloads.fragment.DetailBaseFragment
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.loadImageWithInside
import com.zasko.imageloads.utils.onClick
import com.zasko.imageloads.utils.setTint
import com.zasko.imageloads.utils.switchThread
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


class XiuRenDetailFragment : DetailBaseFragment() {

    companion object {
        private const val TAG = "XiuRenDetail"
    }


    private var loadMoreIndex = 1
    lateinit var viewModel: XiuRenViewDetailModel


    private val isDownloading = AtomicBoolean(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[XiuRenViewDetailModel::class.java].apply {
            this.initBindLife(this@XiuRenDetailFragment)
            setLoadInfo(imageLoadsInfo)
        }

    }

    override fun bindStart() {
        super.bindStart()
        loadNewData()
    }

    private fun updateHasDownloadView() {
        binding.downloadIv.setTint(binding.downloadIv.context.getColor(if (viewModel.checkHasDownload()) R.color.color_act_bg else R.color.color_222125))
    }

    override fun loadNewData() {
        super.loadNewData()
        viewModel.getDetailInfo().switchThread().doOnSuccess { info ->
            binding.loadingBar.isVisible = false
            binding.nameTv.text = info.name
            binding.descTv.text = info.desc
            binding.timeTv.text = info.time
            mAdapter.setData(list = info.pictures ?: emptyList())
            updateHasDownloadView()
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
        binding.bufferLoadingView.isVisible = true
        binding.bufferLoadingView.startAni()
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
            binding.bufferLoadingView.stopAni()
            binding.bufferLoadingView.isVisible = false
            isLoadingMore.set(false)
        }.bindLife()

    }

    override fun handleClickDownload() {
        viewModel.createAndNeedPermission(activity = requireActivity())
        if (!File(FileUtil.getDownloadPath()).exists() || isDownloading.get()) {
            return
        }
        if (viewModel.checkHasDownload()) {
            showWarningDialog {
                startDownload()
            }
        } else {
            startDownload()
        }
    }

    private fun startDownload() {
        showDownloadDialog()
        viewModel.downloadPic(context = binding.downloadIv.context, gettingListener = object : GettingImageListener {
            override fun onGettingPage(page: Int) {
                super.onGettingPage(page)
                LogComponent.printD(TAG, "startDownload onGettingPage page:${page}")
                downloadDialog?.addGettingText(text = "page:${page}")
            }
        }, listener = object : DownloadListenerAbs() {

            override fun onStartGettingMaxPage() {
                super.onStartGettingMaxPage()
                isDownloading.set(true)
                downloadDialog?.setTitleText(text = context?.getString(R.string.getting_max_page_list) ?: "")
            }

            override fun onStartDownload(all: Int, dir: String) {
                super.onStartDownload(all, dir)
                downloadDialog?.setTitleText(text = context?.getString(R.string.downloading_tip) ?: "")
            }

            @SuppressLint("SetTextI18n")
            override fun onOneEndDownLoad(index: Int, all: Int, dir: String, fileName: String) {
                super.onOneEndDownLoad(index, all, dir, fileName)

                downloadDialog?.updateProgress(progress = index.toFloat() / all, text = "${index}/${all}")
            }

            override fun onEndDownload(all: Int, dir: String) {
                super.onEndDownload(all, dir)
                isDownloading.set(false)
                downloadDialog?.dismiss()
                updateHasDownloadView()
            }
        })

    }
}
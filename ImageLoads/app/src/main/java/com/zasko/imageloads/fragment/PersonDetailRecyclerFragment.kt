package com.zasko.imageloads.fragment

import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.zasko.imageloads.R
import com.zasko.imageloads.adapter.DetailImagesAdapter
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.dialog.CenterDefaultDialog
import com.zasko.imageloads.dialog.DownloadTipDialog
import com.zasko.imageloads.dialog.WarningDialog
import com.zasko.imageloads.manager.DownloadQueueManager
import com.zasko.imageloads.manager.DownloadTaskStatus
import com.zasko.imageloads.ui.xiuren.XiuRenViewDetailModel
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.loadImageWithInside
import com.zasko.imageloads.utils.onClick
import com.zasko.imageloads.utils.switchThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class PersonDetailRecyclerFragment : DetailBaseFragment() {

    companion object {
        fun newInstance(info: ImageLoadsInfo): PersonDetailRecyclerFragment {
            return PersonDetailRecyclerFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_DATA, info)
                }
            }
        }
    }

    private lateinit var viewModel: XiuRenViewDetailModel
    private lateinit var adapter: DetailImagesAdapter
    private var loadMoreIndex = 1
    private var isInitialLoading = false
    private var downloadObserveJob: Job? = null
    private val isDownloading = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[XiuRenViewDetailModel::class.java].apply {
            initBindLife(this@PersonDetailRecyclerFragment)
            setLoadInfo(imageLoadsInfo)
        }
    }

    override fun onDestroyView() {
        downloadObserveJob?.cancel()
        downloadObserveJob = null
        downloadDialog?.dismiss()
        super.onDestroyView()
    }

    override fun bindStart() {
        binding.backIv.onClick {
            activity?.finish()
        }
        binding.downloadIv.onClick {
            handleClickDownload()
        }

        adapter = DetailImagesAdapter(
            loadMore = ::loadMoreData,
            itemClick = { _, index ->
                ImagePreviewFragment.show(
                    fragmentManager = parentFragmentManager,
                    imageUrls = adapter.getData().map { it.url },
                    initialIndex = index,
                    referer = "",
                )
            },
        )
        binding.coverIv.loadImageWithInside(url = imageLoadsInfo.url, placeId = R.mipmap.icon_pic)
        binding.pictureRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.pictureRecycler.itemAnimator = null
        binding.pictureRecycler.adapter = adapter
        binding.loadingBar.isVisible = false
    }

    override fun initByResume() {
        super.initByResume()
        loadNewData()
    }

    override fun loadNewData() {
        if (isLoadingMore.get()) {
            return
        }
        loadMoreIndex = 1
        isLoadEnd.set(false)
        isInitialLoading = true
        binding.loadingBar.isVisible = true
        viewModel.getDetailInfo()
            .switchThread()
            .doOnSuccess { info ->
                bindHeader(info.name, info.time, info.desc)
                adapter.setData(info.pictures ?: emptyList())
                updateHasDownloadView()
            }
            .doFinally {
                isInitialLoading = false
                binding.loadingBar.isVisible = false
            }
            .bindLife()
    }

    override fun loadMoreData() {
        if (isLoadEnd.get() || isLoadingMore.get() || isInitialLoading) {
            return
        }
        isLoadingMore.set(true)
        binding.bufferLoadingView.startAni()
        val nextIndex = viewModel.getNextPageIndex(currentIndex = loadMoreIndex)
        viewModel.getDetailMore(pageIndex = nextIndex)
            .switchThread()
            .doOnSuccess { list ->
                if (list.isEmpty() || adapter.getData().lastOrNull()?.url == list.lastOrNull()?.url) {
                    isLoadEnd.set(true)
                } else {
                    loadMoreIndex = nextIndex
                    adapter.addData(list)
                }
            }
            .doFinally {
                isLoadingMore.set(false)
                binding.bufferLoadingView.stopAni(unVis = true)
            }
            .bindLife()
    }

    override fun handleClickDownload() {
        viewModel.createAndNeedPermission(activity = requireActivity())
        if (!File(FileUtil.getDownloadPath()).exists() || isDownloading.get()) {
            return
        }
        if (viewModel.checkHasDownload()) {
            showDownloadWarning {
                startDownload()
            }
        } else {
            startDownload()
        }
    }

    private fun bindHeader(name: String, time: String, desc: String) {
        binding.nameTv.text = name
        binding.timeTv.text = time
        binding.timeTv.isVisible = time.isNotBlank()
        binding.descTv.text = desc
        binding.descTv.isVisible = desc.isNotBlank()
    }

    override fun updateHasDownloadView() {
        binding.downloadIv.isSelected = viewModel.checkHasDownload()
    }

    private fun startDownload(forceOverwrite: Boolean = false) {
        if (!isAdded || isDownloading.get()) {
            return
        }
        if (downloadDialog == null) {
            downloadDialog = DownloadTipDialog(activity = requireActivity())
        }
        downloadDialog?.show()
        val taskId = DownloadQueueManager.enqueueXiurenDownload(
            sourceLabel = getString(R.string.xiuren),
            detailUrl = imageLoadsInfo.href,
            detailTitle = imageLoadsInfo.title.ifBlank { "详情" },
            preparedDetailInfo = null,
            forceOverwrite = forceOverwrite,
        )
        isDownloading.set(true)
        downloadObserveJob?.cancel()
        val observerJob = CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                val terminalState = DownloadQueueManager.awaitTaskFinalState(taskId) { state ->
                    isDownloading.set(true)
                    downloadDialog?.setTitleText(
                        text = when (state.status) {
                            DownloadTaskStatus.PREPARING -> getString(R.string.getting_max_page_list)
                            DownloadTaskStatus.DOWNLOADING -> getString(R.string.downloading_tip)
                            else -> getString(R.string.downloading_tip)
                        },
                    )
                    downloadDialog?.updateProgress(
                        progress = state.progressFraction ?: 0f,
                        text = state.progressText,
                    )
                }
                when (terminalState?.status) {
                    DownloadTaskStatus.SUCCEEDED -> {
                        updateHasDownloadView()
                        if (isAdded) {
                            Toast.makeText(requireContext(), "下载完成", Toast.LENGTH_SHORT).show()
                        }
                    }

                    DownloadTaskStatus.FAILED -> {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "下载失败", Toast.LENGTH_SHORT).show()
                        }
                    }

                    else -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                isDownloading.set(false)
                downloadDialog?.dismiss()
                downloadObserveJob = null
            }
        }
        downloadObserveJob = observerJob
    }

    private fun showDownloadWarning(positive: () -> Unit) {
        val dialog = WarningDialog(requireActivity(), clickBack = { status, warningDialog ->
            if (status == CenterDefaultDialog.VALUE_POSITIVE) {
                positive()
            }
            warningDialog.dismiss()
        })
        dialog.show()
        dialog.updateALlText(
            content = getString(R.string.if_oval_download),
            negative = getString(R.string.no),
            positive = getString(R.string.yes),
        )
    }
}

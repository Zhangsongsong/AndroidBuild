package com.zasko.imageloads.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.dialog.CenterDefaultDialog
import com.zasko.imageloads.dialog.DownloadTipDialog
import com.zasko.imageloads.dialog.WarningDialog
import com.zasko.imageloads.fragment.ImagePreviewFragment
import com.zasko.imageloads.manager.DownloadQueueManager
import com.zasko.imageloads.manager.DownloadTaskStatus
import com.zasko.imageloads.ui.common.CommonImageDetailInfo
import com.zasko.imageloads.ui.common.CommonImageDetailScreen
import com.zasko.imageloads.ui.xiuren.XiuRenViewDetailModel
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.switchThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class PersonDetailActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "PersonDetailActivity"
        private const val KEY_DATA = "key_data"

        fun start(activity: Activity, data: ImageLoadsInfo) {
            val intent = Intent(activity, PersonDetailActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(KEY_DATA, data)
            activity.startActivity(intent)
        }
    }

    private lateinit var viewModel: XiuRenViewDetailModel
    private lateinit var imageLoadsInfo: ImageLoadsInfo
    private val pictures = mutableStateListOf<ImageInfo>()

    private var detailInfo by mutableStateOf(ImageDetailInfo())
    private var isInitialLoading by mutableStateOf(false)
    private var isLoadingMoreState by mutableStateOf(false)
    private var isLoadEndState by mutableStateOf(false)
    private var isDownloadedState by mutableStateOf(false)
    private var isDownloadingState by mutableStateOf(false)
    private var loadMoreIndex = 1
    private var downloadDialog: DownloadTipDialog? = null
    private var downloadObserveJob: Job? = null
    private val isDownloading = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageLoadsInfo = readImageInfo()
        viewModel = ViewModelProvider(this)[XiuRenViewDetailModel::class.java].apply {
            initBindLife(this@PersonDetailActivity)
            setLoadInfo(imageLoadsInfo)
        }

        setContent {
            ImageLoadsTheme {
                CommonImageDetailScreen(
                    detailInfo = buildCommonDetailInfo(),
                    defaultTitle = "详情",
                    isLoading = isInitialLoading,
                    isLoadingMore = isLoadingMoreState,
                    isLoadMoreEnabled = !isLoadEndState,
                    isDownloading = isDownloadingState,
                    downloadText = getDownloadText(),
                    showOverwriteDialog = false,
                    errorMessage = "",
                    imageModelProvider = { it.url },
                    onBack = ::handleBack,
                    onDownload = ::handleClickDownload,
                    onConfirmOverwrite = {},
                    onDismissOverwrite = {},
                    onLoadMore = ::loadMoreData,
                    onImageClick = ::openImagePreview,
                )
            }
        }

        loadNewData()
    }

    private fun readImageInfo(): ImageLoadsInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(KEY_DATA, ImageLoadsInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(KEY_DATA) as? ImageLoadsInfo
        } ?: ImageLoadsInfo()
    }

    private fun loadNewData() {
        loadMoreIndex = 1
        isLoadEndState = false
        isInitialLoading = true
        viewModel.getDetailInfo()
            .switchThread()
            .doOnSuccess { info ->
                detailInfo = info
                pictures.clear()
                pictures.addAll(info.pictures ?: emptyList())
                updateHasDownloadView()
            }
            .doFinally { isInitialLoading = false }
            .bindLife()
    }

    private fun loadMoreData() {
        if (isLoadEndState || isLoadingMoreState || isInitialLoading) {
            return
        }
        isLoadingMoreState = true
        val nextIndex = viewModel.getNextPageIndex(currentIndex = loadMoreIndex)
        LogComponent.printD(tag = TAG, message = "loadMore ${imageLoadsInfo.href} nextIndex:$nextIndex")
        viewModel.getDetailMore(pageIndex = nextIndex)
            .switchThread()
            .doOnSuccess { list ->
                if (list.isEmpty() || pictures.lastOrNull()?.url == list.lastOrNull()?.url) {
                    isLoadEndState = true
                } else {
                    loadMoreIndex = nextIndex
                    pictures.addAll(list)
                }
            }
            .doFinally { isLoadingMoreState = false }
            .bindLife()
    }

    private fun updateHasDownloadView() {
        isDownloadedState = viewModel.checkHasDownload()
    }

    private fun buildCommonDetailInfo(): CommonImageDetailInfo {
        val subtitles = mutableListOf<String>()
        if (detailInfo.time.isNotBlank()) {
            subtitles.add(detailInfo.time)
        }
        if (detailInfo.desc.isNotBlank()) {
            subtitles.add(detailInfo.desc)
        }
        return CommonImageDetailInfo(
            url = imageLoadsInfo.href,
            title = detailInfo.name,
            subtitles = subtitles,
            pictures = pictures.map { it.toImageLoadsInfo() },
        )
    }

    private fun ImageInfo.toImageLoadsInfo(): ImageLoadsInfo {
        return ImageLoadsInfo(
            url = url,
            width = width,
            height = height,
        )
    }

    private fun getDownloadText(): String {
        return when {
            isDownloadingState -> "下载中"
            isDownloadedState -> "已下载"
            else -> "下载"
        }
    }

    private fun handleBack() {
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun openImagePreview(imageInfo: ImageLoadsInfo, index: Int) {
        ImagePreviewFragment.show(
            fragmentManager = supportFragmentManager,
            imageUrls = pictures.map { it.url },
            initialIndex = index,
            referer = "",
        )
    }

    private fun handleClickDownload() {
        viewModel.createAndNeedPermission(activity = this)
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

    private fun showWarningDialog(positive: () -> Unit) {
        val dialog = WarningDialog(this, clickBack = { status, d ->
            if (status == CenterDefaultDialog.VALUE_POSITIVE) {
                positive.invoke()
            }
            d.dismiss()
        })
        dialog.show()
        dialog.updateALlText(
            content = getString(R.string.if_oval_download),
            negative = getString(R.string.no),
            positive = getString(R.string.yes),
        )
    }

    private fun showDownloadDialog() {
        if (downloadDialog == null) {
            downloadDialog = DownloadTipDialog(activity = this)
        }
        downloadDialog?.show()
    }

    private fun startDownload(forceOverwrite: Boolean = false) {
        if (isDownloading.get()) {
            return
        }
        showDownloadDialog()
        val currentDetail = detailInfo
        val taskId = DownloadQueueManager.enqueueXiurenDownload(
            sourceLabel = getString(R.string.xiuren),
            detailUrl = imageLoadsInfo.href,
            detailTitle = currentDetail.name.ifBlank { "详情" },
            preparedDetailInfo = currentDetail,
            forceOverwrite = forceOverwrite,
        )
        isDownloading.set(true)
        isDownloadingState = true
        downloadObserveJob?.cancel()
        val observerJob = CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                val terminalState = DownloadQueueManager.awaitTaskFinalState(taskId) { state ->
                    isDownloading.set(true)
                    isDownloadingState = true
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
                        showToast("下载完成")
                    }

                    DownloadTaskStatus.FAILED -> {
                        showToast("下载失败")
                    }

                    else -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                isDownloading.set(false)
                isDownloadingState = false
                downloadDialog?.dismiss()
                downloadObserveJob = null
            }
        }
        downloadObserveJob = observerJob
        addJobBindLife(observerJob)
    }
}

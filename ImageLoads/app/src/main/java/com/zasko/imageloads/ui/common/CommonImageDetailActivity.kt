package com.zasko.imageloads.ui.common

import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.compose.EmptyContent
import com.zasko.imageloads.compose.GlideImage
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.fragment.ImagePreviewFragment
import com.zasko.imageloads.utils.FileUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.copyTo

object CommonImageDetailExtras {
    const val KEY_INFO = "common_key_info"
    const val KEY_DATA_USE_FROM = "common_key_data_use_from"
    const val KEY_SOURCE_TYPE = "common_key_source_type"
}

data class CommonImageDetailInfo(
    val url: String = "",
    val title: String = "",
    val subtitles: List<String> = emptyList(),
    val pictures: List<ImageLoadsInfo> = emptyList(),
    val nextPageUrl: String = "",
)

abstract class CommonImageDetailActivity : BaseActivity() {

    private companion object {
        const val MAX_DETAIL_PAGE_COUNT = 50
    }

    protected abstract val logTag: String
    protected abstract val defaultTitle: String
    protected abstract val referer: String

    private var detailInfo by mutableStateOf(CommonImageDetailInfo())
    private var isLoading by mutableStateOf(false)
    private var isLoadingMore by mutableStateOf(false)
    private var isDownloading by mutableStateOf(false)
    private var downloadFinishedCount by mutableStateOf(0)
    private var downloadTotalCount by mutableStateOf(0)
    private var showOverwriteDialog by mutableStateOf(false)
    private var hasDownloaded by mutableStateOf(false)
    private var isFavorite by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")
    private var coverInfo = ImageLoadsInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            },
        )
        coverInfo = readCoverInfo()
        isFavorite = isFavoriteEnabled && isImageFavorite(imageInfo = coverInfo)
        detailInfo = CommonImageDetailInfo(url = coverInfo.href, title = coverInfo.href)
        setContent {
            ImageLoadsTheme {
                CommonImageDetailScreen(
                    detailInfo = detailInfo,
                    defaultTitle = defaultTitle,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    isLoadMoreEnabled = detailInfo.nextPageUrl.isNotBlank() &&
                        !isLoading &&
                        !isLoadingMore &&
                        !isDownloading,
                    isDownloading = isDownloading,
                    downloadText = getDownloadText(),
                    showFavoriteAction = isFavoriteEnabled,
                    isFavorite = isFavorite,
                    showOverwriteDialog = showOverwriteDialog,
                    errorMessage = errorMessage,
                    imageModelProvider = { imageModel(imageInfo = it) },
                    onBack = ::handleBack,
                    onDownload = ::handleDownloadClick,
                    onFavoriteClick = ::handleFavoriteClick,
                    onConfirmOverwrite = {
                        showOverwriteDialog = false
                        startDownload()
                    },
                    onDismissOverwrite = {
                        showOverwriteDialog = false
                    },
                    onLoadMore = ::loadMoreDetail,
                    onImageClick = ::openImagePreview,
                )
            }
        }
        loadDetail(coverInfo = coverInfo)
    }

    protected abstract suspend fun requestDetail(dataUseFrom: Int?, url: String): CommonImageDetailInfo

    protected abstract fun imageModel(imageInfo: ImageLoadsInfo): Any?

    protected abstract fun getDownloadParentDir(): File

    protected open val isFavoriteEnabled: Boolean
        get() = false

    protected open fun isImageFavorite(imageInfo: ImageLoadsInfo): Boolean = false

    protected open fun toggleFavorite(imageInfo: ImageLoadsInfo): Boolean = false

    private fun loadDetail(coverInfo: ImageLoadsInfo) {
        val detailUrl = coverInfo.href.trim()
        if (detailUrl.isBlank()) {
            errorMessage = "缺少详情地址"
            return
        }
        isLoading = true
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                detailInfo = requestDetail(dataUseFrom = readDataUseFrom(), url = detailUrl)
                if (detailInfo.pictures.isEmpty()) {
                    errorMessage = "暂无详情图片"
                } else {
                    errorMessage = ""
                }
                updateHasDownloadState()
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                errorMessage = throwable.message ?: throwable.toString()
                LogComponent.printE(tag = logTag, message = throwable.toString())
            } finally {
                isLoading = false
            }
        }.let(::addJobBindLife)
    }

    private fun loadMoreDetail() {
        val nextUrl = detailInfo.nextPageUrl.trim()
        if (nextUrl.isBlank() || isLoading || isLoadingMore || isDownloading) {
            return
        }
        isLoadingMore = true
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                val nextDetail = requestDetail(dataUseFrom = readDataUseFrom(), url = nextUrl)
                detailInfo = detailInfo.mergePage(nextDetail)
                updateHasDownloadState()
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                LogComponent.printE(tag = logTag, message = "load more detail failed:$throwable")
                showToast("加载更多失败")
            } finally {
                isLoadingMore = false
            }
        }.let(::addJobBindLife)
    }

    private fun handleDownloadClick() {
        if (isDownloading || isLoadingMore || detailInfo.pictures.isEmpty()) {
            return
        }
        if (requestExternalStoragePermissionIfNeeded()) {
            showToast("需要存储权限后再下载")
            return
        }
        FileUtil.createExternalDir()
        if (hasDownloaded) {
            showOverwriteDialog = true
        } else {
            startDownload()
        }
    }

    private fun handleFavoriteClick() {
        if (!isFavoriteEnabled) {
            return
        }
        isFavorite = toggleFavorite(imageInfo = coverInfo)
        showToast(if (isFavorite) "已收藏" else "已取消收藏")
    }

    private fun startDownload() {
        if (isDownloading || detailInfo.pictures.isEmpty()) {
            return
        }
        val context = applicationContext
        var currentDetail = detailInfo
        downloadTotalCount = currentDetail.pictures.size
        downloadFinishedCount = 0
        isDownloading = true
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                currentDetail = loadRemainingDetailPages(currentDetail)
                detailInfo = currentDetail
                downloadTotalCount = currentDetail.pictures.size
                val savedCount = withContext(Dispatchers.IO) {
                    SourceImageDownloadHelper.downloadDetailImages(
                        context = context,
                        detailInfo = currentDetail,
                        parentDir = SourceImageDownloadHelper.getDetailDownloadDir(
                            parentDir = getDownloadParentDir(),
                            detailInfo = currentDetail,
                        ),
                        imageModelProvider = ::imageModel,
                        logTag = logTag,
                        onProgress = { progress ->
                            downloadFinishedCount = progress
                        },
                    )
                }
                hasDownloaded = savedCount > 0
                showToast("已下载 $savedCount/${currentDetail.pictures.size} 张图片")
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                LogComponent.printE(tag = logTag, message = "download detail failed:$throwable")
                showToast("下载失败")
            } finally {
                isDownloading = false
            }
        }.let(::addJobBindLife)
    }

    private suspend fun loadRemainingDetailPages(initialDetail: CommonImageDetailInfo): CommonImageDetailInfo {
        var currentDetail = initialDetail
        val visitedUrls = mutableSetOf<String>().apply {
            currentDetail.url.trim().takeIf { it.isNotBlank() }?.let(::add)
        }
        var nextUrl = currentDetail.nextPageUrl.trim()
        var loadCount = 0
        while (nextUrl.isNotBlank() && loadCount < MAX_DETAIL_PAGE_COUNT && visitedUrls.add(nextUrl)) {
            val nextDetail = requestDetail(dataUseFrom = readDataUseFrom(), url = nextUrl)
            currentDetail = currentDetail.mergePage(nextDetail)
            detailInfo = currentDetail
            nextUrl = currentDetail.nextPageUrl.trim()
            loadCount += 1
        }
        return currentDetail
    }

    private fun requestExternalStoragePermissionIfNeeded(): Boolean {
        return SourceImageDownloadHelper.requestExternalStoragePermissionIfNeeded(activity = this)
    }

    private fun updateHasDownloadState() {
        hasDownloaded = SourceImageDownloadHelper.hasDownloaded(
            parentDir = getDownloadParentDir(),
            detailInfo = detailInfo,
        )
    }

    private fun handleBack() {
        if (showOverwriteDialog) {
            showOverwriteDialog = false
            return
        }
        if (isDownloading) {
            showToast("正在下载中")
            return
        }
        finish()
    }

    private fun getDownloadText(): String {
        return if (isDownloading) {
            "$downloadFinishedCount/$downloadTotalCount"
        } else if (hasDownloaded) {
            "已下载"
        } else {
            "下载"
        }
    }

    private fun openImagePreview(imageInfo: ImageLoadsInfo, index: Int) {
        ImagePreviewFragment.show(
            fragmentManager = supportFragmentManager,
            imageUrls = detailInfo.pictures.map { it.url },
            initialIndex = index,
            referer = referer,
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun readCoverInfo(): ImageLoadsInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(CommonImageDetailExtras.KEY_INFO, ImageLoadsInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(CommonImageDetailExtras.KEY_INFO) as? ImageLoadsInfo
        } ?: ImageLoadsInfo()
    }

    private fun readDataUseFrom(): Int? {
        return if (intent.hasExtra(CommonImageDetailExtras.KEY_DATA_USE_FROM)) {
            intent.getIntExtra(CommonImageDetailExtras.KEY_DATA_USE_FROM, -1)
        } else {
            null
        }
    }
}

private fun CommonImageDetailInfo.mergePage(pageInfo: CommonImageDetailInfo): CommonImageDetailInfo {
    return copy(
        title = title.ifBlank { pageInfo.title },
        subtitles = if (subtitles.isEmpty()) pageInfo.subtitles else subtitles,
        pictures = (pictures + pageInfo.pictures).distinctBy { it.url },
        nextPageUrl = pageInfo.nextPageUrl,
    )
}

@Composable
fun CommonImageDetailScreen(
    detailInfo: CommonImageDetailInfo,
    defaultTitle: String,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    isLoadMoreEnabled: Boolean,
    isDownloading: Boolean,
    downloadText: String,
    showFavoriteAction: Boolean = false,
    isFavorite: Boolean = false,
    showOverwriteDialog: Boolean,
    errorMessage: String,
    imageModelProvider: (ImageLoadsInfo) -> Any?,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onConfirmOverwrite: () -> Unit,
    onDismissOverwrite: () -> Unit,
    onLoadMore: () -> Unit,
    onImageClick: (ImageLoadsInfo, Int) -> Unit,
) {
    var currentImageIndex by remember { mutableStateOf(0) }
    val colorScheme = MaterialTheme.colorScheme
    val detailTitle = remember(detailInfo.pictures.size, currentImageIndex) {
        detailInfo.toIndexTitle(currentImageIndex = currentImageIndex)
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            ImageLoadsTopBar(
                title = detailTitle,
                onBack = onBack,
                actions = {
                    if (showFavoriteAction) {
                        IconButton(onClick = onFavoriteClick) {
                            Icon(
                                painter = painterResource(
                                    id = if (isFavorite) {
                                        R.drawable.baseline_favorite_24
                                    } else {
                                        R.drawable.baseline_favorite_border_24
                                    },
                                ),
                                contentDescription = null,
                                tint = if (isFavorite) Color(0xFFE91E63) else colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TextButton(
                        enabled = detailInfo.pictures.isNotEmpty() && !isDownloading,
                        onClick = onDownload,
                    ) {
                        Text(text = downloadText)
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
            when {
                isLoading && detailInfo.pictures.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorScheme.primary,
                )

                detailInfo.pictures.isEmpty() -> EmptyContent(text = errorMessage.ifBlank { "暂无详情图片" })

                else -> CommonDetailContent(
                    detailInfo = detailInfo,
                    defaultTitle = defaultTitle,
                    isLoadingMore = isLoadingMore,
                    isLoadMoreEnabled = isLoadMoreEnabled,
                    imageModelProvider = imageModelProvider,
                    onLoadMore = onLoadMore,
                    onCurrentImageIndexChanged = { currentImageIndex = it },
                    onImageClick = onImageClick,
                )
            }
            if (showOverwriteDialog) {
                DownloadOverwriteDialog(
                    onConfirm = onConfirmOverwrite,
                    onDismiss = onDismissOverwrite,
                )
            }
        }
    }
}

@Composable
fun DownloadOverwriteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(text = "已下载过，是否覆盖下载？")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "是")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "否")
            }
        },
    )
}

@Composable
private fun CommonDetailContent(
    detailInfo: CommonImageDetailInfo,
    defaultTitle: String,
    isLoadingMore: Boolean,
    isLoadMoreEnabled: Boolean,
    imageModelProvider: (ImageLoadsInfo) -> Any?,
    onLoadMore: () -> Unit,
    onCurrentImageIndexChanged: (Int) -> Unit,
    onImageClick: (ImageLoadsInfo, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme
    val shouldLoadMore by remember(listState, detailInfo.pictures.size, isLoadMoreEnabled) {
        derivedStateOf {
            if (!isLoadMoreEnabled || detailInfo.pictures.size <= 2) {
                false
            } else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
                lastVisible >= detailInfo.pictures.size
            }
        }
    }
    val currentImageIndex by remember(listState, detailInfo.pictures.size) {
        derivedStateOf {
            val imageCount = detailInfo.pictures.size
            if (imageCount == 0) {
                0
            } else {
                listState.layoutInfo.visibleItemsInfo
                    .asSequence()
                    .map { it.index }
                    .filter { it in 1..imageCount }
                    .minOrNull()
                    ?.coerceIn(1, imageCount)
                    ?: 1
            }
        }
    }

    LaunchedEffect(shouldLoadMore, detailInfo.pictures.size) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }
    LaunchedEffect(currentImageIndex) {
        onCurrentImageIndexChanged(currentImageIndex)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            CommonDetailHeader(detailInfo = detailInfo, defaultTitle = defaultTitle)
        }
        itemsIndexed(detailInfo.pictures) { index, imageInfo ->
            GlideImage(
                model = imageModelProvider(imageInfo),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageInfo.displayRatio())
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorScheme.surfaceVariant)
                    .clickable { onImageClick(imageInfo, index) },
                scaleType = ImageView.ScaleType.FIT_CENTER,
            )
        }
        item {
            if (isLoadingMore) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CommonDetailHeader(
    detailInfo: CommonImageDetailInfo,
    defaultTitle: String,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = detailInfo.title.ifBlank { defaultTitle },
            color = colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        detailInfo.subtitles.forEach { text ->
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun ImageLoadsInfo.displayRatio(): Float {
    return if (width > 0 && height > 0) {
        (width.toFloat() / height.toFloat()).coerceIn(0.3f, 2.5f)
    } else {
        2f / 3f
    }
}

private fun CommonImageDetailInfo.toIndexTitle(currentImageIndex: Int): String {
    val totalCount = pictures.size
    return if (totalCount > 0) {
        "${currentImageIndex.coerceIn(1, totalCount)}/$totalCount"
    } else {
        "0/0"
    }
}

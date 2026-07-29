package com.zasko.imageloads.ui.common

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
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
import com.zasko.imageloads.utils.PermissionUtil
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
)

abstract class CommonImageDetailActivity : BaseActivity() {

    protected abstract val logTag: String
    protected abstract val defaultTitle: String
    protected abstract val referer: String

    private var detailInfo by mutableStateOf(CommonImageDetailInfo())
    private var isLoading by mutableStateOf(false)
    private var isDownloading by mutableStateOf(false)
    private var downloadFinishedCount by mutableStateOf(0)
    private var downloadTotalCount by mutableStateOf(0)
    private var showOverwriteDialog by mutableStateOf(false)
    private var hasDownloaded by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")

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
        val coverInfo = readCoverInfo()
        detailInfo = CommonImageDetailInfo(url = coverInfo.href, title = coverInfo.href)
        setContent {
            ImageLoadsTheme {
                CommonImageDetailScreen(
                    detailInfo = detailInfo,
                    defaultTitle = defaultTitle,
                    isLoading = isLoading,
                    isLoadingMore = false,
                    isLoadMoreEnabled = false,
                    isDownloading = isDownloading,
                    downloadText = getDownloadText(),
                    showOverwriteDialog = showOverwriteDialog,
                    errorMessage = errorMessage,
                    imageModelProvider = { imageModel(imageInfo = it) },
                    onBack = ::handleBack,
                    onDownload = ::handleDownloadClick,
                    onConfirmOverwrite = {
                        showOverwriteDialog = false
                        startDownload()
                    },
                    onDismissOverwrite = {
                        showOverwriteDialog = false
                    },
                    onLoadMore = {},
                    onImageClick = ::openImagePreview,
                )
            }
        }
        loadDetail(coverInfo = coverInfo)
    }

    protected abstract suspend fun requestDetail(dataUseFrom: Int?, url: String): CommonImageDetailInfo

    protected abstract fun imageModel(imageInfo: ImageLoadsInfo): Any?

    protected abstract fun getDownloadParentDir(): File

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

    private fun handleDownloadClick() {
        if (isDownloading || detailInfo.pictures.isEmpty()) {
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

    private fun startDownload() {
        if (isDownloading || detailInfo.pictures.isEmpty()) {
            return
        }
        val context = applicationContext
        val currentDetail = detailInfo
        downloadTotalCount = currentDetail.pictures.size
        downloadFinishedCount = 0
        isDownloading = true
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                val savedCount = withContext(Dispatchers.IO) {
                    downloadDetailImages(
                        context = context,
                        detailInfo = currentDetail,
                        parentDir = getDetailDownloadDir(detailInfo = currentDetail),
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

    private suspend fun downloadDetailImages(
        context: Context,
        detailInfo: CommonImageDetailInfo,
        parentDir: File,
    ): Int {
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        var savedCount = 0
        detailInfo.pictures.forEachIndexed { index, imageInfo ->
            val destFile = File(parentDir, imageInfo.url.toImageFileName(index = index))
            runCatching {
                val futureTarget = Glide.with(context)
                    .asFile()
                    .load(imageModel(imageInfo = imageInfo))
                    .submit()
                try {
                    futureTarget.get().copyTo(destFile, overwrite = true)
                } finally {
                    Glide.with(context).clear(futureTarget)
                }
            }.onSuccess {
                savedCount += 1
                LogComponent.printD(tag = logTag, message = "download detail success:${destFile.absolutePath}")
            }.onFailure { throwable ->
                LogComponent.printE(tag = logTag, message = "download detail failed:${imageInfo.url} $throwable")
            }
            withContext(Dispatchers.Main.immediate) {
                downloadFinishedCount = index + 1
            }
        }
        return savedCount
    }

    private fun requestExternalStoragePermissionIfNeeded(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                PermissionUtil.getReadAndWriteExternal(this)
                true
            } else {
                false
            }
        } else if (!PermissionUtil.checkWriteExternal(this)) {
            PermissionUtil.getReadAndWriteExternal(this)
            true
        } else {
            false
        }
    }

    private fun updateHasDownloadState() {
        hasDownloaded = getDetailDownloadDir(detailInfo = detailInfo)
            .listFiles()
            ?.any { it.isFile }
            ?: false
    }

    private fun getDetailDownloadDir(detailInfo: CommonImageDetailInfo): File {
        return File(getDownloadParentDir(), detailInfo.toDownloadFolderName())
    }

    private fun CommonImageDetailInfo.toDownloadFolderName(): String {
        return title.ifBlank { url.trimEnd('/').substringAfterLast('/').substringBefore('?') }
            .ifBlank { "image_detail" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun String.toImageFileName(index: Int): String {
        val fallbackName = "image_${index.toString().padStart(4, '0')}.jpg"
        val rawName = substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() } ?: fallbackName
        return rawName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
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
        } else {
            "下载"
        }
    }

    private fun openImagePreview(imageInfo: ImageLoadsInfo) {
        ImagePreviewFragment.show(
            fragmentManager = supportFragmentManager,
            imageUrl = imageInfo.url,
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

@Composable
fun CommonImageDetailScreen(
    detailInfo: CommonImageDetailInfo,
    defaultTitle: String,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    isLoadMoreEnabled: Boolean,
    isDownloading: Boolean,
    downloadText: String,
    showOverwriteDialog: Boolean,
    errorMessage: String,
    imageModelProvider: (ImageLoadsInfo) -> Any?,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onConfirmOverwrite: () -> Unit,
    onDismissOverwrite: () -> Unit,
    onLoadMore: () -> Unit,
    onImageClick: (ImageLoadsInfo) -> Unit,
) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            ImageLoadsTopBar(
                title = detailInfo.title.ifBlank { defaultTitle },
                onBack = onBack,
                actions = {
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
                .background(Color.White),
        ) {
            when {
                isLoading && detailInfo.pictures.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorResource(id = R.color.teal_700),
                )

                detailInfo.pictures.isEmpty() -> EmptyContent(text = errorMessage.ifBlank { "暂无详情图片" })

                else -> CommonDetailContent(
                    detailInfo = detailInfo,
                    defaultTitle = defaultTitle,
                    isLoadingMore = isLoadingMore,
                    isLoadMoreEnabled = isLoadMoreEnabled,
                    imageModelProvider = imageModelProvider,
                    onLoadMore = onLoadMore,
                    onImageClick = onImageClick,
                )
            }
            if (showOverwriteDialog) {
                AlertDialog(
                    onDismissRequest = onDismissOverwrite,
                    text = {
                        Text(text = "已下载过，是否覆盖下载？")
                    },
                    confirmButton = {
                        TextButton(onClick = onConfirmOverwrite) {
                            Text(text = "是")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismissOverwrite) {
                            Text(text = "否")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CommonDetailContent(
    detailInfo: CommonImageDetailInfo,
    defaultTitle: String,
    isLoadingMore: Boolean,
    isLoadMoreEnabled: Boolean,
    imageModelProvider: (ImageLoadsInfo) -> Any?,
    onLoadMore: () -> Unit,
    onImageClick: (ImageLoadsInfo) -> Unit,
) {
    val listState = rememberLazyListState()
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

    LaunchedEffect(shouldLoadMore, detailInfo.pictures.size) {
        if (shouldLoadMore) {
            onLoadMore()
        }
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
        items(detailInfo.pictures) { imageInfo ->
            GlideImage(
                model = imageModelProvider(imageInfo),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageInfo.displayRatio())
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1F1F1))
                    .clickable { onImageClick(imageInfo) },
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = detailInfo.title.ifBlank { defaultTitle },
            color = colorResource(id = R.color.color_h1),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        detailInfo.subtitles.forEach { text ->
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    color = colorResource(id = R.color.color_h2),
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

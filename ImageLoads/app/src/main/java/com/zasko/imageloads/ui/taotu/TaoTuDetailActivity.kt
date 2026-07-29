package com.zasko.imageloads.ui.taotu

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.compose.EmptyContent
import com.zasko.imageloads.compose.GlideImage
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.PermissionUtil
import com.bumptech.glide.Glide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.copyTo

class TaoTuDetailActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "TaoTuDetailActivity"
        private const val KEY_INFO = "key_info"
        private const val KEY_DATA_USE_FROM = "key_data_use_from"

        fun start(context: Context, info: ImageLoadsInfo, dataUseFrom: Int?) {
            context.startActivity(Intent(context, TaoTuDetailActivity::class.java).apply {
                putExtra(KEY_INFO, info)
                if (dataUseFrom != null) {
                    putExtra(KEY_DATA_USE_FROM, dataUseFrom)
                }
            })
        }
    }

    private var detailInfo by mutableStateOf(TaoTuDetailInfo())
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
        detailInfo = TaoTuDetailInfo(url = coverInfo.href, title = coverInfo.href)
        setContent {
            ImageLoadsTheme {
                TaoTuDetailScreen(
                    detailInfo = detailInfo,
                    isLoading = isLoading,
                    isDownloading = isDownloading,
                    downloadText = getDownloadText(),
                    showOverwriteDialog = showOverwriteDialog,
                    errorMessage = errorMessage,
                    onBack = ::handleBack,
                    onDownload = ::handleDownloadClick,
                    onConfirmOverwrite = {
                        showOverwriteDialog = false
                        startDownload()
                    },
                    onDismissOverwrite = {
                        showOverwriteDialog = false
                    },
                )
            }
        }
        loadDetail(coverInfo = coverInfo)
    }

    private fun loadDetail(coverInfo: ImageLoadsInfo) {
        val detailUrl = coverInfo.href.trim()
        if (detailUrl.isBlank()) {
            errorMessage = "缺少详情地址"
            return
        }
        isLoading = true
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                detailInfo = TaoTuRepository.getDetail(
                    dataUseFrom = readDataUseFrom(),
                    url = detailUrl,
                )
                if (detailInfo.pictures.isEmpty()) {
                    errorMessage = "暂无详情图片"
                }
                updateHasDownloadState()
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                errorMessage = throwable.message ?: throwable.toString()
                LogComponent.printE(tag = TAG, message = throwable.toString())
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
                LogComponent.printE(tag = TAG, message = "download detail failed:$throwable")
                showToast("下载失败")
            } finally {
                isDownloading = false
            }
        }.let(::addJobBindLife)
    }

    private suspend fun downloadDetailImages(
        context: Context,
        detailInfo: TaoTuDetailInfo,
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
                    .load(imageInfo.url.toTaoTuImageModel())
                    .submit()
                try {
                    futureTarget.get().copyTo(destFile, overwrite = true)
                } finally {
                    Glide.with(context).clear(futureTarget)
                }
            }.onSuccess {
                savedCount += 1
                LogComponent.printD(tag = TAG, message = "download detail success:${destFile.absolutePath}")
            }.onFailure { throwable ->
                LogComponent.printE(tag = TAG, message = "download detail failed:${imageInfo.url} $throwable")
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

    private fun getDetailDownloadDir(detailInfo: TaoTuDetailInfo): File {
        return File(
            "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_TAOTU}/${FileUtil.PICTURE_TAOTU_DETAIL}/${detailInfo.toDownloadFolderName()}",
        )
    }

    private fun TaoTuDetailInfo.toDownloadFolderName(): String {
        return title.ifBlank { url.trimEnd('/').substringAfterLast('/').substringBefore('?') }
            .ifBlank { "taotu_detail" }
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
            "${downloadFinishedCount}/${downloadTotalCount}"
        } else {
            "下载"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun readCoverInfo(): ImageLoadsInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(KEY_INFO, ImageLoadsInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(KEY_INFO) as? ImageLoadsInfo
        } ?: ImageLoadsInfo()
    }

    private fun readDataUseFrom(): Int? {
        return if (intent.hasExtra(KEY_DATA_USE_FROM)) {
            intent.getIntExtra(KEY_DATA_USE_FROM, -1)
        } else {
            null
        }
    }
}

@Composable
private fun TaoTuDetailScreen(
    detailInfo: TaoTuDetailInfo,
    isLoading: Boolean,
    isDownloading: Boolean,
    downloadText: String,
    showOverwriteDialog: Boolean,
    errorMessage: String,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onConfirmOverwrite: () -> Unit,
    onDismissOverwrite: () -> Unit,
) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            ImageLoadsTopBar(
                title = detailInfo.title.ifBlank { "TaoTu详情" },
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

                else -> TaoTuDetailContent(detailInfo = detailInfo)
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
private fun TaoTuDetailContent(detailInfo: TaoTuDetailInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
    ) {
        items(detailInfo.pictures) { imageInfo ->
            GlideImage(
                model = imageInfo.url.toTaoTuImageModel(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageInfo.displayRatio())
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1F1F1)),
                scaleType = ImageView.ScaleType.CENTER_CROP,
            )
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

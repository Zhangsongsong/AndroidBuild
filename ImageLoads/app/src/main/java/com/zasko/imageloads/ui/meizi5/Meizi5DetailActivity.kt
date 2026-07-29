package com.zasko.imageloads.ui.meizi5

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.widget.ImageView
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bumptech.glide.Glide
import java.io.File
import kotlin.io.copyTo

class Meizi5DetailActivity : BaseComposeActivity() {

    companion object {
        private const val TAG = "Meizi5DetailActivity"
        private const val KEY_INFO = "key_info"
        private const val KEY_DATA_USE_FROM = "key_data_use_from"

        fun start(context: Context, info: ImageLoadsInfo, dataUseFrom: Int?) {
            context.startActivity(Intent(context, Meizi5DetailActivity::class.java).apply {
                putExtra(KEY_INFO, info)
                if (dataUseFrom != null) {
                    putExtra(KEY_DATA_USE_FROM, dataUseFrom)
                }
            })
        }
    }

    private var detailInfo by mutableStateOf(Meizi5DetailInfo())
    private var isLoading by mutableStateOf(false)
    private var isDownloading by mutableStateOf(false)
    private var showOverwriteDialog by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val coverInfo = readCoverInfo()
        detailInfo = Meizi5DetailInfo(url = coverInfo.href, title = coverInfo.href)
        setContent {
            ImageLoadsTheme {
                Meizi5DetailScreen(
                    detailInfo = detailInfo,
                    isLoading = isLoading,
                    isDownloading = isDownloading,
                    showOverwriteDialog = showOverwriteDialog,
                    errorMessage = errorMessage,
                    onBack = ::finish,
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
                detailInfo = Meizi5Repository.getDetail(
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

    private var hasDownloaded by mutableStateOf(false)

    private fun handleDownloadClick() {
        if (isDownloading || detailInfo.pictures.isEmpty()) {
            return
        }
        FileUtil.createExternalDir()
        if (requestExternalStoragePermissionIfNeeded()) {
            showToast("需要存储权限后再下载")
            return
        }
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
        isDownloading = true
        CoroutineScope(Dispatchers.Main.immediate).launch {
            val savedCount = withContext(Dispatchers.IO) {
                downloadDetailImages(
                    context = context,
                    detailInfo = currentDetail,
                    parentDir = getDetailDownloadDir(detailInfo = currentDetail),
                )
            }
            isDownloading = false
            hasDownloaded = savedCount > 0
            showToast("已下载 $savedCount/${currentDetail.pictures.size} 张图片")
        }.let(::addJobBindLife)
    }

    private fun downloadDetailImages(
        context: Context,
        detailInfo: Meizi5DetailInfo,
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
                    .load(imageInfo.url.toMeizi5ImageModel())
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

    private fun getDetailDownloadDir(detailInfo: Meizi5DetailInfo): File {
        return File(
            "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_MEIZI5}/${FileUtil.PICTURE_MEIZI5_DETAIL}/${detailInfo.toDownloadFolderName()}",
        )
    }

    private fun Meizi5DetailInfo.toDownloadFolderName(): String {
        return title.ifBlank { url.substringAfterLast('/').substringBefore('?') }
            .ifBlank { "meizi5_detail" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun String.toImageFileName(index: Int): String {
        val fallbackName = "image_${index.toString().padStart(4, '0')}.jpg"
        val rawName = substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() } ?: fallbackName
        return rawName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
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
private fun Meizi5DetailScreen(
    detailInfo: Meizi5DetailInfo,
    isLoading: Boolean,
    isDownloading: Boolean,
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
                title = detailInfo.title.ifBlank { "Meizi5详情" },
                onBack = onBack,
                actions = {
                    TextButton(
                        enabled = detailInfo.pictures.isNotEmpty() && !isDownloading,
                        onClick = onDownload,
                    ) {
                        Text(text = if (isDownloading) "下载中" else "下载")
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

                else -> DetailContent(detailInfo = detailInfo)
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
private fun DetailContent(detailInfo: Meizi5DetailInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            DetailHeader(detailInfo = detailInfo)
        }
        items(detailInfo.pictures) { imageInfo ->
            GlideImage(
                model = imageInfo.url.toMeizi5ImageModel(),
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

@Composable
private fun DetailHeader(detailInfo: Meizi5DetailInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = detailInfo.title.ifBlank { "Meizi5详情" },
            color = colorResource(id = R.color.color_h1),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (detailInfo.date.isNotBlank()) {
            Text(
                text = detailInfo.date,
                color = colorResource(id = R.color.color_h2),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (detailInfo.tags.isNotEmpty()) {
            Text(
                text = "标签: ${detailInfo.tags.joinToString(" / ")}",
                color = colorResource(id = R.color.color_h2),
                style = MaterialTheme.typography.bodySmall,
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

package com.zasko.imageloads.ui.common

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Environment
import com.bumptech.glide.Glide
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.PermissionUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.io.copyTo

data class PreparedFavoriteItemDownload(
    val imageInfo: ImageLoadsInfo,
    val detailInfo: CommonImageDetailInfo? = null,
)

object SourceImageDownloadHelper {

    private const val DOWNLOAD_RECORD_ROOT = "download_records"
    private const val RECORD_LINE_URL = "url="
    private const val RECORD_LINE_FOLDER = "folder="

    fun requestExternalStoragePermissionIfNeeded(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                PermissionUtil.getReadAndWriteExternal(activity)
                true
            } else {
                false
            }
        } else if (!PermissionUtil.checkWriteExternal(activity)) {
            PermissionUtil.getReadAndWriteExternal(activity)
            true
        } else {
            false
        }
    }

    fun hasDownloaded(parentDir: File, detailInfo: CommonImageDetailInfo): Boolean {
        return hasDownloadedFiles(getDetailDownloadDir(parentDir = parentDir, detailInfo = detailInfo)) ||
            isDetailHrefDownloaded(parentDir = parentDir, detailHref = detailInfo.url)
    }

    fun getDetailDownloadDir(parentDir: File, detailInfo: CommonImageDetailInfo): File {
        return File(parentDir, detailInfo.toDownloadFolderName())
    }

    fun isDetailHrefDownloaded(sourceType: Int, detailHref: String): Boolean {
        return isDetailHrefDownloaded(
            parentDir = SourceImageDetailDelegate.getDownloadParentDir(sourceType = sourceType),
            detailHref = detailHref,
        )
    }

    fun startFavoriteItemDownload(
        activity: Activity?,
        scope: CoroutineScope?,
        sourceType: Int,
        dataUseFrom: Int?,
        imageInfo: ImageLoadsInfo,
        preparedDetailInfo: CommonImageDetailInfo? = null,
        forceOverwrite: Boolean = false,
        progressMap: MutableMap<String, String>,
        activeJobs: MutableMap<String, Job>,
        showToast: (String) -> Unit,
        onAlreadyDownloaded: (PreparedFavoriteItemDownload) -> Unit = {},
        onDownloadFinished: (ImageLoadsInfo) -> Unit = {},
    ) {
        val hostActivity = activity ?: return
        val requestScope = scope ?: return
        val imageKey = imageInfo.url.trim()
        if (imageKey.isBlank() || activeJobs.containsKey(imageKey)) {
            return
        }
        val detailUrl = imageInfo.href.trim()
        if (detailUrl.isBlank()) {
            showToast("缺少详情地址")
            return
        }
        if (!forceOverwrite && isDetailHrefDownloaded(sourceType = sourceType, detailHref = detailUrl)) {
            onAlreadyDownloaded(PreparedFavoriteItemDownload(imageInfo = imageInfo))
            return
        }
        if (requestExternalStoragePermissionIfNeeded(activity = hostActivity)) {
            showToast("需要存储权限后再下载")
            return
        }
        FileUtil.createExternalDir()

        val context = hostActivity.applicationContext
        val logTag = SourceImageDetailDelegate.logTag(sourceType = sourceType)
        progressMap[imageKey] = "获取中"
        val job = requestScope.launch {
            try {
                val detailInfo = preparedDetailInfo ?: SourceImageDetailDelegate.requestDetail(
                    sourceType = sourceType,
                    dataUseFrom = dataUseFrom,
                    url = detailUrl,
                )
                if (detailInfo.pictures.isEmpty()) {
                    showToast("暂无详情图片")
                    return@launch
                }
                val downloadParentDir = SourceImageDetailDelegate.getDownloadParentDir(sourceType = sourceType)
                val detailDownloadDir = getDetailDownloadDir(
                    parentDir = downloadParentDir,
                    detailInfo = detailInfo,
                )
                val alreadyDownloaded = !forceOverwrite && withContext(Dispatchers.IO) {
                    hasDownloaded(parentDir = downloadParentDir, detailInfo = detailInfo)
                }
                if (alreadyDownloaded) {
                    progressMap.remove(imageKey)
                    activeJobs.remove(imageKey)
                    onAlreadyDownloaded(
                        PreparedFavoriteItemDownload(
                            imageInfo = imageInfo,
                            detailInfo = detailInfo,
                        ),
                    )
                    return@launch
                }
                val completeDetailInfo = SourceImageDetailDelegate.requestRemainingDetailPages(
                    sourceType = sourceType,
                    dataUseFrom = dataUseFrom,
                    detailInfo = detailInfo,
                )
                progressMap[imageKey] = "0/${completeDetailInfo.pictures.size}"
                val savedCount = withContext(Dispatchers.IO) {
                    downloadDetailImages(
                        context = context,
                        detailInfo = completeDetailInfo,
                        parentDir = detailDownloadDir,
                        imageModelProvider = { sourceImage ->
                            SourceImageDetailDelegate.imageModel(
                                sourceType = sourceType,
                                imageInfo = sourceImage,
                            )
                        },
                        logTag = logTag,
                        onProgress = { progress ->
                            progressMap[imageKey] = "$progress/${completeDetailInfo.pictures.size}"
                        },
                    )
                }
                if (savedCount > 0) {
                    onDownloadFinished(imageInfo)
                }
                showToast("已下载 $savedCount/${completeDetailInfo.pictures.size} 张图片")
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                LogComponent.printE(tag = logTag, message = "download favorite detail failed:$throwable")
                showToast("下载失败")
            } finally {
                progressMap.remove(imageKey)
                activeJobs.remove(imageKey)
            }
        }
        activeJobs[imageKey] = job
    }

    suspend fun downloadDetailImages(
        context: Context,
        detailInfo: CommonImageDetailInfo,
        parentDir: File,
        imageModelProvider: (ImageLoadsInfo) -> Any?,
        logTag: String,
        onProgress: suspend (Int) -> Unit = {},
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
                    .load(imageModelProvider(imageInfo))
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
                onProgress(index + 1)
            }
        }
        if (savedCount > 0) {
            parentDir.parentFile?.let { detailParentDir ->
                recordDetailDownload(
                    parentDir = detailParentDir,
                    detailInfo = detailInfo,
                    detailDownloadDir = parentDir,
                )
            }
        }
        return savedCount
    }

    private fun recordDetailDownload(
        parentDir: File,
        detailInfo: CommonImageDetailInfo,
        detailDownloadDir: File,
    ) {
        val detailHref = detailInfo.url.trim()
        if (detailHref.isBlank()) {
            return
        }
        runCatching {
            val recordDir = getDetailDownloadRecordDir(parentDir = parentDir)
            if (!recordDir.exists()) {
                recordDir.mkdirs()
            }
            getDetailDownloadRecordFile(parentDir = parentDir, detailHref = detailHref).writeText(
                buildString {
                    append(RECORD_LINE_URL)
                    append(detailHref)
                    append('\n')
                    append(RECORD_LINE_FOLDER)
                    append(detailDownloadDir.name)
                    append('\n')
                },
            )
        }.onFailure { throwable ->
            LogComponent.printE(tag = "SourceImageDownloadHelper", message = "record detail download failed:$throwable")
        }
    }

    private fun isDetailHrefDownloaded(parentDir: File, detailHref: String): Boolean {
        val normalizedHref = detailHref.trim()
        if (normalizedHref.isBlank()) {
            return false
        }
        return runCatching {
            val recordFile = getDetailDownloadRecordFile(parentDir = parentDir, detailHref = normalizedHref)
            if (!recordFile.isFile) {
                return@runCatching false
            }
            val lines = recordFile.readLines()
            val recordedHref = lines.firstOrNull { it.startsWith(RECORD_LINE_URL) }
                ?.removePrefix(RECORD_LINE_URL)
                ?.trim()
            recordedHref == normalizedHref
        }.getOrDefault(false)
    }

    private fun getDetailDownloadRecordFile(parentDir: File, detailHref: String): File {
        return File(getDetailDownloadRecordDir(parentDir = parentDir), "${detailHref.toSha256()}.txt")
    }

    private fun getDetailDownloadRecordDir(parentDir: File): File {
        return File(
            File(FileUtil.getPrivateDir(), DOWNLOAD_RECORD_ROOT),
            parentDir.absolutePath.toSha256(),
        )
    }

    private fun hasDownloadedFiles(downloadDir: File): Boolean {
        return downloadDir.listFiles()
            ?.any { it.isFile }
            ?: false
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

    private fun String.toSha256(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }
    }
}

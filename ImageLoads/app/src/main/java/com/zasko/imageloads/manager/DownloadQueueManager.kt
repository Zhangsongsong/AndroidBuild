package com.zasko.imageloads.manager

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.zasko.imageloads.MApplication
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.common.CommonImageDetailInfo
import com.zasko.imageloads.ui.common.SourceImageDetailDelegate
import com.zasko.imageloads.ui.common.SourceImageDownloadHelper
import com.zasko.imageloads.ui.common.DynamicSourceStore
import com.zasko.imageloads.ui.generic.GenericSourceRepository
import com.zasko.imageloads.ui.generic.toGenericImageModel
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.FileUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
private enum class DownloadTaskKind {
    COMMON,
    XIUREN,
}

@Serializable
enum class DownloadTaskStatus {
    QUEUED,
    PREPARING,
    DOWNLOADING,
    SUCCEEDED,
    FAILED,
}

@Serializable
private data class DownloadTaskRecord(
    val id: String,
    val kind: DownloadTaskKind,
    val sourceType: Int,
    val sourceKey: String,
    val sourceLabel: String,
    val detailUrl: String,
    val detailTitle: String,
    val dataUseFrom: Int? = null,
    val forceOverwrite: Boolean = false,
    val status: DownloadTaskStatus = DownloadTaskStatus.QUEUED,
    val totalCount: Int = 0,
    val finishedCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class DownloadTaskUiState(
    val id: String,
    val sourceLabel: String,
    val detailTitle: String,
    val status: DownloadTaskStatus,
    val totalCount: Int,
    val finishedCount: Int,
    val progressFraction: Float?,
    val progressText: String,
)

object DownloadQueueManager {

    private const val TAG = "DownloadQueueManager"
    private const val PREF_NAME = "download_queue_manager"
    private const val PREF_KEY_TASKS = "download_queue_tasks"
    private const val REMOVE_DELAY_MS = 800L
    private const val MAX_XIUREN_PAGE_COUNT = 50
    private const val XIUREN_DOMAIN = "https://xiutaku.com"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val runningJobs = mutableMapOf<String, Job>()
    private val taskRecords = MutableStateFlow<List<DownloadTaskRecord>>(emptyList())
    private var initialized = false

    val activeDownloads: StateFlow<List<DownloadTaskUiState>> = taskRecords
        .map { records ->
            records
                .filter { it.status.isActive() }
                .sortedByDescending { it.createdAt }
                .map { it.toUiState() }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun bootstrap() {
        synchronized(lock) {
            if (initialized) {
                return
            }
            initialized = true
        }
        val persistedRecords = readPersistedRecords()
            .filter { it.status.isActive() }
        synchronized(lock) {
            taskRecords.value = persistedRecords
            persistLocked(persistedRecords)
        }
        persistedRecords.forEach { record ->
            launchTaskIfNeeded(record = record, preparedCommonDetailInfo = null, preparedXiurenDetailInfo = null)
        }
    }

    fun observeTask(taskId: String): Flow<DownloadTaskUiState?> {
        return taskRecords
            .map { records -> records.firstOrNull { it.id == taskId }?.toUiState() }
            .distinctUntilChanged()
    }

    suspend fun awaitTaskFinalState(
        taskId: String,
        onUpdate: suspend (DownloadTaskUiState) -> Unit = {},
    ): DownloadTaskUiState? {
        return observeTask(taskId)
            .filterNotNull()
            .onEach(onUpdate)
            .first { it.status.isTerminal() }
    }

    fun buildCommonTaskId(sourceType: Int, sourceKey: String, detailUrl: String): String {
        return buildTaskId(
            kind = DownloadTaskKind.COMMON,
            sourceType = sourceType,
            sourceKey = sourceKey,
            detailUrl = detailUrl,
        )
    }

    fun buildXiurenTaskId(detailUrl: String): String {
        return buildTaskId(
            kind = DownloadTaskKind.XIUREN,
            sourceType = Constants.THEME_TYPE_XIUREN,
            sourceKey = "xiuren",
            detailUrl = detailUrl,
        )
    }

    fun enqueueCommonDownload(
        sourceType: Int,
        sourceKey: String,
        sourceLabel: String,
        dataUseFrom: Int?,
        detailUrl: String,
        detailTitle: String,
        preparedDetailInfo: CommonImageDetailInfo? = null,
        forceOverwrite: Boolean = false,
    ): String {
        val taskId = buildCommonTaskId(
            sourceType = sourceType,
            sourceKey = sourceKey,
            detailUrl = detailUrl,
        )
        val record = DownloadTaskRecord(
            id = taskId,
            kind = DownloadTaskKind.COMMON,
            sourceType = sourceType,
            sourceKey = sourceKey,
            sourceLabel = sourceLabel.ifBlank { defaultSourceLabel(sourceType = sourceType, sourceKey = sourceKey) },
            detailUrl = detailUrl,
            detailTitle = detailTitle.ifBlank { "获取详情中" },
            dataUseFrom = dataUseFrom,
            forceOverwrite = forceOverwrite,
        )
        if (!upsertRecord(record)) {
            return taskId
        }
        launchTaskIfNeeded(
            record = record,
            preparedCommonDetailInfo = preparedDetailInfo,
            preparedXiurenDetailInfo = null,
        )
        return taskId
    }

    fun enqueueXiurenDownload(
        sourceLabel: String,
        detailUrl: String,
        detailTitle: String,
        preparedDetailInfo: ImageDetailInfo? = null,
        forceOverwrite: Boolean = false,
    ): String {
        val taskId = buildXiurenTaskId(detailUrl = detailUrl)
        val record = DownloadTaskRecord(
            id = taskId,
            kind = DownloadTaskKind.XIUREN,
            sourceType = Constants.THEME_TYPE_XIUREN,
            sourceKey = "xiuren",
            sourceLabel = sourceLabel.ifBlank { "XiuRen" },
            detailUrl = detailUrl,
            detailTitle = detailTitle.ifBlank { "获取详情中" },
            forceOverwrite = forceOverwrite,
        )
        if (!upsertRecord(record)) {
            return taskId
        }
        launchTaskIfNeeded(
            record = record,
            preparedCommonDetailInfo = null,
            preparedXiurenDetailInfo = preparedDetailInfo,
        )
        return taskId
    }

    private fun buildTaskId(
        kind: DownloadTaskKind,
        sourceType: Int,
        sourceKey: String,
        detailUrl: String,
    ): String {
        return "${kind.name}|$sourceType|${sourceKey.trim().lowercase()}|${detailUrl.trim()}".toSha256()
    }

    private fun upsertRecord(record: DownloadTaskRecord): Boolean {
        synchronized(lock) {
            val current = taskRecords.value
            val existing = current.firstOrNull { it.id == record.id }
            if (existing?.status?.isActive() == true) {
                return false
            }
            val next = current
                .filterNot { it.id == record.id }
                .plus(
                    record.copy(
                        createdAt = existing?.createdAt ?: record.createdAt,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            taskRecords.value = next
            persistLocked(next)
            return true
        }
    }

    private fun updateRecord(taskId: String, block: (DownloadTaskRecord) -> DownloadTaskRecord) {
        synchronized(lock) {
            val current = taskRecords.value
            val index = current.indexOfFirst { it.id == taskId }
            if (index < 0) {
                return
            }
            val updated = block(current[index]).copy(updatedAt = System.currentTimeMillis())
            val next = current.toMutableList().apply {
                this[index] = updated
            }
            taskRecords.value = next
            persistLocked(next)
        }
    }

    private fun completeRecord(taskId: String, status: DownloadTaskStatus) {
        updateRecord(taskId) { record ->
            record.copy(
                status = status,
                finishedCount = if (status == DownloadTaskStatus.SUCCEEDED) {
                    record.totalCount.coerceAtLeast(record.finishedCount)
                } else {
                    record.finishedCount
                },
            )
        }
        scope.launch {
            delay(REMOVE_DELAY_MS)
            removeRecord(taskId = taskId)
        }
    }

    private fun removeRecord(taskId: String) {
        synchronized(lock) {
            val next = taskRecords.value.filterNot { it.id == taskId }
            if (next.size == taskRecords.value.size) {
                return
            }
            taskRecords.value = next
            persistLocked(next)
        }
    }

    private fun launchTaskIfNeeded(
        record: DownloadTaskRecord,
        preparedCommonDetailInfo: CommonImageDetailInfo?,
        preparedXiurenDetailInfo: ImageDetailInfo?,
    ) {
        synchronized(lock) {
            if (runningJobs.containsKey(record.id)) {
                return
            }
            runningJobs[record.id] = scope.launch {
                try {
                    when (record.kind) {
                        DownloadTaskKind.COMMON -> runCommonDownload(
                            record = record,
                            preparedDetailInfo = preparedCommonDetailInfo,
                        )

                        DownloadTaskKind.XIUREN -> runXiurenDownload(
                            record = record,
                            preparedDetailInfo = preparedXiurenDetailInfo,
                        )
                    }
                } finally {
                    synchronized(lock) {
                        runningJobs.remove(record.id)
                    }
                }
            }
        }
    }

    private suspend fun runCommonDownload(
        record: DownloadTaskRecord,
        preparedDetailInfo: CommonImageDetailInfo?,
    ) {
        try {
            updateRecord(record.id) { it.copy(status = DownloadTaskStatus.PREPARING) }
            val detailInfo = preparedDetailInfo ?: requestCommonDetail(record = record)
            val completeDetailInfo = requestCommonRemainingPages(
                record = record,
                detailInfo = detailInfo,
            )
            if (completeDetailInfo.pictures.isEmpty()) {
                throw IllegalStateException("暂无详情图片")
            }
            updateRecord(record.id) {
                it.copy(
                    detailTitle = completeDetailInfo.title.ifBlank { it.detailTitle },
                    totalCount = completeDetailInfo.pictures.size,
                    finishedCount = 0,
                    status = DownloadTaskStatus.DOWNLOADING,
                )
            }
            val savedCount = withContext(Dispatchers.IO) {
                SourceImageDownloadHelper.downloadDetailImages(
                    context = MApplication.application.applicationContext,
                    detailInfo = completeDetailInfo,
                    parentDir = commonParentDir(record = record, detailInfo = completeDetailInfo),
                    imageModelProvider = { imageInfo ->
                        commonImageModel(record = record, imageInfo = imageInfo)
                    },
                    logTag = commonLogTag(record = record),
                    replaceExisting = record.forceOverwrite,
                    onProgress = { progress ->
                        updateRecord(record.id) {
                            it.copy(
                                finishedCount = progress,
                                totalCount = completeDetailInfo.pictures.size,
                            )
                        }
                    },
                )
            }
            if (savedCount <= 0) {
                throw IllegalStateException("下载失败")
            }
            completeRecord(taskId = record.id, status = DownloadTaskStatus.SUCCEEDED)
        } catch (e: CancellationException) {
            throw e
        } catch (throwable: Throwable) {
            LogComponent.printE(tag = TAG, message = "common download failed:${record.detailUrl} $throwable")
            completeRecord(taskId = record.id, status = DownloadTaskStatus.FAILED)
        }
    }

    private suspend fun runXiurenDownload(
        record: DownloadTaskRecord,
        preparedDetailInfo: ImageDetailInfo?,
    ) {
        try {
            updateRecord(record.id) { it.copy(status = DownloadTaskStatus.PREPARING) }
            val initialDetail = preparedDetailInfo ?: requestXiurenDetail(record = record)
            val completeDetailInfo = requestXiurenRemainingPages(
                record = record,
                initialDetail = initialDetail,
            )
            val commonDetailInfo = initialDetail.toCommonDetailInfo(
                detailUrl = record.detailUrl,
                imageInfos = completeDetailInfo.pictures.orEmpty(),
            )
            if (commonDetailInfo.pictures.isEmpty()) {
                throw IllegalStateException("暂无详情图片")
            }
            updateRecord(record.id) {
                it.copy(
                    detailTitle = commonDetailInfo.title.ifBlank { it.detailTitle },
                    totalCount = commonDetailInfo.pictures.size,
                    finishedCount = 0,
                    status = DownloadTaskStatus.DOWNLOADING,
                )
            }
            val savedCount = withContext(Dispatchers.IO) {
                SourceImageDownloadHelper.downloadDetailImages(
                    context = MApplication.application.applicationContext,
                    detailInfo = commonDetailInfo,
                    parentDir = commonDetailParentDir(record = record, detailInfo = commonDetailInfo),
                    imageModelProvider = { imageInfo -> imageInfo.url },
                    logTag = "XiuRenDownload",
                    replaceExisting = record.forceOverwrite,
                    onProgress = { progress ->
                        updateRecord(record.id) {
                            it.copy(
                                finishedCount = progress,
                                totalCount = commonDetailInfo.pictures.size,
                            )
                        }
                    },
                )
            }
            if (savedCount <= 0) {
                throw IllegalStateException("下载失败")
            }
            completeRecord(taskId = record.id, status = DownloadTaskStatus.SUCCEEDED)
        } catch (e: CancellationException) {
            throw e
        } catch (throwable: Throwable) {
            LogComponent.printE(tag = TAG, message = "xiuren download failed:${record.detailUrl} $throwable")
            completeRecord(taskId = record.id, status = DownloadTaskStatus.FAILED)
        }
    }

    private suspend fun requestCommonDetail(record: DownloadTaskRecord): CommonImageDetailInfo {
        return if (record.sourceKey.isBlank()) {
            SourceImageDetailDelegate.requestDetail(
                sourceType = record.sourceType,
                dataUseFrom = record.dataUseFrom,
                url = record.detailUrl,
            )
        } else {
            val config = DynamicSourceStore.getConfig(sourceKey = record.sourceKey)
                ?: throw IllegalArgumentException("来源配置不存在")
            GenericSourceRepository.getDetail(
                config = config,
                dataUseFrom = record.dataUseFrom,
                url = record.detailUrl,
            )
        }
    }

    private suspend fun requestCommonRemainingPages(
        record: DownloadTaskRecord,
        detailInfo: CommonImageDetailInfo,
    ): CommonImageDetailInfo {
        return if (record.sourceKey.isBlank()) {
            SourceImageDetailDelegate.requestRemainingDetailPages(
                sourceType = record.sourceType,
                dataUseFrom = record.dataUseFrom,
                detailInfo = detailInfo,
            )
        } else {
            val config = DynamicSourceStore.getConfig(sourceKey = record.sourceKey)
                ?: throw IllegalArgumentException("来源配置不存在")
            GenericSourceRepository.requestRemainingDetailPages(
                config = config,
                dataUseFrom = record.dataUseFrom,
                detailInfo = detailInfo,
            )
        }
    }

    private suspend fun requestXiurenDetail(record: DownloadTaskRecord): ImageDetailInfo {
        return withContext(Dispatchers.IO) {
            val detailUrl = xiurenPageUrl(detailUrl = record.detailUrl, page = 1)
            com.zasko.imageloads.manager.ImageLoadsManager.getXiuRenDetail(url = detailUrl).blockingGet()
        }
    }

    private suspend fun requestXiurenRemainingPages(
        record: DownloadTaskRecord,
        initialDetail: ImageDetailInfo,
    ): ImageDetailInfo {
        var currentDetail = initialDetail
        val images = mutableListOf<ImageInfo>().apply {
            addAll(initialDetail.pictures.orEmpty())
        }
        var page = 2
        var lastImageUrl = images.lastOrNull()?.url.orEmpty()
        while (page <= MAX_XIUREN_PAGE_COUNT) {
            val nextPageImages = withContext(Dispatchers.IO) {
                com.zasko.imageloads.manager.ImageLoadsManager
                    .getXiuRenDetailMore(url = xiurenPageUrl(detailUrl = record.detailUrl, page = page))
                    .blockingGet()
            }
            if (nextPageImages.isEmpty()) {
                break
            }
            val nextLastUrl = nextPageImages.lastOrNull()?.url.orEmpty()
            if (nextLastUrl == lastImageUrl) {
                break
            }
            images.addAll(nextPageImages)
            lastImageUrl = nextLastUrl
            page += 1
        }
        return currentDetail.copy(
            pictures = images.distinctBy { it.url },
        )
    }

    private fun xiurenPageUrl(detailUrl: String, page: Int): String {
        val normalizedUrl = detailUrl.trim()
        if (normalizedUrl.isBlank()) {
            return "$XIUREN_DOMAIN/?page=$page"
        }
        val pageRegex = Regex("""([?&]page=)\d+""")
        return if (pageRegex.containsMatchIn(normalizedUrl)) {
            normalizedUrl.replace(pageRegex, "$1$page")
        } else if (normalizedUrl.contains("?")) {
            "$normalizedUrl&page=$page"
        } else {
            "$normalizedUrl?page=$page"
        }
    }

    private fun commonParentDir(record: DownloadTaskRecord, detailInfo: CommonImageDetailInfo): File {
        return when {
            record.sourceKey.isNotBlank() -> File("${FileUtil.getDownloadPath()}/${record.sourceKey}/detail")
            else -> SourceImageDetailDelegate.getDownloadParentDir(sourceType = record.sourceType)
        }.also { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
    }

    private fun commonDetailParentDir(record: DownloadTaskRecord, detailInfo: CommonImageDetailInfo): File {
        val parent = File("${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_XIUREN}")
        if (!parent.exists()) {
            parent.mkdirs()
        }
        return parent
    }

    private fun commonImageModel(record: DownloadTaskRecord, imageInfo: ImageLoadsInfo): Any? {
        return if (record.sourceKey.isNotBlank()) {
            imageInfo.url.toGenericImageModel()
        } else {
            SourceImageDetailDelegate.imageModel(sourceType = record.sourceType, imageInfo = imageInfo)
        }
    }

    private fun commonLogTag(record: DownloadTaskRecord): String {
        return if (record.sourceKey.isNotBlank()) {
            "GenericDetail:${record.sourceKey}"
        } else {
            SourceImageDetailDelegate.logTag(sourceType = record.sourceType)
        }
    }

    private fun defaultSourceLabel(sourceType: Int, sourceKey: String): String {
        return if (sourceKey.isNotBlank()) {
            sourceKey
        } else {
            when (sourceType) {
                Constants.THEME_TYPE_MEIZI5 -> "Meizi5"
                Constants.THEME_TYPE_TAOTU -> "TaoTu"
                Constants.THEME_TYPE_TRENDSZINE -> "Trendszine"
                Constants.THEME_TYPE_XIUREN -> "XiuRen"
                else -> "详情"
            }
        }
    }

    private fun DownloadTaskRecord.toUiState(): DownloadTaskUiState {
        val progress = when {
            status == DownloadTaskStatus.DOWNLOADING && totalCount > 0 -> {
                finishedCount.coerceAtMost(totalCount).toFloat() / totalCount.toFloat()
            }
            else -> null
        }
        return DownloadTaskUiState(
            id = id,
            sourceLabel = sourceLabel,
            detailTitle = detailTitle.ifBlank { "获取详情中" },
            status = status,
            totalCount = totalCount,
            finishedCount = finishedCount,
            progressFraction = progress,
            progressText = when (status) {
                DownloadTaskStatus.QUEUED -> "等待中"
                DownloadTaskStatus.PREPARING -> "获取详情"
                DownloadTaskStatus.DOWNLOADING -> {
                    if (totalCount > 0) {
                        "$finishedCount/$totalCount"
                    } else {
                        "下载中"
                    }
                }
                DownloadTaskStatus.SUCCEEDED -> "已完成"
                DownloadTaskStatus.FAILED -> "下载失败"
            },
        )
    }

    private fun DownloadTaskStatus.isActive(): Boolean {
        return this == DownloadTaskStatus.QUEUED ||
            this == DownloadTaskStatus.PREPARING ||
            this == DownloadTaskStatus.DOWNLOADING
    }

    private fun readPersistedRecords(): List<DownloadTaskRecord> {
        return runCatching {
            val raw = MApplication.application
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(PREF_KEY_TASKS, "[]")
                .orEmpty()
            if (raw.isBlank()) {
                emptyList()
            } else {
                json.decodeFromString(ListSerializer(DownloadTaskRecord.serializer()), raw)
            }
        }.getOrElse {
            LogComponent.printE(tag = TAG, message = "read persisted tasks failed:$it")
            emptyList()
        }
    }

    private fun persistLocked(records: List<DownloadTaskRecord>) {
        runCatching {
            val raw = json.encodeToString(ListSerializer(DownloadTaskRecord.serializer()), records)
            MApplication.application
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY_TASKS, raw)
                .apply()
        }.onFailure {
            LogComponent.printE(tag = TAG, message = "persist tasks failed:$it")
        }
    }

    private fun String.toSha256(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString(separator = "") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun DownloadTaskStatus.isTerminal(): Boolean = !isActive()
}

private fun ImageDetailInfo.toCommonDetailInfo(
    detailUrl: String,
    imageInfos: List<ImageInfo> = this.pictures.orEmpty(),
): CommonImageDetailInfo {
    val subtitles = mutableListOf<String>()
    if (time.isNotBlank()) {
        subtitles.add(time)
    }
    if (desc.isNotBlank()) {
        subtitles.add(desc)
    }
    return CommonImageDetailInfo(
        url = detailUrl,
        title = name,
        subtitles = subtitles,
        pictures = imageInfos.map { it.toImageLoadsInfo() },
    )
}

private fun ImageInfo.toImageLoadsInfo(): ImageLoadsInfo {
    return ImageLoadsInfo(
        url = url,
        width = width,
        height = height,
    )
}

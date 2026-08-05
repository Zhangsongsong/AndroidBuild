package com.zasko.imageloads.ui.generic

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.compose.ImageListScreen
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.fragment.ComposeBaseFragment
import com.zasko.imageloads.ui.common.DownloadOverwriteDialog
import com.zasko.imageloads.ui.common.DynamicSourceConfig
import com.zasko.imageloads.ui.common.DynamicSourceStore
import com.zasko.imageloads.ui.common.FavoriteBulkDownloadDialog
import com.zasko.imageloads.ui.common.FavoriteBulkDownloadDialogState
import com.zasko.imageloads.ui.common.FavoriteBulkDownloadPlan
import com.zasko.imageloads.ui.common.PreparedFavoriteItemDownload
import com.zasko.imageloads.ui.common.SourceImageDownloadHelper
import com.zasko.imageloads.utils.FileUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

class GenericSourceFragment : ComposeBaseFragment() {

    companion object {
        private const val TAG = "GenericSourceFragment"
        private const val KEY_SHOW_FAVORITES = "generic_key_show_favorites"

        fun newInstance(data: MainThemeSelectInfo, showFavoritesOnly: Boolean = false): GenericSourceFragment {
            return GenericSourceFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_DATA, data)
                    putBoolean(KEY_SHOW_FAVORITES, showFavoritesOnly)
                }
            }
        }
    }

    private enum class LoadMode {
        Refresh,
        More,
    }

    private val images = mutableStateListOf<ImageLoadsInfo>()
    private val favoriteImages = mutableStateListOf<ImageLoadsInfo>()
    private val pageLabels = mutableStateMapOf<Int, Int>()
    private val favoriteDownloadProgress = mutableStateMapOf<String, String>()
    private val favoriteDownloadJobs = mutableMapOf<String, Job>()
    private val downloadedFavoriteImageUrls = mutableStateListOf<String>()

    private var dataInfo: MainThemeSelectInfo? = null
    private var sourceConfig: DynamicSourceConfig? = null
    private var nextPage = 1
    private var isRefreshing by mutableStateOf(false)
    private var isLoadingMoreState by mutableStateOf(false)
    private var showFavoritesOnly by mutableStateOf(false)
    private var openedFavoritesOnly = false
    private var activeRequest: Job? = null
    private var favoriteBulkDownloadJob: Job? = null
    private var pendingOverwriteDownload by mutableStateOf<PreparedFavoriteItemDownload?>(null)
    private var showBulkDownloadDialog by mutableStateOf(false)
    private var favoriteBulkDownloadPlan by mutableStateOf(FavoriteBulkDownloadPlan())
    private var favoriteBulkDownloadState by mutableStateOf(FavoriteBulkDownloadDialogState())
    private var requestVersion = 0

    private val isFavoriteBulkDownloading: Boolean
        get() = favoriteBulkDownloadState.isDownloading

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataInfo = readThemeInfo()
        sourceConfig = dataInfo?.sourceKey?.let(DynamicSourceStore::getConfig)
        showFavoritesOnly = arguments?.getBoolean(KEY_SHOW_FAVORITES) == true
        openedFavoritesOnly = showFavoritesOnly
        refreshFavorites()
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            },
        )
    }

    override fun onResume() {
        super.onResume()
        refreshFavorites()
    }

    @Composable
    override fun FragmentContent() {
        val displayImages = if (showFavoritesOnly) favoriteImages else images
        ImageLoadsTheme {
            ImageListScreen(
                title = screenTitle(),
                images = displayImages,
                isRefreshing = !showFavoritesOnly && isRefreshing,
                isLoadingMore = !showFavoritesOnly && isLoadingMoreState,
                onBack = ::handleBack,
                onOpenWeb = { openUrl(dataInfo?.baseUrl.orEmpty()) },
                onLoadMore = {
                    if (!showFavoritesOnly) {
                        loadMoreData()
                    }
                },
                onImageClick = ::handleImageClick,
                pageLabelProvider = if (showFavoritesOnly) {
                    { _, _ -> null }
                } else {
                    ::pageLabelFor
                },
                imageModelProvider = { it.url.toGenericImageModel() },
                imageRatioProvider = { it.genericDisplayRatio() },
                imageScaleType = ImageView.ScaleType.CENTER_CROP,
                showWebAction = !showFavoritesOnly,
                showActionMenu = !showFavoritesOnly,
                showDownloadMenuAction = false,
                showPageJumpMenuAction = true,
                showFavoriteMenuAction = !showFavoritesOnly,
                favoriteMenuText = "收藏",
                showDownloadAllAction = showFavoritesOnly,
                isDownloadAllActionEnabled = !isFavoriteBulkDownloading && favoriteImages.isNotEmpty(),
                pageJumpInitialPage = currentFirstPage(),
                showFavoriteAction = true,
                favoriteImageKeys = favoriteImages.map { it.url }.toSet(),
                showItemDownloadAction = showFavoritesOnly && !isFavoriteBulkDownloading,
                downloadingImageKeys = favoriteDownloadProgress.keys.toSet(),
                downloadedImageKeys = downloadedFavoriteImageUrls.toSet(),
                imageKeyProvider = { it.url },
                itemDownloadProgressProvider = { favoriteDownloadProgress[it.url] },
                onDownloadAllClick = ::showFavoriteBulkDownloadDialog,
                onPageJump = ::jumpToPage,
                onFavoriteMenuClick = ::toggleFavoriteList,
                onFavoriteClick = ::toggleFavorite,
                onItemDownloadClick = ::downloadFavoriteItem,
            )
            pendingOverwriteDownload?.let { pendingDownload ->
                DownloadOverwriteDialog(
                    onConfirm = {
                        pendingOverwriteDownload = null
                        startFavoriteItemDownload(
                            imageInfo = pendingDownload.imageInfo,
                            preparedDownload = pendingDownload,
                            forceOverwrite = true,
                        )
                    },
                    onDismiss = {
                        pendingOverwriteDownload = null
                    },
                )
            }
            if (showBulkDownloadDialog) {
                FavoriteBulkDownloadDialog(
                    state = favoriteBulkDownloadState,
                    onConfirm = ::startFavoriteBulkDownload,
                    onDismiss = {
                        if (!isFavoriteBulkDownloading) {
                            showBulkDownloadDialog = false
                        }
                    },
                )
            }
        }
    }

    override fun initByResume() {
        super.initByResume()
        if (!showFavoritesOnly) {
            loadNewData()
        }
    }

    override fun onClearComposeView() {
        activeRequest?.cancel()
        favoriteBulkDownloadJob?.cancel()
        favoriteDownloadJobs.values.forEach { it.cancel() }
    }

    private fun screenTitle(): String {
        return if (showFavoritesOnly) {
            "${dataInfo?.title.orEmpty()} 收藏"
        } else {
            dataInfo?.title.orEmpty()
        }
    }

    private fun loadNewData() {
        if (isLoadingMore.get()) {
            isRefreshing = false
            return
        }
        requestImages(mode = LoadMode.Refresh)
    }

    private fun loadMoreData() {
        if (!isRefreshing && !isLoadingMore.get() && !isLoadEnd.get()) {
            requestImages(mode = LoadMode.More)
        }
    }

    private fun requestImages(mode: LoadMode, targetPage: Int? = null) {
        val scope = composeRequestScope ?: return
        val config = sourceConfig ?: return
        val page = when (mode) {
            LoadMode.Refresh -> targetPage?.coerceAtLeast(1) ?: 1
            LoadMode.More -> nextPage
        }
        val requestId = nextRequestId()
        activeRequest?.cancel()
        beginLoading(mode = mode, page = page)
        activeRequest = scope.launch {
            try {
                val result = GenericSourceRepository.getImages(
                    config = config,
                    dataUseFrom = dataInfo?.dataUseFrom,
                    page = page,
                )
                if (isActiveRequest(requestId = requestId, scope = scope)) {
                    applyLoadedImages(mode = mode, page = page, result = result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                LogComponent.printE(tag = TAG, message = throwable.toString())
                showToast("加载失败")
            } finally {
                if (isActiveRequest(requestId = requestId, scope = scope)) {
                    endLoading(mode = mode)
                    activeRequest = null
                }
            }
        }
    }

    private fun beginLoading(mode: LoadMode, page: Int) {
        when (mode) {
            LoadMode.Refresh -> {
                nextPage = page
                isLoadEnd.set(false)
                isRefreshing = true
            }

            LoadMode.More -> {
                isLoadingMore.set(true)
                isLoadingMoreState = true
            }
        }
    }

    private fun endLoading(mode: LoadMode) {
        when (mode) {
            LoadMode.Refresh -> isRefreshing = false
            LoadMode.More -> {
                isLoadingMore.set(false)
                isLoadingMoreState = false
            }
        }
    }

    private fun applyLoadedImages(mode: LoadMode, page: Int, result: GenericSourcePageResult) {
        when (mode) {
            LoadMode.Refresh -> {
                pageLabels.clear()
                if (result.images.isNotEmpty()) {
                    pageLabels[0] = page
                    sourceConfig?.let { config ->
                        SourceLocalDataStore.saveCover(targetId = config.key, cover = result.images.first().url)
                    }
                }
                images.clear()
                images.addAll(result.images)
            }

            LoadMode.More -> {
                if (result.images.isNotEmpty()) {
                    pageLabels[images.size] = page
                }
                images.addAll(result.images)
            }
        }
        nextPage = result.nextPage ?: nextPage
        isLoadEnd.set(result.images.isEmpty() || result.nextPage == null)
    }

    private fun pageLabelFor(index: Int, imageInfo: ImageLoadsInfo): String? {
        return pageLabels[index]?.let { "第 $it 页" }
    }

    private fun currentFirstPage(): Int {
        return pageLabels[0] ?: maxOf(1, nextPage - 1)
    }

    private fun jumpToPage(page: Int) {
        if (showFavoritesOnly || isRefreshing || isLoadingMore.get()) {
            return
        }
        images.clear()
        pageLabels.clear()
        requestImages(mode = LoadMode.Refresh, targetPage = page)
    }

    private fun handleImageClick(imageInfo: ImageLoadsInfo) {
        if (favoriteDownloadProgress.containsKey(imageInfo.url)) {
            return
        }
        dataInfo?.let { data ->
            GenericImageDetailActivity.start(
                context = requireContext(),
                data = data,
                info = imageInfo.copy(fromType = sourceConfig?.type ?: imageInfo.fromType),
            )
        }
    }

    private fun toggleFavoriteList() {
        showFavoritesOnly = true
        refreshFavorites()
    }

    private fun refreshFavorites() {
        val config = sourceConfig ?: return
        favoriteImages.clear()
        favoriteImages.addAll(GenericFavoriteStore.getFavorites(config = config))
        refreshDownloadedFavoriteState()
    }

    private fun toggleFavorite(imageInfo: ImageLoadsInfo) {
        val config = sourceConfig ?: return
        val isFavorite = GenericFavoriteStore.toggleFavorite(config = config, imageInfo = imageInfo)
        refreshFavorites()
        showToast(if (isFavorite) "已收藏" else "已取消收藏")
    }

    private fun refreshDownloadedFavoriteState() {
        downloadedFavoriteImageUrls.clear()
        downloadedFavoriteImageUrls.addAll(
            favoriteImages.filter {
                SourceImageDownloadHelper.isDetailHrefDownloaded(
                    parentDir = getDownloadParentDir(),
                    detailHref = it.href,
                )
            }.map { it.url },
        )
    }

    private fun downloadFavoriteItem(imageInfo: ImageLoadsInfo) {
        if (isFavoriteBulkDownloading) {
            return
        }
        if (SourceImageDownloadHelper.isDetailHrefDownloaded(parentDir = getDownloadParentDir(), detailHref = imageInfo.href)) {
            pendingOverwriteDownload = PreparedFavoriteItemDownload(imageInfo = imageInfo)
            return
        }
        startFavoriteItemDownload(imageInfo = imageInfo)
    }

    private fun startFavoriteItemDownload(
        imageInfo: ImageLoadsInfo,
        preparedDownload: PreparedFavoriteItemDownload? = null,
        forceOverwrite: Boolean = false,
    ) {
        val config = sourceConfig ?: return
        val scope = composeRequestScope ?: return
        val hostActivity = activity ?: return
        val imageKey = imageInfo.url.trim()
        if (imageKey.isBlank() || favoriteDownloadJobs.containsKey(imageKey)) {
            return
        }
        val detailUrl = imageInfo.href.trim()
        if (detailUrl.isBlank()) {
            showToast("缺少详情地址")
            return
        }
        if (!forceOverwrite && SourceImageDownloadHelper.isDetailHrefDownloaded(parentDir = getDownloadParentDir(), detailHref = detailUrl)) {
            pendingOverwriteDownload = PreparedFavoriteItemDownload(imageInfo = imageInfo)
            return
        }
        if (SourceImageDownloadHelper.requestExternalStoragePermissionIfNeeded(activity = hostActivity)) {
            showToast("需要存储权限后再下载")
            return
        }
        FileUtil.createExternalDir()
        favoriteDownloadProgress[imageKey] = "获取中"
        val job = scope.launch {
            try {
                val detailInfo = preparedDownload?.detailInfo ?: GenericSourceRepository.getDetail(
                    config = config,
                    dataUseFrom = dataInfo?.dataUseFrom,
                    url = detailUrl,
                )
                val completeDetailInfo = GenericSourceRepository.requestRemainingDetailPages(
                    config = config,
                    dataUseFrom = dataInfo?.dataUseFrom,
                    detailInfo = detailInfo,
                )
                favoriteDownloadProgress[imageKey] = "0/${completeDetailInfo.pictures.size}"
                val savedCount = withContext(Dispatchers.IO) {
                    SourceImageDownloadHelper.downloadDetailImages(
                        context = hostActivity.applicationContext,
                        detailInfo = completeDetailInfo,
                        parentDir = SourceImageDownloadHelper.getDetailDownloadDir(
                            parentDir = getDownloadParentDir(),
                            detailInfo = completeDetailInfo,
                        ),
                        imageModelProvider = { it.url.toGenericImageModel() },
                        logTag = TAG,
                        replaceExisting = forceOverwrite,
                        onProgress = { progress ->
                            favoriteDownloadProgress[imageKey] = "$progress/${completeDetailInfo.pictures.size}"
                        },
                    )
                }
                refreshDownloadedFavoriteState()
                showToast("已下载 $savedCount/${completeDetailInfo.pictures.size} 张图片")
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                LogComponent.printE(tag = TAG, message = "download favorite failed:$throwable")
                showToast("下载失败")
            } finally {
                favoriteDownloadProgress.remove(imageKey)
                favoriteDownloadJobs.remove(imageKey)
            }
        }
        favoriteDownloadJobs[imageKey] = job
    }

    private fun showFavoriteBulkDownloadDialog() {
        if (!showFavoritesOnly || isFavoriteBulkDownloading) {
            return
        }
        val uniqueFavorites = favoriteImages.distinctBy { it.url.trim() }
        val pendingItems = uniqueFavorites.filterNot {
            SourceImageDownloadHelper.isDetailHrefDownloaded(parentDir = getDownloadParentDir(), detailHref = it.href)
        }
        favoriteBulkDownloadPlan = FavoriteBulkDownloadPlan(
            totalCount = uniqueFavorites.size,
            alreadyDownloadedCount = uniqueFavorites.size - pendingItems.size,
            pendingItems = pendingItems,
        )
        favoriteBulkDownloadState = FavoriteBulkDownloadDialogState(
            totalCount = favoriteBulkDownloadPlan.totalCount,
            alreadyDownloadedCount = favoriteBulkDownloadPlan.alreadyDownloadedCount,
            pendingCount = favoriteBulkDownloadPlan.pendingCount,
        )
        showBulkDownloadDialog = true
    }

    private fun startFavoriteBulkDownload() {
        val config = sourceConfig ?: return
        val scope = composeRequestScope ?: return
        val hostActivity = activity ?: return
        val plan = favoriteBulkDownloadPlan
        if (isFavoriteBulkDownloading || plan.pendingItems.isEmpty()) {
            return
        }
        if (SourceImageDownloadHelper.requestExternalStoragePermissionIfNeeded(activity = hostActivity)) {
            showToast("需要存储权限后再下载")
            return
        }
        FileUtil.createExternalDir()
        favoriteBulkDownloadState = favoriteBulkDownloadState.copy(isDownloading = true, finishedCount = 0, failedCount = 0)
        favoriteBulkDownloadJob = scope.launch {
            var failedCount = 0
            try {
                plan.pendingItems.forEachIndexed { index, imageInfo ->
                    val itemIndex = index + 1
                    favoriteDownloadProgress[imageInfo.url] = "获取中"
                    favoriteBulkDownloadState = favoriteBulkDownloadState.copy(
                        currentTitle = "$itemIndex/${plan.pendingCount} ${imageInfo.bulkTitle()}",
                        currentImageProgress = "获取详情",
                    )
                    val success = runCatching {
                        val detailInfo = GenericSourceRepository.getDetail(
                            config = config,
                            dataUseFrom = dataInfo?.dataUseFrom,
                            url = imageInfo.href,
                        )
                        val completeDetailInfo = GenericSourceRepository.requestRemainingDetailPages(
                            config = config,
                            dataUseFrom = dataInfo?.dataUseFrom,
                            detailInfo = detailInfo,
                        )
                        withContext(Dispatchers.IO) {
                            SourceImageDownloadHelper.downloadDetailImages(
                                context = hostActivity.applicationContext,
                                detailInfo = completeDetailInfo,
                                parentDir = SourceImageDownloadHelper.getDetailDownloadDir(
                                    parentDir = getDownloadParentDir(),
                                    detailInfo = completeDetailInfo,
                                ),
                                imageModelProvider = { it.url.toGenericImageModel() },
                                logTag = TAG,
                                onProgress = { progress ->
                                    favoriteDownloadProgress[imageInfo.url] = "$progress/${completeDetailInfo.pictures.size}"
                                    favoriteBulkDownloadState = favoriteBulkDownloadState.copy(
                                        currentImageProgress = "图片 $progress/${completeDetailInfo.pictures.size}",
                                    )
                                },
                            )
                        } > 0
                    }.getOrElse { throwable ->
                        LogComponent.printE(tag = TAG, message = "bulk download failed:$throwable")
                        false
                    }
                    if (!success) {
                        failedCount += 1
                    }
                    favoriteDownloadProgress.remove(imageInfo.url)
                    favoriteBulkDownloadState = favoriteBulkDownloadState.copy(
                        finishedCount = itemIndex,
                        failedCount = failedCount,
                    )
                    refreshDownloadedFavoriteState()
                }
                favoriteBulkDownloadState = favoriteBulkDownloadState.copy(isDownloading = false)
                if (failedCount == 0) {
                    showBulkDownloadDialog = false
                    showToast("已下载 ${plan.pendingCount}/${plan.pendingCount} 个收藏")
                } else {
                    showToast("下载完成，失败 $failedCount 个")
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                favoriteDownloadProgress.clear()
                favoriteBulkDownloadJob = null
                favoriteBulkDownloadState = favoriteBulkDownloadState.copy(isDownloading = false)
            }
        }
    }

    private fun getDownloadParentDir(): File {
        return File("${FileUtil.getDownloadPath()}/${dataInfo?.sourceKey.orEmpty()}/detail")
    }

    private fun handleBack() {
        if (isFavoriteBulkDownloading) {
            showToast("正在下载中")
            return
        }
        if (showBulkDownloadDialog) {
            showBulkDownloadDialog = false
            return
        }
        if (pendingOverwriteDownload != null) {
            pendingOverwriteDownload = null
            return
        }
        if (showFavoritesOnly) {
            if (openedFavoritesOnly) {
                requireActivity().finish()
            } else {
                showFavoritesOnly = false
            }
            return
        }
        requireActivity().finish()
    }

    private fun readThemeInfo(): MainThemeSelectInfo? {
        return arguments?.let { args ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                args.getSerializable(KEY_DATA, MainThemeSelectInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                args.getSerializable(KEY_DATA) as? MainThemeSelectInfo
            }
        }
    }

    private fun openUrl(url: String) {
        if (url.isBlank()) {
            return
        }
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { throwable ->
            LogComponent.printE(tag = TAG, message = throwable.toString())
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun nextRequestId(): Int {
        requestVersion += 1
        return requestVersion
    }

    private fun isActiveRequest(requestId: Int, scope: CoroutineScope): Boolean {
        return requestId == requestVersion && composeRequestScope == scope
    }

    private fun ImageLoadsInfo.genericDisplayRatio(): Float {
        return if (width > 0 && height > 0) {
            width.toFloat() / height.toFloat()
        } else {
            2f / 3f
        }
    }

    private fun ImageLoadsInfo.bulkTitle(): String {
        return title.trim().ifBlank { href.trim() }.ifBlank { url.trim() }
    }
}

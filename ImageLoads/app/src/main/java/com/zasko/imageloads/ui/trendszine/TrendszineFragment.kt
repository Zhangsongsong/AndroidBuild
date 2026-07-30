package com.zasko.imageloads.ui.trendszine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.bumptech.glide.Glide
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.compose.ImageListScreen
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.fragment.ComposeBaseFragment
import com.zasko.imageloads.ui.common.DownloadOverwriteDialog
import com.zasko.imageloads.ui.common.FavoriteBulkDownloadDialog
import com.zasko.imageloads.ui.common.FavoriteBulkDownloadDialogState
import com.zasko.imageloads.ui.common.FavoriteBulkDownloadPlan
import com.zasko.imageloads.ui.common.PreparedFavoriteItemDownload
import com.zasko.imageloads.ui.common.SourceImageDetailActivity
import com.zasko.imageloads.ui.common.SourceImageDownloadHelper
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.PermissionUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.copyTo

class TrendszineFragment : ComposeBaseFragment() {

    companion object {
        private const val TAG = "TrendszineFragment"
        private const val HOME_URL = "https://trendszine.com/"
        private const val KEY_SHOW_FAVORITES = "key_show_favorites"

        fun newInstance(data: MainThemeSelectInfo, showFavoritesOnly: Boolean = false): TrendszineFragment {
            return TrendszineFragment().apply {
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
    private val categories = mutableStateListOf(TrendszineRepository.allCategory)
    private val selectedImageUrls = mutableStateListOf<String>()
    private val pageLabels = mutableStateMapOf<Int, Int>()
    private val favoriteDownloadProgress = mutableStateMapOf<String, String>()
    private val favoriteDownloadJobs = mutableMapOf<String, Job>()
    private val downloadedFavoriteImageUrls = mutableStateListOf<String>()

    private var dataInfo: MainThemeSelectInfo? = null
    private var selectedParentCategory by mutableStateOf(TrendszineRepository.allCategory)
    private var selectedCategory by mutableStateOf(TrendszineRepository.allCategory)
    private var nextPage = 1
    private var isRefreshing by mutableStateOf(false)
    private var isLoadingMoreState by mutableStateOf(false)
    private var isSelectionMode by mutableStateOf(false)
    private var isDownloading by mutableStateOf(false)
    private var showFavoritesOnly by mutableStateOf(false)
    private var openedFavoritesOnly = false
    private var activeRequest: Job? = null
    private var downloadJob: Job? = null
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
        val displayImages = if (showFavoritesOnly) {
            favoriteImages
        } else {
            images
        }
        ImageLoadsTheme {
            ImageListScreen(
                title = screenTitle(),
                images = displayImages,
                isRefreshing = !showFavoritesOnly && isRefreshing,
                isLoadingMore = !showFavoritesOnly && isLoadingMoreState,
                onBack = ::handleBack,
                onOpenWeb = { openUrl(if (showFavoritesOnly) HOME_URL else selectedCategory.url) },
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
                titleContent = if (showFavoritesOnly) {
                    null
                } else {
                    {
                        TrendszineTitleCategorySelector(
                            categories = categories,
                            selectedParentCategory = selectedParentCategory,
                            selectedCategory = selectedCategory,
                            onParentCategorySelected = ::selectParentCategory,
                            onChildCategorySelected = ::selectChildCategory,
                        )
                    }
                },
                imageModelProvider = { it.url.toTrendszineImageModel() },
                imageRatioProvider = { it.trendszineDisplayRatio() },
                imageScaleType = ImageView.ScaleType.CENTER_CROP,
                showWebAction = !showFavoritesOnly,
                showActionMenu = !showFavoritesOnly,
                showDownloadMenuAction = !showFavoritesOnly,
                showDownloadAllAction = showFavoritesOnly,
                isDownloadAllActionEnabled = !isFavoriteBulkDownloading && favoriteImages.isNotEmpty(),
                showPageJumpMenuAction = true,
                pageJumpInitialPage = currentFirstPage(),
                showFavoriteMenuAction = !showFavoritesOnly,
                favoriteMenuText = "收藏",
                isSelectionMode = isSelectionMode,
                selectedImageKeys = selectedImageUrls.toSet(),
                isDownloadActionEnabled = !isDownloading,
                selectionDownloadText = "下载",
                showFavoriteAction = true,
                favoriteImageKeys = favoriteImages.map { it.url }.toSet(),
                showItemDownloadAction = showFavoritesOnly && !isFavoriteBulkDownloading,
                downloadingImageKeys = favoriteDownloadProgress.keys.toSet(),
                downloadedImageKeys = downloadedFavoriteImageUrls.toSet(),
                imageKeyProvider = { it.url },
                itemDownloadProgressProvider = { favoriteDownloadProgress[it.url] },
                onImageDownloadModeClick = ::enterSelectionMode,
                onDownloadAllClick = ::showFavoriteBulkDownloadDialog,
                onPageJump = ::jumpToPage,
                onFavoriteMenuClick = ::toggleFavoriteList,
                onFavoriteClick = ::toggleFavorite,
                onItemDownloadClick = ::downloadFavoriteItem,
                onCancelSelection = ::cancelSelectionMode,
                onDownloadSelected = ::downloadSelectedCovers,
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
        clearViewRequests()
    }

    private fun screenTitle(): String {
        return when {
            showFavoritesOnly -> "Trendszine 收藏"
            selectedCategory == TrendszineRepository.allCategory -> dataInfo?.title.orEmpty()
            else -> selectedCategory.title
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
        if (!canLoadMore()) {
            return
        }
        requestImages(mode = LoadMode.More)
    }

    private fun canLoadMore(): Boolean {
        return !isRefreshing && !isLoadingMore.get() && !isLoadEnd.get()
    }

    private fun selectParentCategory(category: TrendszineCategory) {
        if ((selectedParentCategory == category && selectedCategory == category) || isRefreshing || isLoadingMore.get()) {
            return
        }
        selectedParentCategory = category
        selectCategory(category = category)
    }

    private fun selectChildCategory(category: TrendszineCategory) {
        val parentCategory = findParentCategory(category = category)
        if ((selectedParentCategory == parentCategory && selectedCategory == category) || isRefreshing || isLoadingMore.get()) {
            return
        }
        selectedParentCategory = parentCategory
        selectCategory(category = category)
    }

    private fun findParentCategory(category: TrendszineCategory): TrendszineCategory {
        return categories.firstOrNull {
            it.url == category.url || it.children.any { childCategory -> childCategory.url == category.url }
        } ?: category
    }

    private fun selectCategory(category: TrendszineCategory) {
        selectedCategory = category
        images.clear()
        pageLabels.clear()
        nextPage = 1
        isLoadEnd.set(false)
        requestImages(mode = LoadMode.Refresh)
    }

    private fun requestImages(mode: LoadMode, targetPage: Int? = null) {
        val scope = composeRequestScope ?: return
        val page = when (mode) {
            LoadMode.Refresh -> targetPage?.coerceAtLeast(1) ?: 1
            LoadMode.More -> nextPage
        }
        val requestId = nextRequestId()

        activeRequest?.cancel()
        beginLoading(mode = mode, page = page)
        activeRequest = scope.launch {
            try {
                val result = TrendszineRepository.getImages(
                    dataUseFrom = dataInfo?.dataUseFrom,
                    category = selectedCategory,
                    page = page,
                )
                if (isActiveRequest(requestId = requestId, scope = scope)) {
                    applyLoadedImages(mode = mode, page = page, result = result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                if (isActiveRequest(requestId = requestId, scope = scope)) {
                    LogComponent.printE(tag = TAG, message = throwable.toString())
                }
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

    private fun applyLoadedImages(mode: LoadMode, page: Int, result: TrendszinePageResult) {
        if (result.categories.isNotEmpty()) {
            val selectedParentUrl = selectedParentCategory.url
            val selectedCategoryUrl = selectedCategory.url
            categories.clear()
            categories.addAll(result.categories)
            selectedParentCategory = categories.firstOrNull { it.url == selectedParentUrl }
                ?: TrendszineRepository.allCategory
            selectedCategory = if (selectedParentCategory.url == selectedCategoryUrl) {
                selectedParentCategory
            } else {
                selectedParentCategory.children.firstOrNull { it.url == selectedCategoryUrl }
                    ?: selectedParentCategory
            }
        }
        when (mode) {
            LoadMode.Refresh -> {
                pageLabels.clear()
                if (result.images.isNotEmpty()) {
                    pageLabels[0] = page
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
        if (showFavoritesOnly || isRefreshing || isLoadingMore.get() || isDownloading) {
            return
        }
        selectedImageUrls.clear()
        isSelectionMode = false
        images.clear()
        pageLabels.clear()
        requestImages(mode = LoadMode.Refresh, targetPage = page)
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

    private fun clearViewRequests() {
        requestVersion += 1
        activeRequest?.cancel()
        activeRequest = null
        downloadJob?.cancel()
        downloadJob = null
        favoriteBulkDownloadJob?.cancel()
        favoriteBulkDownloadJob = null
        favoriteDownloadJobs.values.forEach { it.cancel() }
        favoriteDownloadJobs.clear()
        favoriteDownloadProgress.clear()
        downloadedFavoriteImageUrls.clear()
        pendingOverwriteDownload = null
        showBulkDownloadDialog = false
        favoriteBulkDownloadPlan = FavoriteBulkDownloadPlan()
        favoriteBulkDownloadState = FavoriteBulkDownloadDialogState()
        isRefreshing = false
        isLoadingMore.set(false)
        isLoadingMoreState = false
        isDownloading = false
    }

    private fun handleImageClick(imageInfo: ImageLoadsInfo) {
        if (isSelectionMode) {
            toggleCoverSelection(url = imageInfo.url)
        } else {
            openDetail(imageInfo = imageInfo)
        }
    }

    private fun openDetail(imageInfo: ImageLoadsInfo) {
        if (imageInfo.href.isBlank()) {
            showToast("缺少详情地址")
            return
        }
        SourceImageDetailActivity.start(
            context = requireContext(),
            sourceType = Constants.THEME_TYPE_TRENDSZINE,
            info = imageInfo,
            dataUseFrom = dataInfo?.dataUseFrom,
        )
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
                activity?.finish()
            } else {
                exitFavoriteList()
            }
        } else {
            activity?.finish()
        }
    }

    private fun toggleFavoriteList() {
        if (showFavoritesOnly) {
            exitFavoriteList()
        } else {
            enterFavoriteList()
        }
    }

    private fun enterFavoriteList() {
        refreshFavorites()
        selectedImageUrls.clear()
        isSelectionMode = false
        showFavoritesOnly = true
    }

    private fun exitFavoriteList() {
        selectedImageUrls.clear()
        isSelectionMode = false
        showFavoritesOnly = false
    }

    private fun toggleFavorite(imageInfo: ImageLoadsInfo) {
        val isFavorite = TrendszineFavoriteStore.toggleFavorite(imageInfo)
        refreshFavorites()
        showToast(if (isFavorite) "已收藏" else "已取消收藏")
    }

    private fun refreshFavorites() {
        favoriteImages.clear()
        favoriteImages.addAll(TrendszineFavoriteStore.getFavorites())
        refreshDownloadedFavoriteState()
    }

    private fun refreshDownloadedFavoriteState() {
        downloadedFavoriteImageUrls.clear()
        downloadedFavoriteImageUrls.addAll(
            favoriteImages
                .filter {
                    SourceImageDownloadHelper.isDetailHrefDownloaded(
                        sourceType = Constants.THEME_TYPE_TRENDSZINE,
                        detailHref = it.href,
                    )
                }
                .map { it.url },
        )
    }

    private fun downloadFavoriteItem(imageInfo: ImageLoadsInfo) {
        if (isFavoriteBulkDownloading) {
            showToast("正在下载中")
            return
        }
        if (SourceImageDownloadHelper.isDetailHrefDownloaded(
                sourceType = Constants.THEME_TYPE_TRENDSZINE,
                detailHref = imageInfo.href,
            )
        ) {
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
        if (isFavoriteBulkDownloading) {
            return
        }
        SourceImageDownloadHelper.startFavoriteItemDownload(
            activity = activity,
            scope = composeRequestScope,
            sourceType = Constants.THEME_TYPE_TRENDSZINE,
            dataUseFrom = dataInfo?.dataUseFrom,
            imageInfo = imageInfo,
            preparedDetailInfo = preparedDownload?.detailInfo,
            forceOverwrite = forceOverwrite,
            progressMap = favoriteDownloadProgress,
            activeJobs = favoriteDownloadJobs,
            showToast = ::showToast,
            onAlreadyDownloaded = { pendingOverwriteDownload = it },
            onDownloadFinished = { refreshDownloadedFavoriteState() },
        )
    }

    private fun showFavoriteBulkDownloadDialog() {
        if (!showFavoritesOnly || isFavoriteBulkDownloading) {
            return
        }
        favoriteBulkDownloadPlan = SourceImageDownloadHelper.createFavoriteBulkDownloadPlan(
            sourceType = Constants.THEME_TYPE_TRENDSZINE,
            favorites = favoriteImages,
        )
        favoriteBulkDownloadState = FavoriteBulkDownloadDialogState(
            totalCount = favoriteBulkDownloadPlan.totalCount,
            alreadyDownloadedCount = favoriteBulkDownloadPlan.alreadyDownloadedCount,
            pendingCount = favoriteBulkDownloadPlan.pendingCount,
        )
        showBulkDownloadDialog = true
    }

    private fun startFavoriteBulkDownload() {
        if (isFavoriteBulkDownloading || favoriteBulkDownloadPlan.pendingItems.isEmpty()) {
            return
        }
        val hostActivity = activity ?: return
        val scope = composeRequestScope ?: return
        if (SourceImageDownloadHelper.requestExternalStoragePermissionIfNeeded(activity = hostActivity)) {
            showToast("需要存储权限后再下载")
            return
        }
        FileUtil.createExternalDir()

        val context = hostActivity.applicationContext
        val plan = favoriteBulkDownloadPlan
        pendingOverwriteDownload = null
        favoriteDownloadProgress.clear()
        favoriteBulkDownloadState = favoriteBulkDownloadState.copy(
            finishedCount = 0,
            failedCount = 0,
            currentTitle = "",
            currentImageProgress = "",
            isDownloading = true,
        )
        favoriteBulkDownloadJob = scope.launch {
            try {
                val result = SourceImageDownloadHelper.downloadFavoriteItemsSequentially(
                    context = context,
                    sourceType = Constants.THEME_TYPE_TRENDSZINE,
                    dataUseFrom = dataInfo?.dataUseFrom,
                    imageInfos = plan.pendingItems,
                    onItemStarted = { itemIndex, imageInfo ->
                        favoriteDownloadProgress[imageInfo.url] = "获取中"
                        favoriteBulkDownloadState = favoriteBulkDownloadState.copy(
                            currentTitle = "${itemIndex}/${plan.pendingCount} ${imageInfo.bulkTitle()}",
                            currentImageProgress = "获取详情",
                        )
                    },
                    onImageProgress = { itemIndex, progress, total ->
                        val imageInfo = plan.pendingItems.getOrNull(itemIndex - 1)
                        val progressText = "$progress/$total"
                        if (imageInfo != null) {
                            favoriteDownloadProgress[imageInfo.url] = progressText
                        }
                        favoriteBulkDownloadState = favoriteBulkDownloadState.copy(
                            currentImageProgress = "图片 $progressText",
                        )
                    },
                    onItemFinished = { itemIndex, imageInfo, success ->
                        favoriteDownloadProgress.remove(imageInfo.url)
                        favoriteBulkDownloadState = favoriteBulkDownloadState.copy(
                            finishedCount = itemIndex,
                            failedCount = favoriteBulkDownloadState.failedCount + if (success) 0 else 1,
                            currentImageProgress = if (success) "当前完成" else "当前失败",
                        )
                        refreshDownloadedFavoriteState()
                    },
                )
                refreshDownloadedFavoriteState()
                favoriteBulkDownloadState = favoriteBulkDownloadState.copy(
                    isDownloading = false,
                    currentImageProgress = "",
                )
                if (result.failedItemCount == 0) {
                    showBulkDownloadDialog = false
                    showToast("已下载 ${result.successItemCount}/${plan.pendingCount} 个收藏")
                } else {
                    showToast("下载完成，失败 ${result.failedItemCount} 个")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                LogComponent.printE(tag = TAG, message = "favorite bulk download failed:$throwable")
                favoriteBulkDownloadState = favoriteBulkDownloadState.copy(isDownloading = false)
                showToast("下载失败")
            } finally {
                favoriteDownloadProgress.clear()
                favoriteBulkDownloadJob = null
            }
        }
    }

    private fun enterSelectionMode() {
        if (isDownloading) {
            return
        }
        selectedImageUrls.clear()
        isSelectionMode = true
    }

    private fun cancelSelectionMode() {
        if (isDownloading) {
            return
        }
        selectedImageUrls.clear()
        isSelectionMode = false
    }

    private fun toggleCoverSelection(url: String) {
        if (url.isBlank() || isDownloading) {
            return
        }
        if (selectedImageUrls.contains(url)) {
            selectedImageUrls.remove(url)
        } else {
            selectedImageUrls.add(url)
        }
    }

    private fun downloadSelectedCovers() {
        if (isDownloading) {
            return
        }
        val selectedUrls = selectedImageUrls
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (selectedUrls.isEmpty()) {
            return
        }
        val hostActivity = activity ?: return
        if (requestExternalStoragePermissionIfNeeded()) {
            showToast("需要存储权限后再下载")
            return
        }
        FileUtil.createExternalDir()

        val context = hostActivity.applicationContext
        val scope = composeRequestScope ?: return
        isDownloading = true
        downloadJob = scope.launch {
            try {
                val savedCount = withContext(Dispatchers.IO) {
                    downloadCoverImages(
                        context = context,
                        urls = selectedUrls,
                        parentDir = getCoverDownloadDir(),
                    )
                }
                LogComponent.printD(tag = TAG, message = "downloadSelectedCovers count:$savedCount")
                showToast("已下载 $savedCount/${selectedUrls.size} 张封面")
                isSelectionMode = false
                selectedImageUrls.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                LogComponent.printE(tag = TAG, message = "downloadSelectedCovers failed:$throwable")
                showToast("下载失败")
            } finally {
                isDownloading = false
                downloadJob = null
            }
        }
    }

    private fun requestExternalStoragePermissionIfNeeded(): Boolean {
        val hostActivity = activity ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                PermissionUtil.getReadAndWriteExternal(hostActivity)
                true
            } else {
                false
            }
        } else if (!PermissionUtil.checkWriteExternal(hostActivity)) {
            PermissionUtil.getReadAndWriteExternal(hostActivity)
            true
        } else {
            false
        }
    }

    private fun downloadCoverImages(
        context: Context,
        urls: List<String>,
        parentDir: File,
    ): Int {
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        var savedCount = 0
        urls.forEachIndexed { index, url ->
            val destFile = File(parentDir, url.toCoverFileName(index = index))
            runCatching {
                downloadOneCover(
                    context = context,
                    url = url,
                    destFile = destFile,
                )
            }.onSuccess {
                savedCount += 1
                LogComponent.printD(tag = TAG, message = "download cover success:${destFile.absolutePath}")
            }.onFailure { throwable ->
                LogComponent.printE(tag = TAG, message = "download cover failed:$url $throwable")
            }
        }
        return savedCount
    }

    private fun downloadOneCover(
        context: Context,
        url: String,
        destFile: File,
    ) {
        val futureTarget = Glide.with(context)
            .asFile()
            .load(url.toTrendszineImageModel())
            .submit()
        try {
            futureTarget.get().copyTo(destFile, overwrite = true)
        } finally {
            Glide.with(context).clear(futureTarget)
        }
    }

    private fun getCoverDownloadDir(): File {
        return File(
            "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_TRENDSZINE}/${FileUtil.PICTURE_TRENDSZINE_COVERS}",
        )
    }

    private fun String.toCoverFileName(index: Int): String {
        val fallbackName = "cover_${index.toString().padStart(4, '0')}.jpg"
        val rawName = runCatching { Uri.parse(this).lastPathSegment }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: fallbackName
        return rawName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun nextRequestId(): Int {
        requestVersion += 1
        return requestVersion
    }

    private fun isActiveRequest(requestId: Int, scope: CoroutineScope): Boolean {
        return requestId == requestVersion && composeRequestScope == scope
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
        if (!isAdded) {
            return
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun ImageLoadsInfo.trendszineDisplayRatio(): Float {
        return if (width > 0 && height > 0) {
            width.toFloat() / height.toFloat()
        } else {
            2f / 3f
        }
    }

    private fun ImageLoadsInfo.bulkTitle(): String {
        return title.trim()
            .ifBlank { href.trim() }
            .ifBlank { url.trim() }
    }
}

@Preview(name = "Trendszine", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun TrendszineFragmentPreview() {
    ImageLoadsTheme {
        ImageListScreen(
            title = "Trendszine",
            images = listOf(
                ImageLoadsInfo(
                    url = "preview://trendszine-1",
                    width = 400,
                    height = 600,
                ),
                ImageLoadsInfo(
                    url = "preview://trendszine-2",
                    width = 400,
                    height = 600,
                ),
            ),
            isRefreshing = false,
            isLoadingMore = true,
            onBack = {},
            onOpenWeb = {},
            onLoadMore = {},
            onImageClick = {},
            titleContent = {
                TrendszineTitleCategorySelector(
                    categories = listOf(
                        TrendszineRepository.allCategory,
                        TrendszineCategory(
                            title = "Cosplay",
                            url = "https://trendszine.com/category/cosplay",
                            children = listOf(
                                TrendszineCategory(
                                    title = "精选 Coser",
                                    url = "https://trendszine.com/category/cosplay/best-coser",
                                ),
                            ),
                        ),
                    ),
                    selectedParentCategory = TrendszineRepository.allCategory,
                    selectedCategory = TrendszineRepository.allCategory,
                    onParentCategorySelected = {},
                    onChildCategorySelected = {},
                )
            },
        )
    }
}

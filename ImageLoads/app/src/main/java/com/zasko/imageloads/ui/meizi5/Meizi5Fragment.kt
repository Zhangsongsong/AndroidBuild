package com.zasko.imageloads.ui.meizi5

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageListScreen
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.fragment.ComposeBaseFragment
import com.zasko.imageloads.ui.common.DownloadOverwriteDialog
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

class Meizi5Fragment : ComposeBaseFragment() {

    companion object {
        private const val TAG = "Meizi5Fragment"
        private const val HOME_URL = "https://meizi5.com/"
        private const val KEY_SHOW_FAVORITES = "key_show_favorites"

        fun newInstance(data: MainThemeSelectInfo, showFavoritesOnly: Boolean = false): Meizi5Fragment {
            return Meizi5Fragment().apply {
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
    private val selectedImageUrls = mutableStateListOf<String>()
    private val pageLabels = mutableStateMapOf<Int, Int>()
    private val favoriteDownloadProgress = mutableStateMapOf<String, String>()
    private val favoriteDownloadJobs = mutableMapOf<String, Job>()
    private val downloadedFavoriteImageUrls = mutableStateListOf<String>()

    private var dataInfo: MainThemeSelectInfo? = null
    private var nextPage = 1
    private var isRefreshing by mutableStateOf(false)
    private var isLoadingMoreState by mutableStateOf(false)
    private var isSelectionMode by mutableStateOf(false)
    private var isDownloading by mutableStateOf(false)
    private var showFavoritesOnly by mutableStateOf(false)
    private var openedFavoritesOnly = false
    private var activeRequest: Job? = null
    private var downloadJob: Job? = null
    private var pendingOverwriteDownload by mutableStateOf<PreparedFavoriteItemDownload?>(null)
    private var requestVersion = 0

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
                title = if (showFavoritesOnly) "Meizi5 收藏" else dataInfo?.title.orEmpty(),
                images = displayImages,
                isRefreshing = !showFavoritesOnly && isRefreshing,
                isLoadingMore = !showFavoritesOnly && isLoadingMoreState,
                onBack = ::handleBack,
                onOpenWeb = { openUrl(HOME_URL) },
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
                showWebAction = false,
                imageModelProvider = { it.url.toMeizi5ImageModel() },
                imageRatioProvider = { it.meizi5DisplayRatio() },
                showActionMenu = !showFavoritesOnly,
                showFavoriteMenuAction = true,
                showPageJumpMenuAction = true,
                pageJumpInitialPage = currentFirstPage(),
                favoriteMenuText = "收藏",
                isSelectionMode = isSelectionMode,
                selectedImageKeys = selectedImageUrls.toSet(),
                isDownloadActionEnabled = !isDownloading,
                selectionDownloadText = "下载",
                showFavoriteAction = true,
                favoriteImageKeys = favoriteImages.map { it.url }.toSet(),
                showItemDownloadAction = showFavoritesOnly,
                downloadingImageKeys = favoriteDownloadProgress.keys.toSet(),
                downloadedImageKeys = downloadedFavoriteImageUrls.toSet(),
                imageKeyProvider = { it.url },
                itemDownloadProgressProvider = { favoriteDownloadProgress[it.url] },
                onImageDownloadModeClick = ::enterSelectionMode,
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

    private fun loadNewData() {
        if (isLoadingMore.get()) {
            isRefreshing = false
            return
        }

        requestImages(LoadMode.Refresh)
    }

    private fun loadMoreData() {
        if (!canLoadMore()) {
            return
        }

        requestImages(LoadMode.More)
    }

    private fun canLoadMore(): Boolean {
        return !isRefreshing && !isLoadingMore.get() && !isLoadEnd.get()
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
                val list = Meizi5Repository.getImages(
                    dataUseFrom = dataInfo?.dataUseFrom,
                    page = page,
                )
                if (isActiveRequest(requestId = requestId, scope = scope)) {
                    applyLoadedImages(mode = mode, page = page, list = list)
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

    private fun applyLoadedImages(
        mode: LoadMode,
        page: Int,
        list: List<ImageLoadsInfo>,
    ) {
        when (mode) {
            LoadMode.Refresh -> {
                nextPage = page + 1
                pageLabels.clear()
                if (list.isNotEmpty()) {
                    pageLabels[0] = page
                }
                images.clear()
                images.addAll(list)
                isLoadEnd.set(list.isEmpty())
            }

            LoadMode.More -> {
                if (list.isEmpty()) {
                    isLoadEnd.set(true)
                } else {
                    pageLabels[images.size] = page
                    nextPage = page + 1
                    images.addAll(list)
                }
            }
        }
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
        favoriteDownloadJobs.values.forEach { it.cancel() }
        favoriteDownloadJobs.clear()
        favoriteDownloadProgress.clear()
        downloadedFavoriteImageUrls.clear()
        pendingOverwriteDownload = null
        isRefreshing = false
        isLoadingMore.set(false)
        isLoadingMoreState = false
        isDownloading = false
    }

    private fun handleImageClick(imageInfo: ImageLoadsInfo) {
        if (isSelectionMode) {
            toggleCoverSelection(url = imageInfo.url)
        } else {
            if (imageInfo.href.isBlank()) {
                showToast("缺少详情地址")
            } else {
                SourceImageDetailActivity.start(
                    context = requireContext(),
                    sourceType = Constants.THEME_TYPE_MEIZI5,
                    info = imageInfo,
                    dataUseFrom = dataInfo?.dataUseFrom,
                )
            }
        }
    }

    private fun handleBack() {
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
        val isFavorite = Meizi5FavoriteStore.toggleFavorite(imageInfo)
        refreshFavorites()
        showToast(if (isFavorite) "已收藏" else "已取消收藏")
    }

    private fun refreshFavorites() {
        favoriteImages.clear()
        favoriteImages.addAll(Meizi5FavoriteStore.getFavorites())
        refreshDownloadedFavoriteState()
    }

    private fun refreshDownloadedFavoriteState() {
        downloadedFavoriteImageUrls.clear()
        downloadedFavoriteImageUrls.addAll(
            favoriteImages
                .filter {
                    SourceImageDownloadHelper.isDetailHrefDownloaded(
                        sourceType = Constants.THEME_TYPE_MEIZI5,
                        detailHref = it.href,
                    )
                }
                .map { it.url },
        )
    }

    private fun downloadFavoriteItem(imageInfo: ImageLoadsInfo) {
        if (SourceImageDownloadHelper.isDetailHrefDownloaded(
                sourceType = Constants.THEME_TYPE_MEIZI5,
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
        SourceImageDownloadHelper.startFavoriteItemDownload(
            activity = activity,
            scope = composeRequestScope,
            sourceType = Constants.THEME_TYPE_MEIZI5,
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
        isSelectionMode = false
        selectedImageUrls.clear()
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
            val savedCount = withContext(Dispatchers.IO) {
                downloadCoverImages(
                    context = context,
                    urls = selectedUrls,
                    parentDir = getCoverDownloadDir(),
                )
            }
            LogComponent.printD(tag = TAG, message = "downloadSelectedCovers count:$savedCount")
            showToast("已下载 $savedCount/${selectedUrls.size} 张封面")
            isDownloading = false
            isSelectionMode = false
            selectedImageUrls.clear()
            downloadJob = null
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
            val fileName = url.toCoverFileName(index = index)
            val destFile = File(parentDir, fileName)
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
            .load(url.toMeizi5ImageModel())
            .submit()
        try {
            val cacheFile = futureTarget.get()
            cacheFile.copyTo(destFile, overwrite = true)
        } finally {
            Glide.with(context).clear(futureTarget)
        }
    }

    private fun getCoverDownloadDir(): File {
        return File(
            "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_MEIZI5}/${FileUtil.PICTURE_MEIZI5_COVERS}",
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

    private fun ImageLoadsInfo.meizi5DisplayRatio(): Float {
        return if (width > 0 && height > 0) {
            width.toFloat() / height.toFloat()
        } else {
            2f / 3f
        }
    }
}

@Preview(name = "Meizi5", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun Meizi5FragmentPreview() {
    ImageLoadsTheme {
        ImageListScreen(
            title = "Meizi5",
            images = listOf(
                ImageLoadsInfo(
                    url = "preview://meizi5-1",
                    width = 400,
                    height = 600,
                ),
                ImageLoadsInfo(
                    url = "preview://meizi5-2",
                    width = 600,
                    height = 900,
                ),
            ),
            isRefreshing = false,
            isLoadingMore = true,
            onBack = {},
            onOpenWeb = {},
            onLoadMore = {},
            onImageClick = {},
        )
    }
}

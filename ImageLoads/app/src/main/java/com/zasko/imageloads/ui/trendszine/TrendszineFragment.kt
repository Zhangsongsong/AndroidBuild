package com.zasko.imageloads.ui.trendszine

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.compose.ImageListScreen
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.fragment.ComposeBaseFragment
import com.zasko.imageloads.ui.common.SourceImageDetailActivity
import com.zasko.imageloads.utils.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TrendszineFragment : ComposeBaseFragment() {

    companion object {
        private const val TAG = "TrendszineFragment"
        private const val HOME_URL = "https://trendszine.com/"

        fun newInstance(data: MainThemeSelectInfo): TrendszineFragment {
            return TrendszineFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_DATA, data)
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

    private var dataInfo: MainThemeSelectInfo? = null
    private var selectedParentCategory by mutableStateOf(TrendszineRepository.allCategory)
    private var selectedCategory by mutableStateOf(TrendszineRepository.allCategory)
    private var nextPage = 1
    private var isRefreshing by mutableStateOf(false)
    private var isLoadingMoreState by mutableStateOf(false)
    private var showFavoritesOnly by mutableStateOf(false)
    private var activeRequest: Job? = null
    private var requestVersion = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataInfo = readThemeInfo()
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
                onBack = {
                    if (showFavoritesOnly) {
                        exitFavoriteList()
                    } else {
                        activity?.finish()
                    }
                },
                onOpenWeb = { openUrl(if (showFavoritesOnly) HOME_URL else selectedCategory.url) },
                onLoadMore = {
                    if (!showFavoritesOnly) {
                        loadMoreData()
                    }
                },
                onImageClick = ::openDetail,
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
                showActionMenu = true,
                showDownloadMenuAction = false,
                showFavoriteMenuAction = true,
                favoriteMenuText = if (showFavoritesOnly) "全部图片" else "收藏",
                showFavoriteAction = true,
                favoriteImageKeys = favoriteImages.map { it.url }.toSet(),
                imageKeyProvider = { it.url },
                onFavoriteMenuClick = ::toggleFavoriteList,
                onFavoriteClick = ::toggleFavorite,
            )
        }
    }

    override fun initByResume() {
        super.initByResume()
        loadNewData()
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
        nextPage = 1
        isLoadEnd.set(false)
        requestImages(mode = LoadMode.Refresh)
    }

    private fun requestImages(mode: LoadMode) {
        val scope = composeRequestScope ?: return
        val page = when (mode) {
            LoadMode.Refresh -> 1
            LoadMode.More -> nextPage
        }
        val requestId = nextRequestId()

        activeRequest?.cancel()
        beginLoading(mode = mode)
        activeRequest = scope.launch {
            try {
                val result = TrendszineRepository.getImages(
                    dataUseFrom = dataInfo?.dataUseFrom,
                    category = selectedCategory,
                    page = page,
                )
                if (isActiveRequest(requestId = requestId, scope = scope)) {
                    applyLoadedImages(mode = mode, result = result)
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

    private fun beginLoading(mode: LoadMode) {
        when (mode) {
            LoadMode.Refresh -> {
                nextPage = 1
                isLoadEnd.set(false)
                isRefreshing = true
            }

            LoadMode.More -> {
                isLoadingMore.set(true)
                isLoadingMoreState = true
            }
        }
    }

    private fun applyLoadedImages(mode: LoadMode, result: TrendszinePageResult) {
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
                images.clear()
                images.addAll(result.images)
            }

            LoadMode.More -> {
                images.addAll(result.images)
            }
        }
        nextPage = result.nextPage ?: nextPage
        isLoadEnd.set(result.images.isEmpty() || result.nextPage == null)
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
        isRefreshing = false
        isLoadingMore.set(false)
        isLoadingMoreState = false
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

    private fun toggleFavoriteList() {
        if (showFavoritesOnly) {
            exitFavoriteList()
        } else {
            enterFavoriteList()
        }
    }

    private fun enterFavoriteList() {
        refreshFavorites()
        showFavoritesOnly = true
    }

    private fun exitFavoriteList() {
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

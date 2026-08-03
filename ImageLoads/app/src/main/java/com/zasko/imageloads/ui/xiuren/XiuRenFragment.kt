package com.zasko.imageloads.ui.xiuren

import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.zasko.imageloads.activity.PersonDetailActivity
import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageListScreen
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.fragment.ComposeBaseFragment
import com.zasko.imageloads.ui.xiuren.activity.XiuRenWebViewActivity
import com.zasko.imageloads.utils.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class XiuRenFragment : ComposeBaseFragment() {

    companion object {
        private const val TAG = "XiuRenFragment"
        private const val LOAD_MAX_SIZE = 20
        private const val KEY_FAVORITE_MODE = "key_favorite_mode"

        fun newInstance(data: MainThemeSelectInfo, favoriteMode: Boolean = false): XiuRenFragment {
            return XiuRenFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_DATA, data)
                    putBoolean(KEY_FAVORITE_MODE, favoriteMode)
                }
            }
        }
    }

    private enum class LoadMode {
        Refresh,
        More,
    }

    private val images = mutableStateListOf<ImageLoadsInfo>()
    private val pageLabels = mutableStateMapOf<Int, Int>()

    private var dataInfo: MainThemeSelectInfo? = null
    private var loadStartIndex = 0
    private var isRefreshing by mutableStateOf(false)
    private var isLoadingMoreState by mutableStateOf(false)
    private var favoriteMode by mutableStateOf(false)
    private var activeRequest: Job? = null
    private var requestVersion = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataInfo = readThemeInfo()
        favoriteMode = arguments?.getBoolean(KEY_FAVORITE_MODE) == true
    }

    @Composable
    override fun FragmentContent() {
        ImageLoadsTheme {
            ImageListScreen(
                title = if (favoriteMode) "${dataInfo?.title.orEmpty()} 收藏" else dataInfo?.title.orEmpty(),
                images = images,
                isRefreshing = !favoriteMode && isRefreshing,
                isLoadingMore = !favoriteMode && isLoadingMoreState,
                onBack = { activity?.finish() },
                onOpenWeb = ::openWebView,
                onLoadMore = {
                    if (!favoriteMode) {
                        loadMoreData()
                    }
                },
                onImageClick = {
                    if (!favoriteMode) {
                        openDetail(it)
                    }
                },
                pageLabelProvider = if (favoriteMode) {
                    { _, _ -> null }
                } else {
                    ::pageLabelFor
                },
                showWebAction = !favoriteMode,
            )
        }
    }

    override fun initByResume() {
        super.initByResume()
        if (!favoriteMode) {
            loadNewData()
        }
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

    override fun onClearComposeView() {
        clearViewRequests()
    }

    private fun canLoadMore(): Boolean {
        return !isRefreshing && !isLoadingMore.get() && !isLoadEnd.get()
    }

    private fun requestImages(mode: LoadMode) {
        val scope = composeRequestScope ?: return
        val start = when (mode) {
            LoadMode.Refresh -> 0
            LoadMode.More -> loadStartIndex
        }
        val requestId = nextRequestId()

        activeRequest?.cancel()
        beginLoading(mode)
        activeRequest = scope.launch {
            try {
                val list = loadImages(start = start)
                if (isActiveRequest(requestId = requestId, scope = scope)) {
                    applyLoadedImages(mode = mode, start = start, list = list)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                if (isActiveRequest(requestId = requestId, scope = scope)) {
                    LogComponent.printE(tag = TAG, message = throwable.toString())
                }
            } finally {
                if (isActiveRequest(requestId = requestId, scope = scope)) {
                    endLoading(mode)
                    activeRequest = null
                }
            }
        }
    }

    private fun beginLoading(mode: LoadMode) {
        when (mode) {
            LoadMode.Refresh -> {
                loadStartIndex = 0
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
        start: Int,
        list: List<ImageLoadsInfo>,
    ) {
        when (mode) {
            LoadMode.Refresh -> {
                loadStartIndex = start + LOAD_MAX_SIZE
                pageLabels.clear()
                if (list.isNotEmpty()) {
                    pageLabels[0] = start.toPageNumber()
                    SourceLocalDataStore.saveCover(
                        sourceType = Constants.THEME_TYPE_XIUREN,
                        cover = list.first().url,
                    )
                }
                images.clear()
                images.addAll(list)
                isLoadEnd.set(list.isEmpty())
            }

            LoadMode.More -> {
                if (list.isEmpty()) {
                    isLoadEnd.set(true)
                } else {
                    pageLabels[images.size] = start.toPageNumber()
                    loadStartIndex = start + LOAD_MAX_SIZE
                    images.addAll(list)
                }
            }
        }
    }

    private fun pageLabelFor(index: Int, imageInfo: ImageLoadsInfo): String? {
        return pageLabels[index]?.let { "第 $it 页" }
    }

    private fun Int.toPageNumber(): Int {
        return this / LOAD_MAX_SIZE + 1
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

    private suspend fun loadImages(start: Int): List<ImageLoadsInfo> {
        return XiuRenRepository.getImages(
            dataUseFrom = dataInfo?.dataUseFrom,
            start = start,
        )
    }

    private fun openDetail(itemInfo: ImageLoadsInfo) {
        activity?.let { act ->
            PersonDetailActivity.start(
                activity = act,
                data = itemInfo.copy(fromType = Constants.THEME_TYPE_XIUREN),
            )
        }
    }

    private fun openWebView() {
        activity?.let { act ->
            XiuRenWebViewActivity.start(context = act)
        }
    }
}

@Preview(name = "XiuRen Fragment", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun XiuRenFragmentPreview() {
    ImageLoadsTheme {
        ImageListScreen(
            title = "秀人网",
            images = xiuRenPreviewImages,
            isRefreshing = false,
            isLoadingMore = true,
            onBack = {},
            onOpenWeb = {},
            onLoadMore = {},
            onImageClick = {},
        )
    }
}

private val xiuRenPreviewImages = listOf(
    ImageLoadsInfo(url = "preview://xiuren-1", width = 1024, height = 1536),
    ImageLoadsInfo(url = "preview://xiuren-2", width = 900, height = 1280),
    ImageLoadsInfo(url = "preview://xiuren-3", width = 1200, height = 1600),
    ImageLoadsInfo(url = "preview://xiuren-4", width = 960, height = 1440),
    ImageLoadsInfo(url = "preview://xiuren-5", width = 1024, height = 1400),
    ImageLoadsInfo(url = "preview://xiuren-6", width = 960, height = 1300),
)

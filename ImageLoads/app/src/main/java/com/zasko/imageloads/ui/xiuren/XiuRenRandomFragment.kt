package com.zasko.imageloads.ui.xiuren

import android.os.Build
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.XiuRenListScreen
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.fragment.ComposeBaseFragment
import com.zasko.imageloads.ui.xiuren.activity.XiuRenWebViewActivity

class XiuRenRandomFragment : ComposeBaseFragment() {

    companion object {
        private const val TAG = "XiuRenRandomFragment"
        private const val LOAD_MAX_SIZE = 20
        private const val START_IMAGE_ID = 17351
        private const val IMAGE_WIDTH = 1024
        private const val IMAGE_HEIGHT = 1535
        private const val IMAGE_URL = "https://i.xiutaku.com/photo/uploadfile/pic/%d.webp"

        fun newInstance(data: MainThemeSelectInfo): XiuRenRandomFragment {
            return XiuRenRandomFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_DATA, data)
                }
            }
        }
    }

    private val images = mutableStateListOf<ImageLoadsInfo>()

    private var title = "升序图片"
    private var nextImageId = START_IMAGE_ID
    private var isRefreshing by mutableStateOf(false)
    private var isLoadingMoreState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "${readThemeInfo()?.title.orEmpty()} 升序".trim()
    }

    @Composable
    override fun FragmentContent() {
        ImageLoadsTheme {
            XiuRenListScreen(
                title = title,
                images = images,
                isRefreshing = isRefreshing,
                isLoadingMore = isLoadingMoreState,
                onBack = { activity?.finish() },
                onOpenWeb = ::openWebView,
                onLoadMore = ::loadMoreImages,
                onImageClick = {},
                showWebAction = false,
            )
        }
    }

    override fun initByResume() {
        super.initByResume()
        images.addAll(createSequentialImages())
    }

    private fun loadMoreImages() {
        if (isLoadingMore.get()) {
            return
        }

        isLoadingMore.set(true)
        isLoadingMoreState = true
        images.addAll(createSequentialImages())
        isLoadingMore.set(false)
        isLoadingMoreState = false
    }

    private fun createSequentialImages(): List<ImageLoadsInfo> {
        val startId = nextImageId
        val endId = startId + LOAD_MAX_SIZE - 1
        nextImageId = endId + 1

        return (startId..endId).map { imageId ->
            ImageLoadsInfo(
                url = IMAGE_URL.format(imageId),
                width = IMAGE_WIDTH,
                height = IMAGE_HEIGHT,
            ).apply {
                LogComponent.printD(tag = TAG, message = url)
            }
        }
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

    private fun openWebView() {
        activity?.let { act ->
            XiuRenWebViewActivity.start(context = act)
        }
    }
}

@Preview(name = "XiuRen Random", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun XiuRenRandomFragmentPreview() {
    ImageLoadsTheme {
        XiuRenListScreen(
            title = "秀人网 升序",
            images = List(8) { index ->
                ImageLoadsInfo(
                    url = "preview://image-$index",
                    width = 1024,
                    height = 1100 + index * 70,
                )
            },
            isRefreshing = false,
            isLoadingMore = false,
            onBack = {},
            onOpenWeb = {},
            onLoadMore = {},
            onImageClick = {},
            showWebAction = false,
        )
    }
}

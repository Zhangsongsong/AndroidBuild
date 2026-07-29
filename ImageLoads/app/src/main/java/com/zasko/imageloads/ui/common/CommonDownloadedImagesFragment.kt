package com.zasko.imageloads.ui.common

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.XiuRenListScreen
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.fragment.ComposeBaseFragment
import com.zasko.imageloads.fragment.ImagePreviewFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CommonDownloadedImagesFragment : ComposeBaseFragment() {

    companion object {
        private const val KEY_NAME = "key_name"
        private const val KEY_PATH = "key_path"

        fun newInstance(name: String, path: String): CommonDownloadedImagesFragment {
            return CommonDownloadedImagesFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_NAME, name)
                    putString(KEY_PATH, path)
                }
            }
        }
    }

    private val images = mutableStateListOf<ImageLoadsInfo>()
    private var title by mutableStateOf("")
    private var path = ""
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString(KEY_NAME).orEmpty()
        path = arguments?.getString(KEY_PATH).orEmpty()
    }

    @Composable
    override fun FragmentContent() {
        ImageLoadsTheme {
            XiuRenListScreen(
                title = title.ifBlank { "已下载图片" },
                images = images,
                isRefreshing = isLoading,
                isLoadingMore = false,
                onBack = {
                    parentFragmentManager.popBackStack()
                },
                onOpenWeb = {},
                onLoadMore = {},
                onImageClick = ::openImagePreview,
                showWebAction = false,
            )
        }
    }

    override fun initByResume() {
        super.initByResume()
        loadImages()
    }

    private fun loadImages() {
        val scope = composeRequestScope ?: return
        isLoading = true
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                readImages()
            }
            images.clear()
            images.addAll(list)
            isLoading = false
        }
    }

    private fun readImages(): List<ImageLoadsInfo> {
        val parentFile = File(path)
        if (!parentFile.exists() || !parentFile.isDirectory) {
            return emptyList()
        }
        return parentFile.listFiles()
            ?.filter { it.isFile && it.isDownloadedImageFile() }
            ?.sortedBy { it.name }
            ?.map { file ->
                val size = file.readImageSize()
                ImageLoadsInfo(
                    url = file.absolutePath,
                    width = size.first,
                    height = size.second,
                )
            }
            ?: emptyList()
    }

    private fun openImagePreview(imageInfo: ImageLoadsInfo) {
        ImagePreviewFragment.show(
            fragmentManager = parentFragmentManager,
            imageUrl = imageInfo.url,
            referer = "",
        )
    }

    private fun File.readImageSize(): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(absolutePath, options)
        return options.outWidth to options.outHeight
    }
}

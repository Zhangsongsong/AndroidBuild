package com.zasko.imageloads.ui.meizi5

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zasko.imageloads.compose.HasDownloadScreen
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.data.HasDownloadInfo
import com.zasko.imageloads.fragment.ComposeBaseFragment
import com.zasko.imageloads.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class Meizi5DetailHasDownloadFragment : ComposeBaseFragment() {

    companion object {
        fun newInstance(): Meizi5DetailHasDownloadFragment {
            return Meizi5DetailHasDownloadFragment()
        }
    }

    private val downloads = mutableStateListOf<HasDownloadInfo>()
    private var isLoading by mutableStateOf(false)

    @Composable
    override fun FragmentContent() {
        ImageLoadsTheme {
            HasDownloadScreen(
                items = downloads,
                isLoading = isLoading,
                onBack = { activity?.finish() },
                onItemClick = { info ->
                    (activity as? Meizi5DetailHasDownloadActivity)?.showDownloadedImages(
                        name = info.name,
                        path = info.path,
                    )
                },
            )
        }
    }

    override fun initByResume() {
        super.initByResume()
        loadDownloads()
    }

    private fun loadDownloads() {
        val scope = composeRequestScope ?: return
        isLoading = true
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                readDownloads()
            }
            downloads.clear()
            downloads.addAll(list)
            isLoading = false
        }
    }

    private fun readDownloads(): List<HasDownloadInfo> {
        val parentFile = File(
            "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_MEIZI5}/${FileUtil.PICTURE_MEIZI5_DETAIL}",
        )
        if (!parentFile.exists() || !parentFile.isDirectory) {
            return emptyList()
        }
        return parentFile.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val imageList = file.listFiles()
                    ?.filter { it.isFile && it.isMeizi5ImageFile() }
                    ?.sortedBy { it.name }
                    ?.map { it.absolutePath }
                    ?.take(3)
                    ?: emptyList()
                HasDownloadInfo(name = file.name, path = file.absolutePath, images = imageList)
            }
            ?: emptyList()
    }
}

internal fun File.isMeizi5ImageFile(): Boolean {
    val fileName = name.lowercase()
    return fileName.endsWith(".jpg") ||
        fileName.endsWith(".jpeg") ||
        fileName.endsWith(".png") ||
        fileName.endsWith(".webp")
}

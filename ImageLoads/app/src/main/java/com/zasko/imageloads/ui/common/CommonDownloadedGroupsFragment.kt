package com.zasko.imageloads.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zasko.imageloads.compose.HasDownloadScreen
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.data.HasDownloadInfo
import com.zasko.imageloads.fragment.ComposeBaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CommonDownloadedGroupsFragment : ComposeBaseFragment() {

    companion object {
        private const val KEY_PARENT_PATH = "key_parent_path"

        fun newInstance(parentPath: String): CommonDownloadedGroupsFragment {
            return CommonDownloadedGroupsFragment().apply {
                arguments = android.os.Bundle().apply {
                    putString(KEY_PARENT_PATH, parentPath)
                }
            }
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
                    (activity as? CommonDownloadedActivity)?.showDownloadedImages(
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
        val parentFile = File(readParentPath())
        if (!parentFile.exists() || !parentFile.isDirectory) {
            return emptyList()
        }
        return parentFile.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val imageList = file.listFiles()
                    ?.filter { it.isFile && it.isDownloadedImageFile() }
                    ?.sortedBy { it.name }
                    ?.map { it.absolutePath }
                    ?.take(3)
                    ?: emptyList()
                HasDownloadInfo(name = file.name, path = file.absolutePath, images = imageList)
            }
            ?: emptyList()
    }

    private fun readParentPath(): String {
        return arguments?.getString(KEY_PARENT_PATH).orEmpty()
    }
}

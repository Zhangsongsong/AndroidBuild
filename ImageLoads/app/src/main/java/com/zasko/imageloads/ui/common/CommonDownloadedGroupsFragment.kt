package com.zasko.imageloads.ui.common

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    private var pendingDeleteDownload by mutableStateOf<HasDownloadInfo?>(null)

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
                onItemDelete = { info ->
                    pendingDeleteDownload = info
                },
            )
            pendingDeleteDownload?.let { info ->
                AlertDialog(
                    onDismissRequest = {
                        pendingDeleteDownload = null
                    },
                    text = {
                        Text(text = "确认删除「${info.name}」？会直接删除本地文件。")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pendingDeleteDownload = null
                                deleteDownload(info)
                            },
                        ) {
                            Text(text = "删除")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                pendingDeleteDownload = null
                            },
                        ) {
                            Text(text = "取消")
                        }
                    },
                )
            }
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

    private fun deleteDownload(info: HasDownloadInfo) {
        val scope = composeRequestScope ?: return
        val parentDir = File(readParentPath())
        val downloadDir = File(info.path)
        scope.launch {
            val deleted = withContext(Dispatchers.IO) {
                SourceImageDownloadHelper.deleteDownloadedDetailFolder(
                    parentDir = parentDir,
                    detailDownloadDir = downloadDir,
                )
            }
            if (deleted) {
                downloads.removeAll { it.path == info.path }
                showToast("已删除")
            } else {
                showToast("删除失败")
            }
        }
    }

    private fun readParentPath(): String {
        return arguments?.getString(KEY_PARENT_PATH).orEmpty()
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}

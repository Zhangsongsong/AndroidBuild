package com.zasko.imageloads.ui.common

import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
    private val selectedDownloadPaths = mutableStateListOf<String>()
    private var isLoading by mutableStateOf(false)
    private var isSelectionMode by mutableStateOf(false)
    private var pendingDeleteDownloads by mutableStateOf<List<HasDownloadInfo>>(emptyList())

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isSelectionMode) {
                        clearSelection()
                    } else {
                        activity?.finish()
                    }
                }
            },
        )
    }

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
                    pendingDeleteDownloads = listOf(info)
                },
                isSelectionMode = isSelectionMode,
                selectedItemKeys = selectedDownloadPaths.toSet(),
                onSelectAll = ::selectAllDownloads,
                onClearSelection = ::clearSelection,
                onItemSelectToggle = ::toggleDownloadSelection,
                onDeleteSelected = ::confirmDeleteSelectedDownloads,
            )
            pendingDeleteDownloads.takeIf { it.isNotEmpty() }?.let { pendingDownloads ->
                AlertDialog(
                    onDismissRequest = {
                        pendingDeleteDownloads = emptyList()
                    },
                    text = {
                        Text(text = pendingDownloads.toDeleteConfirmText())
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pendingDeleteDownloads = emptyList()
                                deleteDownloads(pendingDownloads)
                            },
                        ) {
                            Text(text = "删除")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                pendingDeleteDownloads = emptyList()
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
            selectedDownloadPaths.removeAll { selectedPath -> list.none { it.path == selectedPath } }
            if (downloads.isEmpty()) {
                clearSelection()
            }
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

    private fun selectAllDownloads() {
        selectedDownloadPaths.clear()
        selectedDownloadPaths.addAll(downloads.map { it.path })
        isSelectionMode = true
    }

    private fun clearSelection() {
        selectedDownloadPaths.clear()
        isSelectionMode = false
    }

    private fun toggleDownloadSelection(info: HasDownloadInfo) {
        isSelectionMode = true
        if (selectedDownloadPaths.contains(info.path)) {
            selectedDownloadPaths.remove(info.path)
        } else {
            selectedDownloadPaths.add(info.path)
        }
    }

    private fun confirmDeleteSelectedDownloads() {
        val pendingDownloads = downloads.filter { selectedDownloadPaths.contains(it.path) }
        if (pendingDownloads.isEmpty()) {
            return
        }
        pendingDeleteDownloads = pendingDownloads
    }

    private fun deleteDownloads(items: List<HasDownloadInfo>) {
        val scope = composeRequestScope ?: return
        val parentDir = File(readParentPath())
        scope.launch {
            val deletedPaths = withContext(Dispatchers.IO) {
                items.mapNotNull { info ->
                    val deleted = SourceImageDownloadHelper.deleteDownloadedDetailFolder(
                        parentDir = parentDir,
                        detailDownloadDir = File(info.path),
                    )
                    info.path.takeIf { deleted }
                }
            }
            if (deletedPaths.isNotEmpty()) {
                downloads.removeAll { it.path in deletedPaths }
                selectedDownloadPaths.removeAll { it in deletedPaths }
            }
            if (selectedDownloadPaths.isEmpty()) {
                isSelectionMode = false
            }
            if (deletedPaths.size == items.size) {
                showToast("已删除 ${deletedPaths.size} 个")
            } else if (deletedPaths.isNotEmpty()) {
                showToast("已删除 ${deletedPaths.size}/${items.size} 个")
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

    private fun List<HasDownloadInfo>.toDeleteConfirmText(): String {
        return if (size == 1) {
            "确认删除「${first().name}」？会直接删除本地文件。"
        } else {
            "确认删除选中的 $size 个下载？会直接删除本地文件。"
        }
    }
}

package com.zasko.imageloads.ui.meizi5

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.XiuRenListScreen
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single
import java.io.File

class Meizi5HasDownloadActivity : BaseComposeActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, Meizi5HasDownloadActivity::class.java))
        }
    }

    private val covers = mutableStateListOf<ImageLoadsInfo>()
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadsTheme {
                XiuRenListScreen(
                    title = "已下载封面",
                    images = covers,
                    isRefreshing = isLoading,
                    isLoadingMore = false,
                    onBack = ::finish,
                    onOpenWeb = {},
                    onLoadMore = {},
                    onImageClick = {},
                    showWebAction = false,
                )
            }
        }
        getData()
    }

    private fun getData() {
        isLoading = true
        Single.fromCallable {
            val parentFile = getCoverDownloadDir()
            if (!parentFile.exists() || !parentFile.isDirectory) {
                return@fromCallable emptyList<ImageLoadsInfo>()
            }
            parentFile.listFiles()
                ?.filter { it.isFile && it.isImageFile() }
                ?.sortedByDescending { it.lastModified() }
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
            .switchThread()
            .doOnSuccess { list ->
                covers.clear()
                covers.addAll(list)
            }
            .doFinally { isLoading = false }
            .bindLife()
    }

    private fun getCoverDownloadDir(): File {
        return File(
            "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_MEIZI5}/${FileUtil.PICTURE_MEIZI5_COVERS}",
        )
    }

    private fun File.isImageFile(): Boolean {
        val fileName = name.lowercase()
        return fileName.endsWith(".jpg") ||
            fileName.endsWith(".jpeg") ||
            fileName.endsWith(".png") ||
            fileName.endsWith(".webp")
    }

    private fun File.readImageSize(): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(absolutePath, options)
        return options.outWidth to options.outHeight
    }
}

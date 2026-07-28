package com.zasko.imageloads.ui.xiuren

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.HasDownloadScreen
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.data.HasDownloadInfo
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

class XiuRenHasDownloadActivity : BaseComposeActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, XiuRenHasDownloadActivity::class.java))
        }
    }

    private val downloads = mutableStateListOf<HasDownloadInfo>()
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadsTheme {
                HasDownloadScreen(
                    items = downloads,
                    isLoading = isLoading,
                    onBack = ::finish,
                )
            }
        }
        getData()
    }

    private fun getData() {
        isLoading = true
        Single.just(true)
            .subscribeOn(Schedulers.io())
            .map {
                val parentFile = File(FileUtil.getDownloadPath() + "/${FileUtil.PICTURE_XIUREN}")
                if (!parentFile.exists() || !parentFile.isDirectory) {
                    return@map emptyList()
                }
                parentFile.listFiles()
                    ?.filter { it.isDirectory }
                    ?.sortedByDescending { it.lastModified() }
                    ?.map { file ->
                        val imageList = file.listFiles()
                            ?.filter { it.isFile }
                            ?.sortedBy { it.name }
                            ?.map { it.absolutePath }
                            ?.take(3)
                            ?: emptyList()
                        HasDownloadInfo(name = file.name, path = file.absolutePath, images = imageList)
                    }
                    ?: emptyList()
            }
            .switchThread()
            .doOnSuccess { list ->
                downloads.clear()
                downloads.addAll(list)
            }
            .doFinally { isLoading = false }
            .bindLife()
    }
}

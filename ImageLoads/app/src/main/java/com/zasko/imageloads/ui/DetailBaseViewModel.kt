package com.zasko.imageloads.ui

import android.app.Activity
import android.content.Context
import com.bumptech.glide.Glide
import com.zasko.imageloads.base.BaseViewModel
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.listener.DownloadListener
import com.zasko.imageloads.listener.GettingImageListener
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.PermissionUtil
import com.zasko.imageloads.utils.toFileNameByIndex
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.collections.plus
import kotlin.io.copyTo

abstract class DetailBaseViewModel : BaseViewModel() {

    companion object {
        private const val TAG = "DetailBaseViewModel"
    }


    open fun getDownloadDir(parentPath: String): String {
        val file = File("${FileUtil.getDownloadPath()}/${parentPath}")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath
    }


    fun createAndNeedPermission(activity: Activity) {
        FileUtil.createExternalDir()
        PermissionUtil.getReadAndWriteExternal(context = activity)
    }

    open fun checkHasDownload(): Boolean {
        return false
    }

    abstract fun getDetailInfo(): Single<ImageDetailInfo>


    abstract fun getDetailMore(pageIndex: Int): Single<List<ImageInfo>>

    open fun getImageList(listener: GettingImageListener?): Single<List<ImageInfo>> {
        return getDetailInfo().map { it.pictures ?: emptyList() }.flatMap { firstList ->
            listener?.onGettingPage(1)
            if (firstList.isEmpty()) {
                Single.just(emptyList())
            } else {
                val lastItem = firstList.last()
                fetchAllPages(page = 2, listener = listener, lastItem).map { nextList ->
                    firstList + nextList
                }
            }
        }
    }

    private fun fetchAllPages(page: Int, listener: GettingImageListener?, lastPagerItem: ImageInfo?): Single<List<ImageInfo>> {
        return getDetailMore(page).delay(5, TimeUnit.SECONDS).flatMap { list ->
            listener?.onGettingPage(page)
            if (list.isEmpty()) {
                Single.just(emptyList())
            } else {
                val lastItem = list.last()
                if (lastItem.url == lastPagerItem?.url) {
                    Single.just(emptyList())
                } else {
                    fetchAllPages(page + 1, listener = listener, lastItem).map { nextList ->
                        list + nextList
                    }
                }
            }
        }
    }

    fun startDownload(context: Context, dir: String, pictures: List<ImageInfo>, listener: DownloadListener?): Job? {
        return CoroutineScope(Dispatchers.IO).launch {
            LogComponent.printD(TAG, "startDownload dir:${dir}")
            withContext(Dispatchers.IO) {
                val size = pictures.size - 1
                runInMain {
                    listener?.onStartDownload(all = size, dir = dir)
                }
                pictures.forEachIndexed { index, imageInfo ->
                    val fileName = imageInfo.url.toFileNameByIndex(index = index)
                    LogComponent.printD(TAG, "startDownload index::${index} imageInfo:${imageInfo} fileName:${fileName}")
                    runInMain {
                        listener?.onOneStartDownload(index = index, all = size, dir = dir, fileName = fileName)
                    }
                    val destFile = downloadImage(context = context, url = imageInfo.url, destFile = File(dir, fileName))
                    LogComponent.printD(TAG, "endDownload index::${index} file:${destFile}")
                    runInMain {
                        listener?.onOneEndDownLoad(index = index, all = size, dir = dir, fileName = fileName)
                    }
                }
                runInMain {
                    listener?.onEndDownload(all = size, dir = dir)
                }
            }
        }
    }

    private fun downloadImage(context: Context, url: String, destFile: File): File {
        val futureTarget = Glide.with(context).asFile().load(url).submit()
        val cacheFile = futureTarget.get()
        cacheFile.copyTo(destFile, overwrite = true)
        Glide.with(context).clear(futureTarget)
        return destFile
    }


    private suspend fun runInMain(blocking: () -> Unit) {
        withContext(Dispatchers.Main) {
            blocking.invoke()
        }
    }


}
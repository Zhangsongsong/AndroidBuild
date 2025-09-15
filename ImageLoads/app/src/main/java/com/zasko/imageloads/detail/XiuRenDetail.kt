package com.zasko.imageloads.detail

import android.content.Context
import com.bumptech.glide.Glide
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.toFileNameByIndex
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class XiuRenDetail : DetailAction {

    companion object {
        private const val TAG = "XiuRenDetail"

        private const val DOMAIN = "https://xiutaku.com"
    }

    private var loadsInfo: ImageLoadsInfo? = null

    fun setLoadsInfo(info: ImageLoadsInfo) {
        loadsInfo = info
    }

    private fun toPageUrl(page: Int = 1): String {
        return "${DOMAIN}${loadsInfo?.href}?page=${page}"
    }


    override fun getDetailInfo(): Single<ImageDetailInfo> {
        return ImageLoadsManager.getXiuRenDetail(url = toPageUrl(page = 1))
    }

    override fun getNextPageIndex(cIndex: Int): Int {
        return cIndex + 1
    }

    override fun getDetailMore(pageIndex: Int): Single<List<ImageInfo>> {
        return ImageLoadsManager.getXiuRenDetailMore(url = toPageUrl(page = pageIndex))
    }

    override fun getDownloadDir(): String {
        val file = File("${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_XIUREN}")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath
    }

    override fun getParentFile(parentName: String): String {
        val parentFile = File("${getDownloadDir()}/${parentName}")
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        return parentFile.absolutePath
    }

    override fun getImageList(listener: GettingImageListener?): Single<List<ImageInfo>> {
        return getDetailInfo().map { it.pictures ?: emptyList() }.flatMap { firstList ->
            listener?.onGettingPage(0)
            if (firstList.isEmpty()) {
                Single.just(emptyList())
            } else {
                val lastItem = firstList.last()
                fetchAllPages(page = getNextPageIndex(cIndex = 1), listener = listener, lastItem).map { nextList ->
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

    override fun startDownload(context: Context, dir: String, pictures: List<ImageInfo>, listener: DownloadListener?): Job? {
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
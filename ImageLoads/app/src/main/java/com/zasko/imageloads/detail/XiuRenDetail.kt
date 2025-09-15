package com.zasko.imageloads.detail

import android.content.Context
import com.bumptech.glide.Glide
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.getUrlToSuffix
import com.zasko.imageloads.utils.toFileNameByIndex
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class XiuRenDetail : DetailAction {

    companion object {
        private const val TAG = "XiuRenDetail"
    }

    private var loadsInfo: ImageLoadsInfo? = null

    fun setLoadsInfo(info: ImageLoadsInfo) {
        loadsInfo = info
    }

    private fun toPageUrl(page: Int = 1): String {
        return "${loadsInfo?.href}?page=${page}"
    }


    override fun getDetailInfo(): Single<ImageDetailInfo> {
        return ImageLoadsManager.getXiuRenDetail(url = loadsInfo?.href ?: "")
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

    override fun startDownload(context: Context, dir: String, pictures: List<ImageInfo>): Job? {
        return CoroutineScope(Dispatchers.IO).launch {
            LogComponent.printD(TAG, "startDownload dir:${dir}")
            withContext(Dispatchers.IO) {
                pictures.forEachIndexed { index, imageInfo ->
                    val fileName = imageInfo.url.toFileNameByIndex(index = index)
                    LogComponent.printD(TAG, "startDownload index::${index} imageInfo:${imageInfo} fileName:${fileName}")
                    val destFile = downloadImage(context = context, url = imageInfo.url, destFile = File(dir, fileName))
                    LogComponent.printD(TAG, "endDownload index::${index} file:${destFile}")
                }
            }
        }
    }

    private suspend fun downloadImage(context: Context, url: String, destFile: File): File {
        val futureTarget = Glide.with(context).asFile().load(url).submit()
        val cacheFile = futureTarget.get()
        cacheFile.copyTo(destFile, overwrite = true)
        Glide.with(context).clear(futureTarget)
        return destFile
    }

}
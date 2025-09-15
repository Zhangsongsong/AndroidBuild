package com.zasko.imageloads.detail

import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.FileUtil
import io.reactivex.rxjava3.core.Single
import java.io.File

class XiuRenDetail : DetailAction {

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



}
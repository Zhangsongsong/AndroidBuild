package com.zasko.imageloads.ui.xiuren

import android.content.Context
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.listener.DownloadListener
import com.zasko.imageloads.listener.GettingImageListener
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.ui.DetailBaseViewModel
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single
import java.io.File

class XiuRenViewDetailModel : DetailBaseViewModel() {

    companion object {
        private const val TAG = "XiuRenViewModel"
        private const val DOMAIN = "https://xiutaku.com"
    }

    private var currentDetailInfo: ImageDetailInfo? = null
    private var loadsInfo: ImageLoadsInfo? = null

    fun setLoadInfo(info: ImageLoadsInfo) {
        loadsInfo = info
    }

    private fun toPageUrl(page: Int = 1): String {
        return "${DOMAIN}${loadsInfo?.href}?page=${page}"
    }

    override fun getDetailInfo(): Single<ImageDetailInfo> {
        return ImageLoadsManager.getXiuRenDetail(url = toPageUrl(page = 1)).doOnSuccess {
            currentDetailInfo = it
        }
    }

    fun getNextPageIndex(currentIndex: Int): Int {
        return currentIndex + 1
    }

    override fun getDetailMore(pageIndex: Int): Single<List<ImageInfo>> {
        return ImageLoadsManager.getXiuRenDetailMore(url = toPageUrl(page = pageIndex))
    }


    override fun checkHasDownload(): Boolean {
        if (currentDetailInfo == null) {
            return false
        }
        val file = File(getDownloadDir(), currentDetailInfo?.name ?: "")
        return file.exists()
    }

    fun downloadPic(context: Context, gettingListener: GettingImageListener, listener: DownloadListener) {
        LogComponent.printD(TAG, "downloadPic:${getDownloadDir()}")
        LogComponent.printD(TAG, "downloadPic:${currentDetailInfo}")
        currentDetailInfo?.let { info ->
            listener.onStartGettingMaxPage()
            getImageList(listener = gettingListener).switchThread().doOnSuccess { list ->
                LogComponent.printD(TAG, "downloadPic size:${list.size}")
                startDownload(context = context, dir = getParentFile(parentName = info.name), pictures = list, listener = listener)?.bindLifeJob()
            }.doFinally { listener.onEndGettingMaxPage() }.bindLife()
        }
    }

    private fun getDownloadDir(): String {
        return getDownloadDir(parentPath = FileUtil.PICTURE_XIUREN)
    }

    private fun getParentFile(parentName: String): String {
        val parentFile = File("${getDownloadDir()}/${parentName}")
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        return parentFile.absolutePath

    }


}
package com.zasko.imageloads.viewmodel

import android.content.Context
import com.zasko.imageloads.base.BaseViewModel
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.detail.DetailAction
import com.zasko.imageloads.detail.XiuRenDetail
import com.zasko.imageloads.utils.Constants
import io.reactivex.rxjava3.core.Single

class ImageDetailViewModel : BaseViewModel() {

    companion object {
        private const val TAG = "ImageDetailViewModel"
    }


    private var loadsInfo: ImageLoadsInfo? = null

    private var currentDetailInfo: ImageDetailInfo? = null
    private lateinit var detailAction: DetailAction


    fun setLoadsInfo(info: ImageLoadsInfo) {
        loadsInfo = info
        detailAction = when (info.fromType) {
            Constants.THEME_TYPE_XIUREN -> XiuRenDetail().apply {
                this.setLoadsInfo(info = info)
            }

            else -> {
                object : DetailAction {}
            }
        }
    }

    fun getDetailInfo(): Single<ImageDetailInfo> {
        return detailAction.getDetailInfo().doOnSuccess {
            currentDetailInfo = it
        }
    }

    fun getNextPageIndex(currentIndex: Int): Int {
        return detailAction.getNextPageIndex(cIndex = currentIndex)
    }

    fun getDetailMore(pageIndex: Int): Single<List<ImageInfo>> {
        return detailAction.getDetailMore(pageIndex = pageIndex)
    }


    fun downloadPic(context: Context) {
        LogComponent.printD(TAG, "downloadPic:${detailAction.getDownloadDir()}")
        LogComponent.printD(TAG, "downloadPic:${currentDetailInfo}")
        currentDetailInfo?.let { info ->
            detailAction.startDownload(
                context = context, dir = detailAction.getParentFile(parentName = info.name), pictures = info.pictures ?: emptyList()
            )?.bindLifeJob()
        }

    }


}
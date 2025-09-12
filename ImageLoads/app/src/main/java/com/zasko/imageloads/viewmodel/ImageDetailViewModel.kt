package com.zasko.imageloads.viewmodel

import com.zasko.imageloads.base.BaseViewModel
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single

class ImageDetailViewModel : BaseViewModel() {


    private var loadsInfo: ImageLoadsInfo? = null

    fun setLoadsInfo(info: ImageLoadsInfo) {
        loadsInfo = info
    }

    fun getDetailInfo(): Single<ImageDetailInfo> {
        return when (loadsInfo?.fromType) {
            Constants.THEME_TYPE_XIUREN -> {
                ImageLoadsManager.getXiuRenDetail(url = loadsInfo?.href ?: "")
            }

            else -> Single.just(ImageDetailInfo())
        }
    }
}
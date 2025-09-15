package com.zasko.imageloads.detail

import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import io.reactivex.rxjava3.core.Single

interface DetailAction {

    fun getDetailInfo(): Single<ImageDetailInfo> {
        return Single.just(ImageDetailInfo())
    }

    fun getNextPageIndex(cIndex: Int): Int {
        return cIndex + 1
    }


    fun getDetailMore(pageIndex: Int): Single<List<ImageInfo>> {
        return Single.just(arrayListOf(ImageInfo()))
    }

    fun getDownloadDir(): String {
        return ""
    }

}
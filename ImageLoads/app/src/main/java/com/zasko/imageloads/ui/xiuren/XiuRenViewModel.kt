package com.zasko.imageloads.ui.xiuren

import com.zasko.imageloads.base.BaseViewModel
import com.zasko.imageloads.data.ImageLoadsInfo
import io.reactivex.rxjava3.core.Single

class XiuRenViewModel : BaseViewModel() {

    fun getNetworkData(start: Int): Single<List<ImageLoadsInfo>> {
        return XiuRenRepository.getNetworkData(start = start)
    }

    fun getLocalData(start: Int): Single<List<ImageLoadsInfo>> {
        return XiuRenRepository.getLocalData(start = start)
    }
}

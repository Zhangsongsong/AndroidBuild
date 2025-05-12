package com.zasko.imageloads.manager

import android.annotation.SuppressLint
import com.zasko.imageloads.components.HttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.services.ImageLoadsServices
import com.zasko.imageloads.utils.switchThread

object ImageLoadsManager {


    private val imageServer by lazy {
        HttpComponent.getRetrofit().create(ImageLoadsServices::class.java)
    }

    @SuppressLint("CheckResult")
    fun getImageData() {
        imageServer.getImage().switchThread().doOnSuccess { result ->
            LogComponent.printD(tag = "ImageLoadsManager", message = "result:$result")
        }.subscribe({}, {})
    }
}
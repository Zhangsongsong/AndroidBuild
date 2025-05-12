package com.zasko.imageloads.manager

import com.zasko.imageloads.components.HttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.data.TestInfo
import com.zasko.imageloads.services.ImageLoadsServices
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Job
import java.util.concurrent.TimeUnit

object ImageLoadsManager {

    private const val TAG = "ImageLoadsManager"

    private val imageServer by lazy {
        HttpComponent.getRetrofit().create(ImageLoadsServices::class.java)
    }

    fun getImageData(): Single<MainLoadsInfo> {
        return imageServer.getImage().switchThread()
    }


    private var looperDisposable: Disposable? = null

    fun looperLoadImage(next: () -> Unit) {
        disLoadingImage()
        looperDisposable = Observable.interval(0, 5, TimeUnit.SECONDS).doOnNext {
            LogComponent.printD(tag = TAG, message = "loadingImage:${it}")
            next.invoke()
        }.subscribe({}, {})
    }

    fun disLoadingImage() {
        looperDisposable?.dispose()
        looperDisposable = null
    }
}
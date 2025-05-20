package com.zasko.imageloads.manager

import android.content.Context
import com.zasko.imageloads.components.HttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.HeiSiInfo
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.manager.html.HtmlParseManager
import com.zasko.imageloads.services.ImageLoadsServices
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

object ImageLoadsManager {

    private const val TAG = "ImageLoadsManager"

    private val imageServer by lazy {
        HttpComponent.getRetrofit().create(ImageLoadsServices::class.java)
    }

    fun getImageData(): Single<HeiSiInfo> {
        return imageServer.getImage().switchThread()
    }


    private var looperDisposable: Disposable? = null

    fun looperLoadImage(next: () -> Unit) {
        disLoadingImage()
        looperDisposable = Observable.interval(15, 15, TimeUnit.SECONDS).doOnNext {
            LogComponent.printD(tag = TAG, message = "loadingImage:${it}")
            next.invoke()
        }.subscribe({}, {})
    }

    fun disLoadingImage() {
        looperDisposable?.dispose()
        looperDisposable = null
    }


    fun getXiuTaKuData(): Single<String> {
        return imageServer.getXiuTaKu().switchThread().doOnSuccess {
//            LogComponent.printD(tag = TAG, message = "getXiuTaKuData data:${it}")
        }
    }


    fun getXiuRenData(start: Int = 0): Single<List<MainLoadsInfo>> {
        return imageServer.getXiuTaKu(start = start).map { data ->
//            return HtmlParseManager.parseXiuRenByLocal(context = context).map { data ->
            val doc = Jsoup.parse(data.toString())
            val itemLinks = doc.getElementsByClass("item-link")
            itemLinks.map {
                it.getElementsByTag("img")
            }.filter {
                it.isNotEmpty()
            }.map {
                MainLoadsInfo(url = it.attr("src"), width = it.attr("width").toInt(), height = it.attr("height").toInt())
            }
        }
    }

}
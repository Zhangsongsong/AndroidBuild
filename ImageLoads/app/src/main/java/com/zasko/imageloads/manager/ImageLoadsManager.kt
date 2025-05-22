package com.zasko.imageloads.manager

import android.content.Context
import com.zasko.imageloads.MApplication
import com.zasko.imageloads.components.HttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.HeiSiInfo
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.manager.html.HtmlParseManager
import com.zasko.imageloads.services.ImageLoadsServices
import com.zasko.imageloads.utils.BuildConfig
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import kotlin.math.sin

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
        val single = if (BuildConfig.isUseLocal) {
            HtmlParseManager.parseXiuRenByLocal(context = MApplication.application)
        } else {
            imageServer.getXiuTaKu(start = start)
        }
        return single.map { data ->
            val doc = Jsoup.parse(data.toString())
            val itemLinks = doc.getElementsByClass("item-link")
//            LogComponent.printD(tag = TAG, message = "getXiuRenData item_link:${itemLinks.firstOrNull()?.getElementsByTag("a")}")
            val resultList = mutableListOf<MainLoadsInfo>()
            itemLinks.forEach { item ->
                item.getElementsByTag("a").firstOrNull()?.let { aInfo ->
                    val href = aInfo.attr("href")
                    item.getElementsByTag("img").firstOrNull()?.let { img ->
                        resultList.add(MainLoadsInfo(
                            url = img.attr("src"), width = img.attr("width").toInt(), height = img.attr("height").toInt(), href = href
                        ).apply {
                            LogComponent.printD(tag = TAG, message = "getXiuRenData mainLoadInfo:${this}")
                        })
                    }
                }
            }
            resultList
        }
    }

}
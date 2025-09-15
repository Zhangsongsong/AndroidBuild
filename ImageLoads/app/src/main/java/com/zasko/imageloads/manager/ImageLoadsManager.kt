package com.zasko.imageloads.manager

import com.zasko.imageloads.MApplication
import com.zasko.imageloads.components.HttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.HeiSiInfo
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.TagsInfo
import com.zasko.imageloads.manager.html.HtmlParseManager
import com.zasko.imageloads.services.ImageLoadsServices
import com.zasko.imageloads.utils.BuildConfig
import com.zasko.imageloads.utils.MJson
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

object ImageLoadsManager {

    private const val TAG = "ImageLoadsManager"

    val imageServer by lazy {
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


    fun getXiuRenData(start: Int = 0, domain: String = ""): Single<List<ImageLoadsInfo>> {
        val single = if (BuildConfig.isUseLocal) {
            HtmlParseManager.parseXiuRenByLocal(context = MApplication.application)
        } else {
            imageServer.getXiuRen(start = start)
        }
        return single.map { data ->
            val doc = MJson.parse(data.toString())
            val itemLinks = doc.getElementsByClass("item-link")
//            LogComponent.printD(tag = TAG, message = "getXiuRenData item_link:${itemLinks.firstOrNull()?.getElementsByTag("a")}")
            val resultList = mutableListOf<ImageLoadsInfo>()
            itemLinks.forEach { item ->
                item.getElementsByTag("a").firstOrNull()?.let { aInfo ->
                    var href = aInfo.attr("href")
                    if (!href.startsWith("http")) {
                        href = domain + href
                    }
                    item.getElementsByTag("img").firstOrNull()?.let { img ->
                        resultList.add(ImageLoadsInfo(
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

    fun getXiuRenDetail(url: String = ""): Single<ImageDetailInfo> {
//        return HtmlParseManager.parseXiuRenDetail(context = MApplication.application).map { data ->
        return imageServer.getXiuRenDetail(url).map { data ->
            val doc = MJson.parse(data.toString())
            val name = doc.getElementsByClass("article-header").firstOrNull()?.getElementsByTag("h1")?.firstOrNull()?.text() ?: ""
            var time = ""
            var desc = ""
            var tags = doc.getElementsByClass("article-tags").firstOrNull()?.getElementsByClass("tag")?.map {
                it.getElementsByTag("span").firstOrNull()?.text()
            }?.map {
                TagsInfo(id = it ?: "")
            }
            val infos = doc.getElementsByClass("article-info")
            if (infos.size > 1) {
                time = infos.first()?.getElementsByTag("small")?.first()?.text() ?: ""
                desc = infos[1].text()
            }
            val pictures = doc.getElementsByClass("article-fulltext").firstOrNull()?.getElementsByTag("img")?.map {
                val src = it.attr("src")
                val width = it.attr("width").toInt()
                val height = it.attr("height").toInt()
                ImageInfo(url = src, width = width, height = height)
            }
            ImageDetailInfo(name = name, time = time, desc = desc, tags = tags, pictures = pictures)
        }
    }

    fun getXiuRenDetailMore(url: String = ""): Single<List<ImageInfo>> {
        LogComponent.printD(TAG, "getXiuRenDetailMore url:${url}")
        return imageServer.getXiuRenDetail(url = url).map { data ->
            val doc = MJson.parse(data)
            doc.getElementsByClass("article-fulltext").firstOrNull()?.getElementsByTag("img")?.map {
                val src = it.attr("src")
                val width = it.attr("width").toInt()
                val height = it.attr("height").toInt()
                ImageInfo(url = src, width = width, height = height)
            } ?: emptyList()
        }
    }

}
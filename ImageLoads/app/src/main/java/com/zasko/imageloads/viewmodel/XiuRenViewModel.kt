package com.zasko.imageloads.viewmodel

import com.zasko.imageloads.MApplication
import com.zasko.imageloads.base.BaseViewModel
import com.zasko.imageloads.base.HtmlParse
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.manager.html.HtmlParseManager
import com.zasko.imageloads.utils.BuildConfig
import com.zasko.imageloads.utils.MJson
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single

class XiuRenViewModel : BaseViewModel(), HtmlParse {

    companion object {
        private const val TAG = "XiuRenViewModel"
    }

    override fun transformHome(data: String): List<ImageLoadsInfo> {
        val doc = MJson.parse(data.toString())
        val itemLinks = doc.getElementsByClass("item-link")
//            LogComponent.printD(tag = TAG, message = "getXiuRenData item_link:${itemLinks.firstOrNull()?.getElementsByTag("a")}")
        val resultList = mutableListOf<ImageLoadsInfo>()
        itemLinks.forEach { item ->
            item.getElementsByTag("a").firstOrNull()?.let { aInfo ->
                var href = aInfo.attr("href")
                if (!href.startsWith("http")) {
                    href = getDomain() + href
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
        return resultList
    }


    fun getNetworkData(start: Int): Single<List<ImageLoadsInfo>> {
        return ImageLoadsManager.imageServer.getXiuRen(start = start).map {
            transformHome(data = it)
        }
    }

    fun getLocalData(): Single<List<ImageLoadsInfo>> {
        return HtmlParseManager.parseXiuRenByLocal(context = MApplication.application).map { data ->
            transformHome(data = data.toString())
        }
    }



}
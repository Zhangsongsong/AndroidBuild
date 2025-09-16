package com.zasko.imageloads.viewmodel

import com.zasko.imageloads.base.BaseViewModel
import com.zasko.imageloads.base.HtmlParse
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.MJson
import io.reactivex.rxjava3.core.Single
import java.io.File

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
            savePageHtml(start, html = it)
            transformHome(data = it)
        }
    }

    fun getLocalData(start: Int): Single<List<ImageLoadsInfo>> {
        return Single.just(true).map {
            val file = File(getParentHtmlDir(), "$start")
            if (file.exists()) {
                transformHome(data = FileUtil.getFileToHtml(file)?.toString() ?: "")
            } else {
                emptyList()
            }
        }

        /* return HtmlParseManager.parseXiuRenByLocal(context = MApplication.application).map { data ->
             transformHome(data = data.toString())
         }*/
    }

    private fun getParentHtmlDir(): String {
        val parentFile = File(FileUtil.getPrivateHtmlDir() + "/${FileUtil.NAME_XIUREN}")
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        return parentFile.absolutePath
    }

    private fun savePageHtml(start: Int, html: String) {
        val file = File(getParentHtmlDir(), "$start")
        if (file.exists()) {
            file.delete()
        }
        file.writeText(html, Charsets.UTF_8)
        LogComponent.printD(TAG, "savePageHtml start:${start} html:${html.length}")
    }
}
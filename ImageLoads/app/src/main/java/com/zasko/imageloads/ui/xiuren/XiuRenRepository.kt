package com.zasko.imageloads.ui.xiuren

import com.zasko.imageloads.base.HtmlParse
import com.zasko.imageloads.components.ComposeHttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.manager.ImageLoadsManager
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.MJson
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object XiuRenRepository : HtmlParse {

    private const val TAG = "XiuRenRepository"

    suspend fun getImages(dataUseFrom: Int?, start: Int): List<ImageLoadsInfo> {
        return if (dataUseFrom == DataUseFrom.PRIVATE_FILE.value) {
            getLocalImages(start = start)
        } else {
            getNetworkImages(start = start)
        }
    }

    fun getNetworkData(start: Int): Single<List<ImageLoadsInfo>> {
        return ImageLoadsManager.imageServer.getXiuRen(start = start).map { html ->
            transformAndSaveNetworkData(start = start, html = html)
        }
    }

    fun getLocalData(start: Int): Single<List<ImageLoadsInfo>> {
        return Single.just(true).map {
            getLocalDataFromCache(start = start)
        }
    }

    override fun transformHome(data: String): List<ImageLoadsInfo> {
        val doc = MJson.parse(data)
        val itemLinks = doc.getElementsByClass("item-link")
        val resultList = mutableListOf<ImageLoadsInfo>()
        itemLinks.forEach { item ->
            item.getElementsByTag("a").firstOrNull()?.let { aInfo ->
                var href = aInfo.attr("href")
                if (!href.startsWith("http")) {
                    href = getDomain() + href
                }
                item.getElementsByTag("img").firstOrNull()?.let { img ->
                    resultList.add(
                        ImageLoadsInfo(
                            url = img.attr("src"),
                            width = img.attr("width").toInt(),
                            height = img.attr("height").toInt(),
                            href = href,
                        ).apply {
                            LogComponent.printD(tag = TAG, message = "getXiuRenData mainLoadInfo:${this}")
                        },
                    )
                }
            }
        }
        return resultList
    }

    private suspend fun getLocalImages(start: Int): List<ImageLoadsInfo> {
        return withContext(Dispatchers.IO) {
            getLocalDataFromCache(start = start)
        }
    }

    private suspend fun getNetworkImages(start: Int): List<ImageLoadsInfo> {
        val html = ComposeHttpComponent.getXiuRen(start = start)
        return withContext(Dispatchers.IO) {
            transformAndSaveNetworkData(start = start, html = html)
        }
    }

    private fun transformAndSaveNetworkData(start: Int, html: String): List<ImageLoadsInfo> {
        savePageHtml(start = start, html = html)
        return transformHome(data = html)
    }

    private fun getLocalDataFromCache(start: Int): List<ImageLoadsInfo> {
        val file = File(getParentHtmlDir(), "$start")
        return if (file.exists()) {
            transformHome(data = FileUtil.getFileToHtml(file)?.toString() ?: "")
        } else {
            emptyList()
        }
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

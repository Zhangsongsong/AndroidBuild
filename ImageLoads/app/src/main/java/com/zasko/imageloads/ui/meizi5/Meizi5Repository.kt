package com.zasko.imageloads.ui.meizi5

import com.zasko.imageloads.base.HtmlParse
import com.zasko.imageloads.components.ComposeHttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.MJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Meizi5Repository : HtmlParse {

    private const val TAG = "Meizi5Repository"
    private const val HOME_URL = "https://meizi5.com/"
    private const val DEFAULT_IMAGE_WIDTH = 600
    private const val DEFAULT_IMAGE_HEIGHT = 900

    suspend fun getImages(dataUseFrom: Int?, page: Int): List<ImageLoadsInfo> {
        return if (dataUseFrom == DataUseFrom.PRIVATE_FILE.value) {
            getLocalImages(page = page)
        } else {
            getNetworkImages(page = page)
        }
    }

    override fun getDomain(): String {
        return HOME_URL
    }

    override fun transformHome(data: String): List<ImageLoadsInfo> {
        val doc = MJson.parse(data)
        val resultList = mutableListOf<ImageLoadsInfo>()

        doc.select("article.masonry-item").forEach { article ->
            val link = article.selectFirst("a.entry-thumbnail") ?: return@forEach
            val img = link.selectFirst("img.wp-post-image") ?: link.selectFirst("img") ?: return@forEach
            val src = img.attr("src").toAbsoluteUrl()
            if (src.isBlank()) {
                return@forEach
            }

            resultList.add(
                ImageLoadsInfo(
                    url = src,
                    width = img.attr("width").trim().toIntOrNull() ?: DEFAULT_IMAGE_WIDTH,
                    height = img.attr("height").trim().toIntOrNull() ?: DEFAULT_IMAGE_HEIGHT,
                    href = link.attr("href").toAbsoluteUrl(),
                ).apply {
                    LogComponent.printD(tag = TAG, message = "item:${this}")
                },
            )
        }

        return resultList
    }

    private suspend fun getLocalImages(page: Int): List<ImageLoadsInfo> {
        return withContext(Dispatchers.IO) {
            getLocalDataFromCache(page = page)
        }
    }

    private suspend fun getNetworkImages(page: Int): List<ImageLoadsInfo> {
        val html = ComposeHttpComponent.getMeizi5(url = buildPageUrl(page = page))
        return withContext(Dispatchers.IO) {
            transformAndSaveNetworkData(page = page, html = html)
        }
    }

    private fun transformAndSaveNetworkData(page: Int, html: String): List<ImageLoadsInfo> {
        savePageHtml(page = page, html = html)
        return transformHome(data = html)
    }

    private fun getLocalDataFromCache(page: Int): List<ImageLoadsInfo> {
        val file = File(getParentHtmlDir(), "$page")
        return if (file.exists()) {
            transformHome(data = FileUtil.getFileToHtml(file)?.toString() ?: "")
        } else {
            emptyList()
        }
    }

    private fun savePageHtml(page: Int, html: String) {
        val file = File(getParentHtmlDir(), "$page")
        if (file.exists()) {
            file.delete()
        }
        file.writeText(html, Charsets.UTF_8)
        LogComponent.printD(tag = TAG, message = "savePageHtml page:${page} html:${html.length}")
    }

    private fun getParentHtmlDir(): String {
        val parentFile = File(FileUtil.getPrivateHtmlDir() + "/${FileUtil.NAME_MEIZI5}")
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        return parentFile.absolutePath
    }

    private fun buildPageUrl(page: Int): String {
        return if (page <= 1) {
            HOME_URL
        } else {
            "${HOME_URL}page/$page"
        }
    }

    private fun String.toAbsoluteUrl(): String {
        val url = trim()
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> HOME_URL.trimEnd('/') + url
            url.isBlank() -> ""
            else -> HOME_URL + url
        }
    }
}

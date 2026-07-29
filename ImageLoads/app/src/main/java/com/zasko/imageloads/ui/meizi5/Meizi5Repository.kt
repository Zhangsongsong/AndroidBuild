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

    suspend fun getDetail(dataUseFrom: Int?, url: String): Meizi5DetailInfo {
        return if (dataUseFrom == DataUseFrom.PRIVATE_FILE.value) {
            getLocalDetail(url = url)
        } else {
            getNetworkDetail(url = url)
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
                    title = article.selectFirst(".entry-title a, h2 a, h2.entry-title")
                        ?.text()
                        ?.trim()
                        .orEmpty()
                        .ifBlank { img.attr("alt").trim() },
                ).apply {
                    LogComponent.printD(tag = TAG, message = "item:${this}")
                },
            )
        }

        return resultList
    }

    fun transformDetail(url: String, data: String): Meizi5DetailInfo {
        val doc = MJson.parse(data)
        val title = doc.selectFirst("h1.entry-title")?.text()?.trim().orEmpty()
        val date = doc.selectFirst(".entry-header .entry-meta .entry-date")?.text()?.trim().orEmpty()
        val tags = doc.select(".post-tags a").map { it.text().trim() }.filter { it.isNotBlank() }
        val pictures = doc.select("div.entry.themeform img[src]").mapNotNull { img ->
            val src = img.attr("src").toAbsoluteUrl()
            if (src.isBlank()) {
                null
            } else {
                ImageLoadsInfo(
                    url = src,
                    width = img.attr("width").trim().toIntOrNull() ?: DEFAULT_IMAGE_WIDTH,
                    height = img.attr("height").trim().toIntOrNull() ?: DEFAULT_IMAGE_HEIGHT,
                )
            }
        }
        return Meizi5DetailInfo(
            url = url,
            title = title,
            date = date,
            tags = tags,
            pictures = pictures,
        )
    }

    private suspend fun getLocalImages(page: Int): List<ImageLoadsInfo> {
        val localImages = withContext(Dispatchers.IO) {
            getLocalDataFromCache(page = page)
        }
        return localImages.takeIf { it.isNotEmpty() } ?: getNetworkImages(page = page)
    }

    private suspend fun getNetworkImages(page: Int): List<ImageLoadsInfo> {
        val html = ComposeHttpComponent.getMeizi5(url = buildPageUrl(page = page))
        return withContext(Dispatchers.IO) {
            transformAndSaveNetworkData(page = page, html = html)
        }
    }

    private suspend fun getLocalDetail(url: String): Meizi5DetailInfo {
        val localDetail = withContext(Dispatchers.IO) {
            val file = File(getDetailHtmlDir(), url.toDetailCacheFileName())
            if (file.exists()) {
                transformDetail(url = url, data = FileUtil.getFileToHtml(file)?.toString() ?: "")
            } else {
                null
            }
        }
        return localDetail?.takeIf { it.pictures.isNotEmpty() } ?: getNetworkDetail(url = url)
    }

    private suspend fun getNetworkDetail(url: String): Meizi5DetailInfo {
        val html = ComposeHttpComponent.getMeizi5(url = url)
        return withContext(Dispatchers.IO) {
            saveDetailHtml(url = url, html = html)
            transformDetail(url = url, data = html)
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

    private fun getDetailHtmlDir(): String {
        val parentFile = File(getParentHtmlDir(), "detail")
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        return parentFile.absolutePath
    }

    private fun saveDetailHtml(url: String, html: String) {
        val file = File(getDetailHtmlDir(), url.toDetailCacheFileName())
        if (file.exists()) {
            file.delete()
        }
        file.writeText(html, Charsets.UTF_8)
        LogComponent.printD(tag = TAG, message = "saveDetailHtml url:${url} html:${html.length}")
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

    private fun String.toDetailCacheFileName(): String {
        val name = substringAfterLast('/').substringBefore('?').trim()
        return name.ifBlank { hashCode().toString() }
    }
}

data class Meizi5DetailInfo(
    val url: String = "",
    val title: String = "",
    val date: String = "",
    val tags: List<String> = emptyList(),
    val pictures: List<ImageLoadsInfo> = emptyList(),
)

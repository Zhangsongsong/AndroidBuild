package com.zasko.imageloads.ui.taotu

import com.zasko.imageloads.components.ComposeHttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.MJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object TaoTuRepository {

    private const val TAG = "TaoTuRepository"
    private const val HOME_URL = "https://taotu.org/"
    private const val DEFAULT_IMAGE_WIDTH = 320
    private const val DEFAULT_IMAGE_HEIGHT = 480

    suspend fun getImages(dataUseFrom: Int?, page: Int?): TaoTuPageResult {
        return if (dataUseFrom == DataUseFrom.PRIVATE_FILE.value) {
            getLocalImages(page = page)
        } else {
            getNetworkImages(page = page)
        }
    }

    suspend fun getDetail(dataUseFrom: Int?, url: String): TaoTuDetailInfo {
        return if (dataUseFrom == DataUseFrom.PRIVATE_FILE.value) {
            getLocalDetail(url = url)
        } else {
            getNetworkDetail(url = url)
        }
    }

    fun transformHome(data: String): TaoTuPageResult {
        val doc = MJson.parse(data)
        val resultList = mutableListOf<ImageLoadsInfo>()
        doc.select("#MainContent_piclist > div").forEach { item ->
            val link = item.selectFirst("a[href]") ?: return@forEach
            val img = link.selectFirst("img[src]") ?: return@forEach
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
        return TaoTuPageResult(
            images = resultList,
            nextPage = doc.selectFirst(".page-nav .page-number")
                ?.text()
                ?.toNextPage(),
        )
    }

    fun transformDetail(url: String, data: String): TaoTuDetailInfo {
        val doc = MJson.parse(data)
        val title = doc.selectFirst("#MainContent_suit_title h1")
            ?.text()
            ?.trim()
            .orEmpty()
        val tags = doc.select("#MainContent_info a")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val pictures = doc.select("#MainContent_piclist a[data-fancybox][href]").mapNotNull { link ->
            val imageUrl = link.attr("href").toAbsoluteUrl()
            val img = link.selectFirst("img[src]")
            if (imageUrl.isBlank()) {
                null
            } else {
                ImageLoadsInfo(
                    url = imageUrl,
                    href = img?.attr("src")?.toAbsoluteUrl().orEmpty(),
                    width = img?.attr("width")?.trim()?.toIntOrNull() ?: DEFAULT_IMAGE_WIDTH,
                    height = img?.attr("height")?.trim()?.toIntOrNull() ?: DEFAULT_IMAGE_HEIGHT,
                )
            }
        }
        return TaoTuDetailInfo(
            url = url,
            title = title,
            tags = tags,
            pictures = pictures,
        )
    }

    private suspend fun getNetworkImages(page: Int?): TaoTuPageResult {
        val html = ComposeHttpComponent.getTaoTu(url = buildPageUrl(page = page))
        return withContext(Dispatchers.IO) {
            saveHomeHtml(page = page, html = html)
            transformHome(data = html)
        }
    }

    private suspend fun getLocalImages(page: Int?): TaoTuPageResult {
        return withContext(Dispatchers.IO) {
            val file = File(getParentHtmlDir(), page.toCacheFileName())
            if (file.exists()) {
                transformHome(data = FileUtil.getFileToHtml(file)?.toString() ?: "")
            } else {
                TaoTuPageResult()
            }
        }
    }

    private suspend fun getNetworkDetail(url: String): TaoTuDetailInfo {
        val html = ComposeHttpComponent.getTaoTu(url = url)
        return withContext(Dispatchers.IO) {
            saveDetailHtml(url = url, html = html)
            transformDetail(url = url, data = html)
        }
    }

    private suspend fun getLocalDetail(url: String): TaoTuDetailInfo {
        return withContext(Dispatchers.IO) {
            val file = File(getDetailHtmlDir(), url.toDetailCacheFileName())
            if (file.exists()) {
                transformDetail(url = url, data = FileUtil.getFileToHtml(file)?.toString() ?: "")
            } else {
                TaoTuDetailInfo(url = url)
            }
        }
    }

    private fun saveHomeHtml(page: Int?, html: String) {
        val file = File(getParentHtmlDir(), page.toCacheFileName())
        if (file.exists()) {
            file.delete()
        }
        file.writeText(html, Charsets.UTF_8)
        LogComponent.printD(tag = TAG, message = "saveHomeHtml html:${html.length}")
    }

    private fun getParentHtmlDir(): String {
        val parentFile = File(FileUtil.getPrivateHtmlDir() + "/${FileUtil.NAME_TAOTU}")
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

    private fun buildPageUrl(page: Int?): String {
        return if (page == null) {
            HOME_URL
        } else {
            "${HOME_URL}page-$page.html"
        }
    }

    private fun Int?.toCacheFileName(): String {
        return this?.toString() ?: "home"
    }

    private fun String.toNextPage(): Int? {
        val currentPage = Regex("""(\d+)\s*/\s*(\d+)""")
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        return currentPage
            ?.minus(1)
            ?.takeIf { it > 0 }
    }

    private fun String.toAbsoluteUrl(): String {
        val url = trim()
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> HOME_URL.trimEnd('/') + url
            url.startsWith("./") -> HOME_URL + url.removePrefix("./")
            url.isBlank() -> ""
            else -> HOME_URL + url
        }
    }

    private fun String.toDetailCacheFileName(): String {
        val name = trim()
            .trimEnd('/')
            .substringAfterLast('/')
            .substringBefore('?')
            .trim()
        return name.ifBlank { hashCode().toString() }
    }
}

data class TaoTuPageResult(
    val images: List<ImageLoadsInfo> = emptyList(),
    val nextPage: Int? = null,
)

data class TaoTuDetailInfo(
    val url: String = "",
    val title: String = "",
    val tags: List<String> = emptyList(),
    val pictures: List<ImageLoadsInfo> = emptyList(),
)

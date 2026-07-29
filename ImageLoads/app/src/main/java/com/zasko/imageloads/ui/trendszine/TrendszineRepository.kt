package com.zasko.imageloads.ui.trendszine

import com.zasko.imageloads.components.ComposeHttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.MJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Element
import java.io.File
import kotlin.math.absoluteValue

object TrendszineRepository {

    private const val TAG = "TrendszineRepository"
    private const val HOME_URL = "https://trendszine.com/"
    private const val DEFAULT_IMAGE_WIDTH = 400
    private const val DEFAULT_IMAGE_HEIGHT = 600

    val allCategory = TrendszineCategory(title = "全部", url = HOME_URL)

    suspend fun getImages(dataUseFrom: Int?, category: TrendszineCategory, page: Int): TrendszinePageResult {
        return if (dataUseFrom == DataUseFrom.PRIVATE_FILE.value) {
            getLocalImages(category = category, page = page)
        } else {
            getNetworkImages(category = category, page = page)
        }
    }

    suspend fun getDetail(dataUseFrom: Int?, url: String): TrendszineDetailInfo {
        return if (dataUseFrom == DataUseFrom.PRIVATE_FILE.value) {
            getLocalDetail(url = url)
        } else {
            getNetworkDetail(url = url)
        }
    }

    fun transformHome(data: String, page: Int): TrendszinePageResult {
        val doc = MJson.parse(data)
        val resultList = mutableListOf<ImageLoadsInfo>()
        doc.select("main article").forEach { article ->
            val link = article.selectFirst(".post-image a[href]")
                ?: article.selectFirst("a[href]:has(img)")
                ?: return@forEach
            val img = link.selectFirst("img[src], img[data-src], img[data-lazy-src]") ?: return@forEach
            val src = img.imageSrc().toAbsoluteUrl()
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
        return TrendszinePageResult(
            images = resultList,
            categories = transformCategories(data = data),
            nextPage = if (hasNextPage(doc = doc)) page + 1 else null,
        )
    }

    fun transformCategories(data: String): List<TrendszineCategory> {
        val doc = MJson.parse(data)
        val categories = buildList {
            add(allCategory)
            doc.select("#mega-menu-primary > li").forEach { item ->
                val topLink = item.children()
                    .firstOrNull { it.tagName() == "a" && it.hasClass("mega-menu-link") && it.hasAttr("href") }
                    ?: return@forEach
                val topCategory = topLink.toCategoryOrNull() ?: return@forEach
                val children = item.select("ul.mega-sub-menu a.mega-menu-link[href]")
                    .mapNotNull { it.toCategoryOrNull() }
                    .distinctBy { it.url }
                add(topCategory.copy(children = children))
            }
        }
        return categories.distinctBy { it.url }
    }

    fun transformDetail(url: String, data: String): TrendszineDetailInfo {
        val doc = MJson.parse(data)
        val title = doc.selectFirst("h1.entry-title")?.text()?.trim().orEmpty()
        val date = doc.selectFirst("time.entry-date")?.text()?.trim().orEmpty()
        val tags = doc.select(".cat-links a, .tags-links a")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val content = doc.selectFirst(".entry-content") ?: doc.selectFirst("article")
        val pictures = content
            ?.select("img[src], img[data-src], img[data-lazy-src]")
            ?.mapNotNull { img ->
                val src = img.imageSrc().toAbsoluteUrl()
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
            ?.distinctBy { it.url }
            .orEmpty()
        return TrendszineDetailInfo(
            url = url,
            title = title,
            date = date,
            tags = tags,
            pictures = pictures,
        )
    }

    private suspend fun getNetworkImages(category: TrendszineCategory, page: Int): TrendszinePageResult {
        val html = ComposeHttpComponent.getTrendszine(url = buildPageUrl(category = category, page = page))
        return withContext(Dispatchers.IO) {
            savePageHtml(category = category, page = page, html = html)
            transformHome(data = html, page = page)
        }
    }

    private suspend fun getLocalImages(category: TrendszineCategory, page: Int): TrendszinePageResult {
        return withContext(Dispatchers.IO) {
            val file = File(getParentHtmlDir(), category.toCacheDirName()).let { dir ->
                File(dir, page.toString())
            }
            if (file.exists()) {
                transformHome(data = FileUtil.getFileToHtml(file)?.toString() ?: "", page = page)
            } else {
                TrendszinePageResult(categories = listOf(allCategory))
            }
        }
    }

    private suspend fun getNetworkDetail(url: String): TrendszineDetailInfo {
        val html = ComposeHttpComponent.getTrendszine(url = url)
        return withContext(Dispatchers.IO) {
            saveDetailHtml(url = url, html = html)
            transformDetail(url = url, data = html)
        }
    }

    private suspend fun getLocalDetail(url: String): TrendszineDetailInfo {
        return withContext(Dispatchers.IO) {
            val file = File(getDetailHtmlDir(), url.toDetailCacheFileName())
            if (file.exists()) {
                transformDetail(url = url, data = FileUtil.getFileToHtml(file)?.toString() ?: "")
            } else {
                TrendszineDetailInfo(url = url)
            }
        }
    }

    private fun savePageHtml(category: TrendszineCategory, page: Int, html: String) {
        val dir = File(getParentHtmlDir(), category.toCacheDirName())
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, page.toString())
        if (file.exists()) {
            file.delete()
        }
        file.writeText(html, Charsets.UTF_8)
        LogComponent.printD(tag = TAG, message = "savePageHtml category:${category.title} page:$page html:${html.length}")
    }

    private fun saveDetailHtml(url: String, html: String) {
        val file = File(getDetailHtmlDir(), url.toDetailCacheFileName())
        if (file.exists()) {
            file.delete()
        }
        file.writeText(html, Charsets.UTF_8)
        LogComponent.printD(tag = TAG, message = "saveDetailHtml url:${url} html:${html.length}")
    }

    private fun getParentHtmlDir(): String {
        val parentFile = File(FileUtil.getPrivateHtmlDir() + "/${FileUtil.NAME_TRENDSZINE}")
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

    private fun buildPageUrl(category: TrendszineCategory, page: Int): String {
        val baseUrl = category.url.ifBlank { HOME_URL }.trimEnd('/')
        return if (page <= 1) {
            category.url.ifBlank { HOME_URL }
        } else {
            "$baseUrl/page/$page"
        }
    }

    private fun hasNextPage(doc: org.jsoup.nodes.Document): Boolean {
        return doc.selectFirst("a.next.page-numbers[href], #nav-below .nav-previous a[href]") != null
    }

    private fun Element.imageSrc(): String {
        return attr("src").ifBlank { attr("data-src") }.ifBlank { attr("data-lazy-src") }
    }

    private fun Element.toCategoryOrNull(): TrendszineCategory? {
        val rawHref = attr("href").trim()
        if (rawHref.isBlank() || rawHref == "#" || rawHref.startsWith("javascript:", ignoreCase = true)) {
            return null
        }
        val title = text().trim()
        val href = rawHref.toAbsoluteUrl()
        return if (title.isNotBlank() && href.startsWith(HOME_URL) && href != HOME_URL) {
            TrendszineCategory(title = title, url = href)
        } else {
            null
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
        val name = trim()
            .trimEnd('/')
            .substringAfterLast('/')
            .substringBefore('?')
            .trim()
        return name.ifBlank { hashCode().absoluteValue.toString() }
    }

    private fun TrendszineCategory.toCacheDirName(): String {
        return if (url == HOME_URL) {
            "home"
        } else {
            url.trimEnd('/')
                .substringAfter(HOME_URL)
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .ifBlank { url.hashCode().absoluteValue.toString() }
        }
    }
}

data class TrendszineCategory(
    val title: String = "",
    val url: String = "",
    val children: List<TrendszineCategory> = emptyList(),
)

data class TrendszinePageResult(
    val images: List<ImageLoadsInfo> = emptyList(),
    val categories: List<TrendszineCategory> = emptyList(),
    val nextPage: Int? = null,
)

data class TrendszineDetailInfo(
    val url: String = "",
    val title: String = "",
    val date: String = "",
    val tags: List<String> = emptyList(),
    val pictures: List<ImageLoadsInfo> = emptyList(),
)

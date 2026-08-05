package com.zasko.imageloads.ui.generic

import com.zasko.imageloads.components.HttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.common.CommonImageDetailInfo
import com.zasko.imageloads.ui.common.DynamicSourceConfig
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.MJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Element
import java.io.File

data class GenericSourcePageResult(
    val images: List<ImageLoadsInfo> = emptyList(),
    val nextPage: Int? = null,
)

object GenericSourceRepository {

    private const val TAG = "GenericSourceRepository"
    private const val DEFAULT_IMAGE_WIDTH = 400
    private const val DEFAULT_IMAGE_HEIGHT = 600
    private const val MAX_DETAIL_PAGE_COUNT = 50
    private const val OLD_TRENDSZINE_DETAIL_CONTENT_SELECTOR = ".entry-content, article"

    private val defaultClient by lazy { HttpComponent.createHeaderAwareClient() }
    private val compatibleClient by lazy { HttpComponent.createCompatibleHeaderAwareClient() }

    suspend fun getImages(config: DynamicSourceConfig, dataUseFrom: Int?, page: Int): GenericSourcePageResult {
        return if (dataUseFrom == DataUseFrom.PRIVATE_FILE.value) {
            getLocalImages(config = config, page = page)
        } else {
            getNetworkImages(config = config, page = page)
        }
    }

    suspend fun getDetail(config: DynamicSourceConfig, dataUseFrom: Int?, url: String): CommonImageDetailInfo {
        return if (dataUseFrom == DataUseFrom.PRIVATE_FILE.value) {
            getLocalDetail(config = config, url = url)
        } else {
            getNetworkDetail(config = config, url = url)
        }
    }

    suspend fun requestRemainingDetailPages(
        config: DynamicSourceConfig,
        dataUseFrom: Int?,
        detailInfo: CommonImageDetailInfo,
    ): CommonImageDetailInfo {
        var currentDetail = detailInfo
        val visitedUrls = mutableSetOf<String>().apply {
            currentDetail.url.trim().takeIf { it.isNotBlank() }?.let(::add)
        }
        var nextUrl = currentDetail.nextPageUrl.trim()
        var loadCount = 0
        while (nextUrl.isNotBlank() && loadCount < MAX_DETAIL_PAGE_COUNT && visitedUrls.add(nextUrl)) {
            val nextDetail = try {
                getDetail(config = config, dataUseFrom = dataUseFrom, url = nextUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                LogComponent.printE(
                    tag = TAG,
                    message = "request remaining detail page failed:${config.key} $nextUrl $throwable",
                )
                return currentDetail.copy(nextPageUrl = "")
            }
            currentDetail = currentDetail.mergePage(nextDetail)
            nextUrl = currentDetail.nextPageUrl.trim()
            loadCount += 1
        }
        return currentDetail
    }

    private suspend fun getNetworkImages(config: DynamicSourceConfig, page: Int): GenericSourcePageResult {
        val url = config.buildPageUrl(page = page)
        val html = requestHtml(url = url)
        return withContext(Dispatchers.IO) {
            saveListHtml(config = config, page = page, html = html)
            transformHome(config = config, data = html, page = page)
        }
    }

    private suspend fun getLocalImages(config: DynamicSourceConfig, page: Int): GenericSourcePageResult {
        val localResult = withContext(Dispatchers.IO) {
            val file = File(getListHtmlDir(config = config), page.toString())
            if (file.exists()) {
                transformHome(config = config, data = FileUtil.getFileToHtml(file)?.toString().orEmpty(), page = page)
            } else {
                GenericSourcePageResult()
            }
        }
        return localResult.takeIf { it.images.isNotEmpty() } ?: getNetworkImages(config = config, page = page)
    }

    private suspend fun getNetworkDetail(config: DynamicSourceConfig, url: String): CommonImageDetailInfo {
        val html = requestHtml(url = url)
        return withContext(Dispatchers.IO) {
            saveDetailHtml(config = config, url = url, html = html)
            transformDetail(config = config, url = url, data = html)
        }
    }

    private suspend fun getLocalDetail(config: DynamicSourceConfig, url: String): CommonImageDetailInfo {
        val localDetail = withContext(Dispatchers.IO) {
            val file = File(getDetailHtmlDir(config = config), url.toCacheFileName())
            if (file.exists()) {
                transformDetail(config = config, url = url, data = FileUtil.getFileToHtml(file)?.toString().orEmpty())
            } else {
                null
            }
        }
        return localDetail?.takeIf { it.pictures.isNotEmpty() } ?: getNetworkDetail(config = config, url = url)
    }

    fun transformHome(config: DynamicSourceConfig, data: String, page: Int): GenericSourcePageResult {
        val doc = MJson.parse(data)
        val listConfig = config.processMethods.optJSONObject("list") ?: JSONObject()
        val parse = listConfig.optJSONObject("parse") ?: JSONObject()
        val itemSelector = parse.optString("itemSelector")
        val resultList = mutableListOf<ImageLoadsInfo>()

        doc.selectOrEmpty(itemSelector).forEach { item ->
            val link = item.firstSelected(parse.optString("detailLinkSelector")) ?: item
            val image = item.firstSelected(parse.optString("coverSelector")) ?: return@forEach
            val src = image.firstAttr(parse.optJSONArray("coverAttrOrder"), defaultAttrs = listOf("src", "data-src", "data-lazy-src"))
                .toAbsoluteUrl(config = config)
            if (src.isBlank()) {
                return@forEach
            }
            resultList.add(
                ImageLoadsInfo(
                    fromType = config.type,
                    url = src,
                    width = image.attr("width").trim().toIntOrNull() ?: DEFAULT_IMAGE_WIDTH,
                    height = image.attr("height").trim().toIntOrNull() ?: DEFAULT_IMAGE_HEIGHT,
                    href = link.attr("href").toAbsoluteUrl(config = config),
                    title = item.extractTextValue(
                        selector = parse.optString("titleSelector"),
                        attrOrder = parse.optJSONArray("titleAttrOrder"),
                    ).ifBlank { image.attr("alt").trim() },
                ),
            )
        }
        return GenericSourcePageResult(
            images = resultList.distinctBy { it.url },
            nextPage = if (hasNextPage(doc = doc, listConfig = listConfig, images = resultList, page = page)) page + 1 else null,
        )
    }

    fun transformDetail(config: DynamicSourceConfig, url: String, data: String): CommonImageDetailInfo {
        val doc = MJson.parse(data)
        val detailConfig = config.processMethods.optJSONObject("detail") ?: JSONObject()
        val parse = detailConfig.optJSONObject("parse") ?: JSONObject()
        val title = doc.selectFirstOrNull(parse.optString("titleSelector"))?.text()?.trim().orEmpty()
        val subtitles = buildList {
            doc.selectFirstOrNull(parse.optString("dateSelector"))?.text()?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
            val tags = doc.selectOrEmpty(parse.optString("tagSelector"))
                .map { it.text().trim() }
                .filter { it.isNotBlank() }
                .distinct()
            if (tags.isNotEmpty()) {
                add("标签: ${tags.joinToString(" / ")}")
            }
        }
        val content = doc.selectFirstContent(parse.optString("contentSelector")) ?: doc
        val images = mutableListOf<ImageLoadsInfo>()

        val imageLinkSelector = parse.optString("imageLinkSelector")
        if (imageLinkSelector.isNotBlank()) {
            val imageAttr = parse.optString("imageAttr").ifBlank { "href" }
            content.selectOrEmpty(imageLinkSelector).forEach { link ->
                val imageUrl = link.attrBySpec(imageAttr).toAbsoluteUrl(config = config)
                if (imageUrl.isNotBlank()) {
                    images.add(ImageLoadsInfo(fromType = config.type, url = imageUrl))
                }
            }
        }

        val imageSelector = parse.optString("imageSelector")
        if (imageSelector.isNotBlank()) {
            content.selectOrEmpty(imageSelector).forEach { image ->
                val imageUrl = image.firstAttr(parse.optJSONArray("imageAttrOrder"), defaultAttrs = listOf("src", "data-src", "data-lazy-src"))
                    .toAbsoluteUrl(config = config)
                if (imageUrl.isNotBlank()) {
                    images.add(
                        ImageLoadsInfo(
                            fromType = config.type,
                            url = imageUrl,
                            width = image.attr("width").trim().toIntOrNull() ?: DEFAULT_IMAGE_WIDTH,
                            height = image.attr("height").trim().toIntOrNull() ?: DEFAULT_IMAGE_HEIGHT,
                        ),
                    )
                }
            }
        }

        return CommonImageDetailInfo(
            url = url,
            title = title,
            subtitles = subtitles,
            pictures = images.distinctBy { it.url },
            nextPageUrl = findNextDetailPageUrl(config = config, currentUrl = url, data = data),
        )
    }

    private fun findNextDetailPageUrl(config: DynamicSourceConfig, currentUrl: String, data: String): String {
        val doc = MJson.parse(data)
        val pagination = config.processMethods.optJSONObject("detail")
            ?.optJSONObject("pagination")
            ?: return ""
        if (pagination.optString("nextPageValue").equals("none", ignoreCase = true)) {
            return ""
        }
        val containerSelector = pagination.optString("containerSelector")
        val container = if (containerSelector.isBlank()) {
            doc
        } else {
            doc.selectFirstOrNull(containerSelector) ?: return ""
        }
        val links = container.selectOrEmpty(pagination.optString("linkSelector").ifBlank { "a[href]" })
        if (links.isEmpty()) {
            return ""
        }
        val currentPage = currentUrl.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: 1
        val nextLink = links.mapNotNull { link ->
            val page = link.text().trim().toIntOrNull()
                ?: link.attr("href").trimEnd('/').substringAfterLast('/').toIntOrNull()
            val href = link.attr("href").toAbsoluteUrl(config = config)
            if (page != null && page > currentPage && href.isNotBlank()) {
                page to href
            } else {
                null
            }
        }.minByOrNull { it.first }?.second
        if (nextLink != null) {
            return nextLink
        }
        return if (currentPage <= links.size) {
            buildDetailPageUrl(currentUrl = currentUrl, nextPage = currentPage + 1)
        } else {
            ""
        }
    }

    private fun hasNextPage(
        doc: org.jsoup.nodes.Document,
        listConfig: JSONObject,
        images: List<ImageLoadsInfo>,
        page: Int,
    ): Boolean {
        val pagination = listConfig.optJSONObject("pagination") ?: JSONObject()
        val selector = pagination.optString("nextPageSelector")
        if (selector.isNotBlank()) {
            return doc.selectFirstOrNull(selector) != null
        }
        return images.isNotEmpty() && page < 10000 && !pagination.optString("nextPageValue").equals("none", ignoreCase = true)
    }

    private fun DynamicSourceConfig.buildPageUrl(page: Int): String {
        val listConfig = processMethods.optJSONObject("list") ?: JSONObject()
        val pageUrl = listConfig.optJSONObject("pageUrl") ?: JSONObject()
        val firstPage = pageUrl.optString("firstPage")
            .ifBlank { listConfig.optJSONObject("request")?.optString("homeUrl").orEmpty() }
            .ifBlank { baseUrl }
        val nextPage = pageUrl.optString("nextPage").ifBlank { firstPage }
        return (if (page <= 1) firstPage else nextPage)
            .replace("{page}", page.toString())
            .replace("{baseUrl}", baseUrl.trimEnd('/'))
            .replace("{homeUrl}", baseUrl)
            .replace("{categoryUrl}", baseUrl.trimEnd('/'))
    }

    private suspend fun requestHtml(url: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).get().build()
            try {
                defaultClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code} ${response.message}")
                    }
                    response.body?.string().orEmpty()
                }
            } catch (throwable: Throwable) {
                if (!HttpComponent.isTlsConnectionReset(throwable)) {
                    throw throwable
                }
                compatibleClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("HTTP ${response.code} ${response.message}")
                    }
                    response.body?.string().orEmpty()
                }
            }
        }
    }

    private fun saveListHtml(config: DynamicSourceConfig, page: Int, html: String) {
        File(getListHtmlDir(config = config), page.toString()).writeText(html, Charsets.UTF_8)
        LogComponent.printD(tag = TAG, message = "saveListHtml source:${config.key} page:$page html:${html.length}")
    }

    private fun saveDetailHtml(config: DynamicSourceConfig, url: String, html: String) {
        File(getDetailHtmlDir(config = config), url.toCacheFileName()).writeText(html, Charsets.UTF_8)
        LogComponent.printD(tag = TAG, message = "saveDetailHtml source:${config.key} url:$url html:${html.length}")
    }

    private fun getListHtmlDir(config: DynamicSourceConfig): File {
        return File(FileUtil.getPrivateHtmlDir(), "${config.key}/list").also { it.mkdirs() }
    }

    private fun getDetailHtmlDir(config: DynamicSourceConfig): File {
        return File(FileUtil.getPrivateHtmlDir(), "${config.key}/detail").also { it.mkdirs() }
    }

    private fun String.toCacheFileName(): String {
        return trim()
            .substringBefore('?')
            .trimEnd('/')
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { hashCode().toString() }
    }

    private fun Element.firstSelected(selector: String): Element? {
        return selector.takeIf { it.isNotBlank() }?.let { selectFirst(it) }
    }

    private fun org.jsoup.nodes.Element.selectFirstContent(selector: String): Element? {
        return if (selector.trim() == OLD_TRENDSZINE_DETAIL_CONTENT_SELECTOR) {
            selectFirst(".entry-content") ?: selectFirst("article")
        } else {
            selectFirstOrNull(selector)
        }
    }

    private fun org.jsoup.nodes.Element.selectFirstOrNull(selector: String): Element? {
        return selector.takeIf { it.isNotBlank() }?.let { selectFirst(it) }
    }

    private fun org.jsoup.nodes.Element.selectOrEmpty(selector: String): List<Element> {
        return selector.takeIf { it.isNotBlank() }?.let { select(it) }.orEmpty()
    }

    private fun Element.extractTextValue(selector: String, attrOrder: JSONArray?): String {
        selector.takeIf { it.isNotBlank() }?.let { css ->
            selectFirst(css)?.text()?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        attrOrder?.toStringList().orEmpty().forEach { attrSpec ->
            attrBySpec(attrSpec).trim().takeIf { it.isNotBlank() }?.let { return it }
        }
        return ""
    }

    private fun Element.firstAttr(attrOrder: JSONArray?, defaultAttrs: List<String>): String {
        (attrOrder?.toStringList().orEmpty().ifEmpty { defaultAttrs }).forEach { attrSpec ->
            attrBySpec(attrSpec).trim().takeIf { it.isNotBlank() }?.let { return it }
        }
        return ""
    }

    private fun Element.attrBySpec(attrSpec: String): String {
        val spec = attrSpec.trim()
        if (spec.isBlank()) {
            return ""
        }
        if ("." in spec && !spec.startsWith("data-")) {
            val selector = spec.substringBeforeLast('.')
            val attrName = spec.substringAfterLast('.')
            val target = if (selector.equals(tagName(), ignoreCase = true)) this else selectFirst(selector)
            return target?.attr(attrName).orEmpty()
        }
        return attr(spec)
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun String.toAbsoluteUrl(config: DynamicSourceConfig): String {
        val url = trim()
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> config.baseUrl.trimEnd('/') + url
            url.isBlank() -> ""
            else -> config.baseUrl.trimEnd('/') + "/" + url.removePrefix("/")
        }
    }

    private fun buildDetailPageUrl(currentUrl: String, nextPage: Int): String {
        val trimmedUrl = currentUrl.trimEnd('/')
        val currentSegment = trimmedUrl.substringAfterLast('/')
        return if (currentSegment.toIntOrNull() != null) {
            trimmedUrl.substringBeforeLast('/') + "/$nextPage"
        } else {
            "$trimmedUrl/$nextPage"
        }
    }

    private fun CommonImageDetailInfo.mergePage(pageInfo: CommonImageDetailInfo): CommonImageDetailInfo {
        return copy(
            title = title.ifBlank { pageInfo.title },
            subtitles = if (subtitles.isEmpty()) pageInfo.subtitles else subtitles,
            pictures = (pictures + pageInfo.pictures).distinctBy { it.url },
            nextPageUrl = pageInfo.nextPageUrl,
        )
    }
}

package com.zasko.imageloads.ui.common

import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.utils.Constants
import org.json.JSONArray
import org.json.JSONObject

object SourceProcessMethodStore {

    private const val KEY_PARSER = "parser"
    private const val KEY_LIST = "list"
    private const val KEY_DETAIL = "detail"
    private const val TRENDSZINE_DETAIL_TAG_SELECTOR = ".cat-links a, .tags-links a"
    private const val TRENDSZINE_DETAIL_CONTENT_SELECTOR = ".entry-content"
    private const val OLD_TRENDSZINE_DETAIL_CONTENT_SELECTOR = ".entry-content, article"

    fun getOrCacheMethods(sourceType: Int): JSONObject {
        val cachedMethods = SourceLocalDataStore.getProcessMethods(sourceType = sourceType)
        if (cachedMethods != null) {
            return migrateCachedMethods(sourceType = sourceType, methods = cachedMethods)
        }
        val methods = createDefaultMethods(sourceType = sourceType)
        SourceLocalDataStore.saveProcessMethods(sourceType = sourceType, methods = methods)
        return methods
    }

    fun resetDefaultMethods(sourceType: Int): JSONObject {
        val methods = createDefaultMethods(sourceType = sourceType)
        SourceLocalDataStore.saveProcessMethods(sourceType = sourceType, methods = methods)
        return methods
    }

    fun saveMethods(sourceType: Int, methods: JSONObject) {
        SourceLocalDataStore.saveProcessMethods(sourceType = sourceType, methods = methods)
    }

    private fun migrateCachedMethods(sourceType: Int, methods: JSONObject): JSONObject {
        if (sourceType != Constants.THEME_TYPE_TRENDSZINE) {
            return methods
        }
        val parse = methods.optJSONObject(KEY_DETAIL)?.optJSONObject("parse") ?: return methods
        var changed = false
        if (parse.optString("contentSelector").trim() != OLD_TRENDSZINE_DETAIL_CONTENT_SELECTOR) {
            if (parse.optString("contentSelector").trim().isBlank()) {
                parse.put("contentSelector", TRENDSZINE_DETAIL_CONTENT_SELECTOR)
                changed = true
            }
        } else {
            parse.put("contentSelector", TRENDSZINE_DETAIL_CONTENT_SELECTOR)
            changed = true
        }
        if (parse.optString("tagSelector").trim().isBlank()) {
            parse.put("tagSelector", TRENDSZINE_DETAIL_TAG_SELECTOR)
            changed = true
        }
        if (changed) {
            SourceLocalDataStore.saveProcessMethods(sourceType = sourceType, methods = methods)
        }
        return methods
    }

    private fun createDefaultMethods(sourceType: Int): JSONObject {
        return JSONObject()
            .put(KEY_PARSER, createParserMethod())
            .put(KEY_LIST, createListMethod(sourceType = sourceType))
            .put(KEY_DETAIL, createDetailMethod(sourceType = sourceType))
    }

    private fun createParserMethod(): JSONObject {
        return JSONObject()
            .put("name", "MJson")
            .put("entry", "MJson.parse(html)")
            .put("engine", "Jsoup.parse(html)")
            .put("description", "把 HTML 字符串转换成 Jsoup Document，后续列表、分页、详情都基于 CSS selector 解析")
    }

    private fun createListMethod(sourceType: Int): JSONObject {
        return when (sourceType) {
            Constants.THEME_TYPE_TRENDSZINE -> JSONObject()
                .put("request", JSONObject().put("homeUrl", "https://trendszine.com/"))
                .put("pageUrl", JSONObject().put("firstPage", "{categoryUrl}").put("nextPage", "{categoryUrl}/page/{page}"))
                .put(
                    "parse",
                    JSONObject()
                        .put("itemSelector", "main article")
                        .put("detailLinkSelector", ".post-image a[href], a[href]:has(img)")
                        .put("coverSelector", "img[src], img[data-src], img[data-lazy-src]")
                        .put("coverAttrOrder", JSONArray(listOf("src", "data-src", "data-lazy-src")))
                        .put("titleSelector", "h2.entry-title a, .entry-title a, .entry-title")
                        .put("categorySelector", "#mega-menu-primary > li")
                        .put("categoryLinkSelector", "a.mega-menu-link[href]")
                        .put("childrenCategorySelector", "ul.mega-sub-menu a.mega-menu-link[href]"),
                )
                .put(
                    "pagination",
                    JSONObject()
                        .put("nextPageSelector", "a.next.page-numbers[href], #nav-below .nav-previous a[href]")
                        .put("nextPageValue", "currentPage + 1"),
                )
                .put("cache", JSONObject().put("pageDir", "privateHtml/trendszine/{categoryCacheDir}/{page}"))

            Constants.THEME_TYPE_MEIZI5 -> JSONObject()
                .put("request", JSONObject().put("homeUrl", "https://meizi5.com/"))
                .put("pageUrl", JSONObject().put("firstPage", "https://meizi5.com/").put("nextPage", "https://meizi5.com/page/{page}"))
                .put(
                    "parse",
                    JSONObject()
                        .put("itemSelector", "article.masonry-item")
                        .put("detailLinkSelector", "a.entry-thumbnail")
                        .put("coverSelector", "img.wp-post-image, img")
                        .put("coverAttrOrder", JSONArray(listOf("src")))
                        .put("titleSelector", ".entry-title a, h2 a, h2.entry-title"),
                )
                .put("pagination", JSONObject().put("nextPageValue", "manual page + 1"))
                .put("cache", JSONObject().put("pageDir", "privateHtml/meizi5/{page}"))

            Constants.THEME_TYPE_TAOTU -> JSONObject()
                .put("request", JSONObject().put("homeUrl", "https://taotu.org/"))
                .put("pageUrl", JSONObject().put("firstPage", "https://taotu.org/").put("nextPage", "https://taotu.org/page-{page}.html"))
                .put(
                    "parse",
                    JSONObject()
                        .put("itemSelector", "#MainContent_piclist > div")
                        .put("detailLinkSelector", "a[href]")
                        .put("coverSelector", "img[src]")
                        .put("coverAttrOrder", JSONArray(listOf("src")))
                        .put("titleAttrOrder", JSONArray(listOf("a.title", "img.alt", "a.text"))),
                )
                .put(
                    "pagination",
                    JSONObject()
                        .put("currentPageSelector", ".page-nav .page-number")
                        .put("currentPagePattern", "(\\d+)\\s*/\\s*(\\d+)")
                        .put("nextPageValue", "currentPage - 1 when > 0"),
                )
                .put("cache", JSONObject().put("pageDir", "privateHtml/taotu/{page ?: home}"))

            else -> JSONObject()
        }
    }

    private fun createDetailMethod(sourceType: Int): JSONObject {
        return when (sourceType) {
            Constants.THEME_TYPE_TRENDSZINE -> JSONObject()
                .put("request", JSONObject().put("detailUrl", "list.item.href"))
                .put(
                    "parse",
                    JSONObject()
                        .put("titleSelector", "h1.entry-title")
                        .put("dateSelector", "time.entry-date")
                        .put("tagSelector", ".cat-links a, .tags-links a")
                        .put("contentSelector", TRENDSZINE_DETAIL_CONTENT_SELECTOR)
                        .put("imageSelector", "img[src], img[data-src], img[data-lazy-src]")
                        .put("imageAttrOrder", JSONArray(listOf("src", "data-src", "data-lazy-src"))),
                )
                .put(
                    "pagination",
                    JSONObject()
                        .put("containerSelector", "div.page-links")
                        .put("linkSelector", "a[href]")
                        .put("nextPageValue", "next numeric page url, fallback currentUrl/{nextPage}"),
                )
                .put("cache", JSONObject().put("detailDir", "privateHtml/trendszine/detail/{detailCacheFileName}"))

            Constants.THEME_TYPE_MEIZI5 -> JSONObject()
                .put("request", JSONObject().put("detailUrl", "list.item.href"))
                .put(
                    "parse",
                    JSONObject()
                        .put("titleSelector", "h1.entry-title")
                        .put("dateSelector", ".entry-header .entry-meta .entry-date")
                        .put("tagSelector", ".post-tags a")
                        .put("imageSelector", "div.entry.themeform img[src]")
                        .put("imageAttrOrder", JSONArray(listOf("src"))),
                )
                .put("pagination", JSONObject().put("nextPageValue", "none"))
                .put("cache", JSONObject().put("detailDir", "privateHtml/meizi5/detail/{detailCacheFileName}"))

            Constants.THEME_TYPE_TAOTU -> JSONObject()
                .put("request", JSONObject().put("detailUrl", "list.item.href"))
                .put(
                    "parse",
                    JSONObject()
                        .put("titleSelector", "#MainContent_suit_title h1")
                        .put("tagSelector", "#MainContent_info a")
                        .put("imageLinkSelector", "#MainContent_piclist a[data-fancybox][href]")
                        .put("imageAttr", "a.href")
                        .put("thumbSelector", "img[src]")
                        .put("thumbAttr", "src"),
                )
                .put("pagination", JSONObject().put("nextPageValue", "none"))
                .put("cache", JSONObject().put("detailDir", "privateHtml/taotu/detail/{detailCacheFileName}"))

            else -> JSONObject()
        }
    }
}

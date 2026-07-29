package com.zasko.imageloads.components

import android.content.Context
import com.zasko.imageloads.MApplication
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import org.json.JSONArray
import org.json.JSONObject

data class HttpHeaderItem(
    val name: String = "",
    val value: String = "",
)

data class HttpHeaderTarget(
    val id: String,
    val title: String,
)

object HttpHeaderConfigStore {

    const val TARGET_COMMON = "common"
    const val TARGET_TRENDSZINE = "trendszine"
    const val TARGET_MEIZI5 = "meizi5"
    const val TARGET_TAOTU = "taotu"

    private const val TAG = "HttpHeaderConfigStore"
    private const val PREF_NAME = "http_header_config_store"
    private const val KEY_PREFIX = "headers_"

    val targets = listOf(
        HttpHeaderTarget(id = TARGET_COMMON, title = "公共"),
        HttpHeaderTarget(id = TARGET_TRENDSZINE, title = "Trendszine"),
        HttpHeaderTarget(id = TARGET_MEIZI5, title = "Meizi5"),
        HttpHeaderTarget(id = TARGET_TAOTU, title = "TaoTu"),
    )

    fun getTarget(id: String): HttpHeaderTarget {
        return targets.firstOrNull { it.id == id } ?: targets.first()
    }

    fun getHeaders(targetId: String): List<HttpHeaderItem> {
        val key = targetId.toPreferenceKey()
        val preferences = getPreferences()
        if (!preferences.contains(key)) {
            return getDefaultHeaders(targetId = targetId)
        }
        val rawData = preferences.getString(key, null).orEmpty()
        return parseHeaders(rawData = rawData).ifEmpty {
            emptyList()
        }
    }

    fun saveHeaders(targetId: String, headers: List<HttpHeaderItem>) {
        val array = JSONArray()
        headers.normalized().forEach { header ->
            array.put(
                JSONObject()
                    .put("name", header.name)
                    .put("value", header.value),
            )
        }
        getPreferences()
            .edit()
            .putString(targetId.toPreferenceKey(), array.toString())
            .apply()
    }

    fun resetHeaders(targetId: String) {
        getPreferences()
            .edit()
            .remove(targetId.toPreferenceKey())
            .apply()
    }

    fun createInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val mergedHeaders = getHeadersForHost(host = request.url.host)
            if (mergedHeaders.isEmpty()) {
                chain.proceed(request)
            } else {
                val builder = request.newBuilder()
                mergedHeaders.forEach { header ->
                    builder.header(header.name, header.value)
                }
                chain.proceed(builder.build())
            }
        }
    }

    fun getHeadersForUrl(url: String): List<HttpHeaderItem> {
        val host = runCatching {
            url.toHttpUrl().host
        }.getOrNull().orEmpty()
        return getHeadersForHost(host = host)
    }

    private fun getHeadersForHost(host: String): List<HttpHeaderItem> {
        val sourceTarget = host.toSourceTargetId() ?: return emptyList()
        return mergeHeaders(
            commonHeaders = getHeaders(targetId = TARGET_COMMON),
            sourceHeaders = getHeaders(targetId = sourceTarget),
        )
    }

    private fun mergeHeaders(
        commonHeaders: List<HttpHeaderItem>,
        sourceHeaders: List<HttpHeaderItem>,
    ): List<HttpHeaderItem> {
        val merged = linkedMapOf<String, HttpHeaderItem>()
        (commonHeaders + sourceHeaders).normalized().forEach { header ->
            val key = header.name.lowercase()
            if (merged.containsKey(key)) {
                merged.remove(key)
            }
            merged[key] = header
        }
        return merged.values.toList()
    }

    private fun parseHeaders(rawData: String): List<HttpHeaderItem> {
        return runCatching {
            val array = JSONArray(rawData)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val name = item.optString("name").trim()
                    val value = item.optString("value").trim()
                    if (name.isNotBlank() && value.isNotBlank()) {
                        add(HttpHeaderItem(name = name, value = value))
                    }
                }
            }.normalized()
        }.onFailure { throwable ->
            LogComponent.printE(tag = TAG, message = throwable.toString())
        }.getOrDefault(emptyList())
    }

    private fun getDefaultHeaders(targetId: String): List<HttpHeaderItem> {
        return when (targetId) {
            TARGET_COMMON -> listOf(
                HttpHeaderItem(
                    name = "User-Agent",
                    value = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                ),
                HttpHeaderItem(
                    name = "Accept",
                    value = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                ),
                HttpHeaderItem(
                    name = "Accept-Language",
                    value = "zh-CN,zh;q=0.9,en;q=0.8",
                ),
            )

            TARGET_TRENDSZINE -> listOf(
                HttpHeaderItem(name = "Referer", value = "https://trendszine.com/"),
            )

            TARGET_MEIZI5 -> listOf(
                HttpHeaderItem(name = "Referer", value = "https://meizi5.com/"),
            )

            TARGET_TAOTU -> listOf(
                HttpHeaderItem(name = "Referer", value = "https://taotu.org/"),
            )

            else -> emptyList()
        }
    }

    private fun List<HttpHeaderItem>.normalized(): List<HttpHeaderItem> {
        val headersByName = linkedMapOf<String, HttpHeaderItem>()
        forEach { item ->
            val header = item.copy(name = item.name.trim(), value = item.value.trim())
            if (header.name.isNotBlank() && header.value.isNotBlank()) {
                val key = header.name.lowercase()
                if (headersByName.containsKey(key)) {
                    headersByName.remove(key)
                }
                headersByName[key] = header
            }
        }
        return headersByName.values.toList()
    }

    private fun String.toSourceTargetId(): String? {
        val normalizedHost = trim().lowercase()
        return when {
            normalizedHost.endsWith("trendszine.com") -> TARGET_TRENDSZINE
            normalizedHost.endsWith("meizi5.com") -> TARGET_MEIZI5
            normalizedHost.endsWith("taotu.org") -> TARGET_TAOTU
            else -> null
        }
    }

    private fun String.toPreferenceKey(): String {
        return KEY_PREFIX + this
    }

    private fun getPreferences() = MApplication.application.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE,
    )
}

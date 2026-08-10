package com.zasko.imageloads.components

import android.content.Context
import android.content.pm.ApplicationInfo
import com.zasko.imageloads.MApplication
import com.zasko.imageloads.utils.Constants
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
    private const val KEY_USE_COMMON_HEADERS = "use_common_headers"

    private val builtInTargets = listOf(
        HttpHeaderTarget(id = TARGET_COMMON, title = "公共"),
        HttpHeaderTarget(id = TARGET_TRENDSZINE, title = "Trendszine"),
        HttpHeaderTarget(id = TARGET_MEIZI5, title = "Meizi5"),
        HttpHeaderTarget(id = TARGET_TAOTU, title = "TaoTu"),
    )

    val targets: List<HttpHeaderTarget>
        get() {
            val builtInIds = builtInTargets.map { it.id }.toSet()
            val dynamicTargets = SourceLocalDataStore.getSourceJsonList()
                .mapNotNull { sourceJson ->
                    val id = sourceJson.optString("key").trim()
                    if (id.isBlank() || id in builtInIds) {
                        null
                    } else {
                        HttpHeaderTarget(
                            id = id,
                            title = sourceJson.optString("title").ifBlank { id },
                        )
                    }
                }
            return builtInTargets + dynamicTargets
        }

    fun getTarget(id: String): HttpHeaderTarget {
        return targets.firstOrNull { it.id == id } ?: targets.first()
    }

    fun getHeaders(targetId: String): List<HttpHeaderItem> {
        val key = targetId.toPreferenceKey()
        val preferences = getPreferences()
        if (preferences.contains(key)) {
            return parseHeaders(rawData = preferences.getString(key, null).orEmpty())
        }

        SourceLocalDataStore.getHeaders(targetId = targetId)?.let { legacyHeaders ->
            saveHeaders(targetId = targetId, headers = legacyHeaders)
            return legacyHeaders
        }

        return getDefaultHeaders(targetId = targetId)
    }

    fun saveHeaders(targetId: String, headers: List<HttpHeaderItem>) {
        getPreferences()
            .edit()
            .putString(targetId.toPreferenceKey(), headers.normalized().toHeaderJsonArray().toString())
            .apply()
    }

    fun resetHeaders(targetId: String) {
        SourceLocalDataStore.removeHeaders(targetId = targetId)
        getPreferences()
            .edit()
            .remove(targetId.toPreferenceKey())
            .apply()
    }

    fun removeTargetConfig(targetId: String) {
        SourceLocalDataStore.removeHeaders(targetId = targetId)
        getPreferences()
            .edit()
            .remove(targetId.toPreferenceKey())
            .remove(targetId.toCommonHeadersEnabledPreferenceKey())
            .apply()
    }

    fun isCommonHeadersEnabled(): Boolean {
        val preferences = getPreferences()
        if (preferences.contains(KEY_USE_COMMON_HEADERS)) {
            return preferences.getBoolean(KEY_USE_COMMON_HEADERS, true)
        }
        SourceLocalDataStore.getGlobalCommonHeadersEnabled()?.let { legacyEnabled ->
            setCommonHeadersEnabled(enabled = legacyEnabled)
            return legacyEnabled
        }
        return true
    }

    fun setCommonHeadersEnabled(enabled: Boolean) {
        getPreferences()
            .edit()
            .putBoolean(KEY_USE_COMMON_HEADERS, enabled)
            .apply()
    }

    fun isCommonHeadersEnabled(sourceType: Int): Boolean {
        return isCommonHeadersEnabled(targetId = sourceType.toHeaderTargetPreferenceId())
    }

    fun setCommonHeadersEnabled(sourceType: Int, enabled: Boolean) {
        setCommonHeadersEnabled(
            targetId = sourceType.toHeaderTargetPreferenceId(),
            enabled = enabled,
        )
    }

    fun getHeaderTargetId(sourceType: Int): String {
        return sourceType.toHeaderTargetPreferenceId()
    }

    fun isCommonHeadersEnabledForTarget(targetId: String): Boolean {
        return isCommonHeadersEnabled(targetId = targetId)
    }

    fun setCommonHeadersEnabledForTarget(targetId: String, enabled: Boolean) {
        setCommonHeadersEnabled(targetId = targetId, enabled = enabled)
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
            commonHeaders = if (isCommonHeadersEnabled(targetId = sourceTarget)) {
                getHeaders(targetId = TARGET_COMMON)
            } else {
                emptyList()
            },
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
            TARGET_COMMON -> if (isDebuggablePackage()) {
                listOf(
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
            } else {
                emptyList()
            }

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

    private fun List<HttpHeaderItem>.toHeaderJsonArray(): JSONArray {
        val array = JSONArray()
        forEach { header ->
            array.put(
                JSONObject()
                    .put("name", header.name)
                    .put("value", header.value),
            )
        }
        return array
    }

    private fun String.toSourceTargetId(): String? {
        val normalizedHost = trim().lowercase()
        return when {
            normalizedHost.endsWith("trendszine.com") -> TARGET_TRENDSZINE
            normalizedHost.endsWith("meizi5.com") -> TARGET_MEIZI5
            normalizedHost.endsWith("taotu.org") -> TARGET_TAOTU
            else -> SourceLocalDataStore.getTargetIdForHost(host = normalizedHost)
        }
    }

    private fun String.toPreferenceKey(): String {
        return KEY_PREFIX + this
    }

    private fun isCommonHeadersEnabled(targetId: String): Boolean {
        if (targetId == TARGET_COMMON) {
            return isCommonHeadersEnabled()
        }
        val preferences = getPreferences()
        val key = targetId.toCommonHeadersEnabledPreferenceKey()
        if (preferences.contains(key)) {
            return preferences.getBoolean(key, true)
        }
        SourceLocalDataStore.getSourceCommonHeadersEnabled(targetId = targetId)?.let { legacyEnabled ->
            setCommonHeadersEnabled(targetId = targetId, enabled = legacyEnabled)
            return legacyEnabled
        }
        return isCommonHeadersEnabled()
    }

    private fun setCommonHeadersEnabled(targetId: String, enabled: Boolean) {
        if (targetId == TARGET_COMMON) {
            setCommonHeadersEnabled(enabled = enabled)
            return
        }
        getPreferences()
            .edit()
            .putBoolean(targetId.toCommonHeadersEnabledPreferenceKey(), enabled)
            .apply()
    }

    private fun Int.toHeaderTargetPreferenceId(): String {
        return when (this) {
            Constants.THEME_TYPE_TRENDSZINE -> TARGET_TRENDSZINE
            Constants.THEME_TYPE_MEIZI5 -> TARGET_MEIZI5
            Constants.THEME_TYPE_TAOTU -> TARGET_TAOTU
            else -> "theme_$this"
        }
    }

    private fun String.toCommonHeadersEnabledPreferenceKey(): String {
        return "${KEY_USE_COMMON_HEADERS}_$this"
    }

    private fun isDebuggablePackage(): Boolean {
        return MApplication.application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun getPreferences() = MApplication.application.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE,
    )
}

package com.zasko.imageloads.components

import android.content.Context
import com.zasko.imageloads.MApplication
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.utils.Constants
import org.json.JSONArray
import org.json.JSONObject

object SourceLocalDataStore {

    private const val VERSION = 2
    private const val PREF_NAME = "source_local_data_store"
    private const val KEY_DATA = "data"
    private const val KEY_VERSION = "version"
    private const val KEY_COMMON = "common"
    private const val KEY_SOURCES = "sources"
    private const val KEY_KEY = "key"
    private const val KEY_TYPE = "type"
    private const val KEY_TITLE = "title"
    private const val KEY_COVER = "cover"
    private const val KEY_BASE_URL = "baseUrl"
    private const val KEY_HEADERS = "headers"
    private const val KEY_SETTINGS = "settings"
    private const val KEY_PROCESS_METHODS = "processMethods"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_USE_COMMON_HEADERS = "useCommonHeaders"
    private const val KEY_USE_LOCAL_DATA = "useLocalData"

    fun getHeaders(targetId: String): List<HttpHeaderItem>? {
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.optJSONArray(KEY_HEADERS)
            ?.toHeaderItems()
    }

    fun saveHeaders(targetId: String, headers: List<HttpHeaderItem>) {
        editRoot { root ->
            getTargetJson(root = root, targetId = targetId, createIfMissing = true)
                ?.put(KEY_HEADERS, headers.toHeaderJsonArray())
        }
    }

    fun removeHeaders(targetId: String) {
        editRoot { root ->
            getTargetJson(root = root, targetId = targetId, createIfMissing = false)
                ?.remove(KEY_HEADERS)
        }
    }

    fun getCover(sourceType: Int): String? {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        return getCover(targetId = targetId)
    }

    fun getCover(targetId: String): String? {
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.optString(KEY_COVER)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun saveCover(sourceType: Int, cover: String) {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        saveCover(targetId = targetId, cover = cover)
    }

    fun saveCover(targetId: String, cover: String) {
        val normalizedCover = cover.trim()
        if (normalizedCover.isBlank()) {
            return
        }
        editRoot { root ->
            getTargetJson(root = root, targetId = targetId, createIfMissing = true)
                ?.put(KEY_COVER, normalizedCover)
        }
    }

    fun getGlobalCommonHeadersEnabled(): Boolean? {
        return getTargetJson(targetId = HttpHeaderConfigStore.TARGET_COMMON, createIfMissing = false)
            ?.optJSONObject(KEY_SETTINGS)
            ?.takeIf { it.has(KEY_USE_COMMON_HEADERS) }
            ?.optBoolean(KEY_USE_COMMON_HEADERS)
    }

    fun setGlobalCommonHeadersEnabled(enabled: Boolean) {
        editRoot { root ->
            getTargetSettingsJson(
                root = root,
                targetId = HttpHeaderConfigStore.TARGET_COMMON,
                createIfMissing = true,
            )?.put(KEY_USE_COMMON_HEADERS, enabled)
        }
    }

    fun getSourceCommonHeadersEnabled(targetId: String): Boolean? {
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.optJSONObject(KEY_SETTINGS)
            ?.takeIf { it.has(KEY_USE_COMMON_HEADERS) }
            ?.optBoolean(KEY_USE_COMMON_HEADERS)
    }

    fun setSourceCommonHeadersEnabled(targetId: String, enabled: Boolean) {
        editRoot { root ->
            getTargetSettingsJson(root = root, targetId = targetId, createIfMissing = true)
                ?.put(KEY_USE_COMMON_HEADERS, enabled)
        }
    }

    fun getLocalDataEnabled(sourceType: Int): Boolean? {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        return getLocalDataEnabled(targetId = targetId)
    }

    fun getLocalDataEnabled(targetId: String): Boolean? {
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.optJSONObject(KEY_SETTINGS)
            ?.takeIf { it.has(KEY_USE_LOCAL_DATA) }
            ?.optBoolean(KEY_USE_LOCAL_DATA)
    }

    fun setLocalDataEnabled(sourceType: Int, enabled: Boolean) {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        setLocalDataEnabled(targetId = targetId, enabled = enabled)
    }

    fun setLocalDataEnabled(targetId: String, enabled: Boolean) {
        editRoot { root ->
            getTargetSettingsJson(root = root, targetId = targetId, createIfMissing = true)
                ?.put(KEY_USE_LOCAL_DATA, enabled)
        }
    }

    fun getFavorites(sourceType: Int): List<ImageLoadsInfo>? {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        return getFavorites(targetId = targetId, defaultSourceType = sourceType)
    }

    fun getFavorites(targetId: String, defaultSourceType: Int): List<ImageLoadsInfo>? {
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.optJSONArray(KEY_FAVORITES)
            ?.toFavorites(defaultSourceType = defaultSourceType)
    }

    fun replaceFavorites(sourceType: Int, favorites: List<ImageLoadsInfo>) {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        replaceFavorites(targetId = targetId, sourceType = sourceType, favorites = favorites)
    }

    fun replaceFavorites(targetId: String, sourceType: Int, favorites: List<ImageLoadsInfo>) {
        editRoot { root ->
            getTargetJson(root = root, targetId = targetId, createIfMissing = true)
                ?.put(KEY_FAVORITES, favorites.toFavoriteJsonArray(sourceType = sourceType))
        }
    }

    fun getProcessMethods(sourceType: Int): JSONObject? {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        return getProcessMethods(targetId = targetId)
    }

    fun getProcessMethods(targetId: String): JSONObject? {
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.optJSONObject(KEY_PROCESS_METHODS)
            ?.let { JSONObject(it.toString()) }
    }

    fun saveProcessMethods(sourceType: Int, methods: JSONObject) {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        saveProcessMethods(targetId = targetId, methods = methods)
    }

    fun saveProcessMethods(targetId: String, methods: JSONObject) {
        editRoot { root ->
            getTargetJson(root = root, targetId = targetId, createIfMissing = true)
                ?.put(KEY_PROCESS_METHODS, JSONObject(methods.toString()))
        }
    }

    fun getSourceJson(targetId: String): JSONObject? {
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.let { JSONObject(it.toString()) }
    }

    fun saveSourceJson(targetId: String, sourceJson: JSONObject) {
        editRoot { root ->
            val sources = root.optJSONObject(KEY_SOURCES)
                ?: JSONObject().also { root.put(KEY_SOURCES, it) }
            val normalizedTargetId = targetId.normalizeTargetId()
            val json = JSONObject(sourceJson.toString())
                .withoutHeaderConfig()
                .put(KEY_KEY, normalizedTargetId)
                .put(KEY_TYPE, sourceJson.optInt(KEY_TYPE, normalizedTargetId.toSourceType()))
                .put(KEY_TITLE, sourceJson.optString(KEY_TITLE).ifBlank { normalizedTargetId.toSourceTitle() })
            sources.put(normalizedTargetId, json)
        }
    }

    fun removeSourceJson(targetId: String) {
        editRoot { root ->
            root.optJSONObject(KEY_SOURCES)
                ?.remove(targetId.normalizeTargetId())
        }
    }

    fun getSourceJsonList(): List<JSONObject> {
        val sources = readRoot().optJSONObject(KEY_SOURCES) ?: return emptyList()
        return buildList {
            val keys = sources.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val source = sources.optJSONObject(key) ?: continue
                add(
                    JSONObject(source.toString())
                        .put(KEY_KEY, source.optString(KEY_KEY).ifBlank { key }),
                )
            }
        }
    }

    fun getTargetIdForHost(host: String): String? {
        val normalizedHost = host.trim().lowercase()
        if (normalizedHost.isBlank()) {
            return null
        }
        return getSourceJsonList().firstOrNull { source ->
            val baseHost = source.optString(KEY_BASE_URL)
                .toHostOrBlank()
            baseHost.isNotBlank() && normalizedHost.endsWith(baseHost)
        }?.optString(KEY_KEY)
    }

    private fun editRoot(block: (JSONObject) -> Unit) {
        val root = readRoot()
        block(root)
        saveRoot(root)
    }

    private fun getTargetJson(targetId: String, createIfMissing: Boolean): JSONObject? {
        return getTargetJson(root = readRoot(), targetId = targetId, createIfMissing = createIfMissing)
    }

    private fun getTargetJson(root: JSONObject, targetId: String, createIfMissing: Boolean): JSONObject? {
        return if (targetId == HttpHeaderConfigStore.TARGET_COMMON) {
            root.optJSONObject(KEY_COMMON) ?: if (createIfMissing) {
                JSONObject().also { root.put(KEY_COMMON, it) }
            } else {
                null
            }
        } else {
            val sources = root.optJSONObject(KEY_SOURCES) ?: if (createIfMissing) {
                JSONObject().also { root.put(KEY_SOURCES, it) }
            } else {
                return null
            }
            val normalizedTargetId = targetId.normalizeTargetId()
            sources.optJSONObject(normalizedTargetId) ?: if (createIfMissing) {
                JSONObject()
                    .put(KEY_KEY, normalizedTargetId)
                    .put(KEY_TYPE, normalizedTargetId.toSourceType())
                    .put(KEY_TITLE, normalizedTargetId.toSourceTitle())
                    .also { sources.put(normalizedTargetId, it) }
            } else {
                null
            }
        }
    }

    private fun getTargetSettingsJson(root: JSONObject, targetId: String, createIfMissing: Boolean): JSONObject? {
        val targetJson = getTargetJson(root = root, targetId = targetId, createIfMissing = createIfMissing)
            ?: return null
        return targetJson.optJSONObject(KEY_SETTINGS) ?: if (createIfMissing) {
            JSONObject().also { targetJson.put(KEY_SETTINGS, it) }
        } else {
            null
        }
    }

    private fun readRoot(): JSONObject {
        val rawData = getPreferences().getString(KEY_DATA, null).orEmpty()
        return runCatching {
            if (rawData.isBlank()) {
                createEmptyRoot()
            } else {
                JSONObject(rawData)
            }
        }.getOrElse {
            createEmptyRoot()
        }.apply {
            if (!has(KEY_VERSION)) {
                put(KEY_VERSION, VERSION)
            }
            if (!has(KEY_SOURCES)) {
                put(KEY_SOURCES, JSONObject())
            }
        }
    }

    private fun saveRoot(root: JSONObject) {
        root.put(KEY_VERSION, VERSION)
        getPreferences()
            .edit()
            .putString(KEY_DATA, root.toString())
            .apply()
    }

    private fun createEmptyRoot(): JSONObject {
        return JSONObject()
            .put(KEY_VERSION, VERSION)
            .put(KEY_SOURCES, JSONObject())
    }

    private fun List<HttpHeaderItem>.toHeaderJsonArray(): JSONArray {
        val array = JSONArray()
        distinctBy { it.name.trim().lowercase() }.forEach { header ->
            val name = header.name.trim()
            val value = header.value.trim()
            if (name.isNotBlank() && value.isNotBlank()) {
                array.put(
                    JSONObject()
                        .put("name", name)
                        .put("value", value),
                )
            }
        }
        return array
    }

    private fun JSONArray.toHeaderItems(): List<HttpHeaderItem> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                val value = item.optString("value").trim()
                if (name.isNotBlank() && value.isNotBlank()) {
                    add(HttpHeaderItem(name = name, value = value))
                }
            }
        }.distinctBy { it.name.lowercase() }
    }

    private fun List<ImageLoadsInfo>.toFavoriteJsonArray(sourceType: Int): JSONArray {
        val array = JSONArray()
        distinctBy { it.url.trim() }.forEach { info ->
            val url = info.url.trim()
            if (url.isNotBlank()) {
                array.put(
                    JSONObject()
                        .put("fromType", sourceType)
                        .put("url", url)
                        .put("href", info.href)
                        .put("width", info.width)
                        .put("height", info.height)
                        .put("title", info.title),
                )
            }
        }
        return array
    }

    private fun JSONArray.toFavorites(defaultSourceType: Int): List<ImageLoadsInfo> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val url = item.optString("url").trim()
                if (url.isBlank()) {
                    continue
                }
                add(
                    ImageLoadsInfo(
                        fromType = item.optInt("fromType", defaultSourceType),
                        url = url,
                        href = item.optString("href"),
                        width = item.optInt("width", -1),
                        height = item.optInt("height", -1),
                        title = item.optString("title"),
                    ),
                )
            }
        }.distinctBy { it.url }
    }

    private fun String.toSourceType(): Int {
        return when (this) {
            HttpHeaderConfigStore.TARGET_TRENDSZINE -> Constants.THEME_TYPE_TRENDSZINE
            HttpHeaderConfigStore.TARGET_MEIZI5 -> Constants.THEME_TYPE_MEIZI5
            HttpHeaderConfigStore.TARGET_TAOTU -> Constants.THEME_TYPE_TAOTU
            else -> Constants.THEME_TYPE_DYNAMIC_START + hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) % 100000 }
        }
    }

    private fun String.toSourceTitle(): String {
        return when (this) {
            HttpHeaderConfigStore.TARGET_TRENDSZINE -> "Trendszine"
            HttpHeaderConfigStore.TARGET_MEIZI5 -> "Meizi5"
            HttpHeaderConfigStore.TARGET_TAOTU -> "TaoTu"
            else -> this
        }
    }

    private fun String.normalizeTargetId(): String {
        return trim().lowercase()
            .replace(Regex("""[^a-z0-9_-]+"""), "_")
            .trim('_')
            .ifBlank { "source" }
    }

    private fun String.toHostOrBlank(): String {
        val value = trim()
        if (value.isBlank()) {
            return ""
        }
        return runCatching {
            java.net.URI(value).host.orEmpty()
        }.getOrDefault("")
            .removePrefix("www.")
            .lowercase()
    }

    private fun JSONObject.withoutHeaderConfig(): JSONObject {
        remove(KEY_HEADERS)
        optJSONObject(KEY_SETTINGS)?.let { settings ->
            settings.remove(KEY_USE_COMMON_HEADERS)
            if (settings.length() == 0) {
                remove(KEY_SETTINGS)
            }
        }
        return this
    }

    private fun getPreferences() = MApplication.application.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE,
    )
}

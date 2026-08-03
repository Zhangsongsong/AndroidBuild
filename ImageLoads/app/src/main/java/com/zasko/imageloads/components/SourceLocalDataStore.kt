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
    private const val KEY_TYPE = "type"
    private const val KEY_TITLE = "title"
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
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.optJSONObject(KEY_SETTINGS)
            ?.takeIf { it.has(KEY_USE_LOCAL_DATA) }
            ?.optBoolean(KEY_USE_LOCAL_DATA)
    }

    fun setLocalDataEnabled(sourceType: Int, enabled: Boolean) {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        editRoot { root ->
            getTargetSettingsJson(root = root, targetId = targetId, createIfMissing = true)
                ?.put(KEY_USE_LOCAL_DATA, enabled)
        }
    }

    fun getFavorites(sourceType: Int): List<ImageLoadsInfo>? {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.optJSONArray(KEY_FAVORITES)
            ?.toFavorites(defaultSourceType = sourceType)
    }

    fun replaceFavorites(sourceType: Int, favorites: List<ImageLoadsInfo>) {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        editRoot { root ->
            getTargetJson(root = root, targetId = targetId, createIfMissing = true)
                ?.put(KEY_FAVORITES, favorites.toFavoriteJsonArray(sourceType = sourceType))
        }
    }

    fun getProcessMethods(sourceType: Int): JSONObject? {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        return getTargetJson(targetId = targetId, createIfMissing = false)
            ?.optJSONObject(KEY_PROCESS_METHODS)
            ?.let { JSONObject(it.toString()) }
    }

    fun saveProcessMethods(sourceType: Int, methods: JSONObject) {
        val targetId = HttpHeaderConfigStore.getHeaderTargetId(sourceType = sourceType)
        editRoot { root ->
            getTargetJson(root = root, targetId = targetId, createIfMissing = true)
                ?.put(KEY_PROCESS_METHODS, JSONObject(methods.toString()))
        }
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
            sources.optJSONObject(targetId) ?: if (createIfMissing) {
                JSONObject()
                    .put(KEY_TYPE, targetId.toSourceType())
                    .put(KEY_TITLE, targetId.toSourceTitle())
                    .also { sources.put(targetId, it) }
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
            else -> -1
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

    private fun getPreferences() = MApplication.application.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE,
    )
}

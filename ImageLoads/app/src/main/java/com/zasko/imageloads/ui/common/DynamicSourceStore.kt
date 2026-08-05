package com.zasko.imageloads.ui.common

import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.utils.Constants
import org.json.JSONObject
import kotlin.math.abs

data class DynamicSourceConfig(
    val key: String,
    val type: Int,
    val title: String,
    val cover: String,
    val baseUrl: String,
    val processMethods: JSONObject,
)

object DynamicSourceStore {

    private val builtInKeys = setOf("trendszine", "meizi5", "taotu", "xiuren")

    fun getDynamicThemes(): List<MainThemeSelectInfo> {
        return SourceLocalDataStore.getSourceJsonList()
            .mapNotNull { sourceJson ->
                val key = sourceJson.optString("key").normalizeSourceKey()
                if (key.isBlank() || sourceJson.optJSONObject("processMethods") == null) {
                    return@mapNotNull null
                }
                MainThemeSelectInfo(
                    cover = sourceJson.optString("cover"),
                    title = sourceJson.optString("title").ifBlank { key },
                    dataUseFrom = if (SourceListSettingsStore.isLocalDataEnabled(sourceKey = key)) {
                        DataUseFrom.PRIVATE_FILE.value
                    } else {
                        DataUseFrom.NETWORK.value
                    },
                    theme = sourceJson.optInt("type", key.toDynamicSourceType()),
                    sourceKey = key,
                    baseUrl = sourceJson.optString("baseUrl"),
                )
            }
    }

    fun getConfig(sourceKey: String): DynamicSourceConfig? {
        val key = sourceKey.normalizeSourceKey()
        val sourceJson = SourceLocalDataStore.getSourceJson(targetId = key) ?: return null
        val processMethods = sourceJson.optJSONObject("processMethods") ?: return null
        return DynamicSourceConfig(
            key = key,
            type = sourceJson.optInt("type", key.toDynamicSourceType()),
            title = sourceJson.optString("title").ifBlank { key },
            cover = sourceJson.optString("cover"),
            baseUrl = sourceJson.optString("baseUrl"),
            processMethods = JSONObject(processMethods.toString()),
        )
    }

    fun normalizeSourceJson(rawKey: String, sourceJson: JSONObject): Pair<String, JSONObject> {
        val key = sourceJson.optString("key")
            .ifBlank { rawKey }
            .normalizeSourceKey()
        val baseUrl = sourceJson.optString("baseUrl")
            .ifBlank {
                sourceJson.optJSONObject("processMethods")
                    ?.optJSONObject("list")
                    ?.optJSONObject("request")
                    ?.optString("homeUrl")
                    .orEmpty()
            }
        val title = sourceJson.optString("title")
            .ifBlank { key }
        val json = JSONObject(sourceJson.toString())
            .put("key", key)
            .put("type", sourceJson.optInt("type", key.toDynamicSourceType()))
            .put("title", title)
            .put("baseUrl", baseUrl)
        return key to json
    }

    fun isBuiltInKey(key: String): Boolean {
        return key.normalizeSourceKey() in builtInKeys
    }

    fun String.normalizeSourceKey(): String {
        return trim().lowercase()
            .replace(Regex("""[^a-z0-9_-]+"""), "_")
            .trim('_')
    }

    fun String.toDynamicSourceType(): Int {
        val hash = hashCode().let { if (it == Int.MIN_VALUE) 0 else abs(it) }
        return Constants.THEME_TYPE_DYNAMIC_START + hash % 100000
    }
}

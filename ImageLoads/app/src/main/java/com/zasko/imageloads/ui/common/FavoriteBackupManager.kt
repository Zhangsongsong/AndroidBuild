package com.zasko.imageloads.ui.common

import com.zasko.imageloads.components.HttpHeaderConfigStore
import com.zasko.imageloads.components.HttpHeaderItem
import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.meizi5.Meizi5FavoriteStore
import com.zasko.imageloads.ui.taotu.TaoTuFavoriteStore
import com.zasko.imageloads.ui.trendszine.TrendszineFavoriteStore
import com.zasko.imageloads.utils.Constants
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteBackupSource(
    val type: Int,
    val title: String,
    val key: String,
    val cover: String = "",
    val baseUrl: String = "",
)

data class FavoriteImportResult(
    val restoredSourceCount: Int,
    val restoredItemCount: Int,
    val restoredHeaderGroupCount: Int,
    val restoredSettingsCount: Int,
    val missingSources: List<String>,
)

object FavoriteBackupManager {

    private const val VERSION = 2
    private const val SOURCE_TYPE_ALL = 0
    private const val KEY_VERSION = "version"
    private const val KEY_EXPORTED_AT = "exportedAt"
    private const val KEY_COMMON = "common"
    private const val KEY_SOURCES = "sources"
    private const val KEY_TYPE = "type"
    private const val KEY_KEY = "key"
    private const val KEY_TITLE = "title"
    private const val KEY_COVER = "cover"
    private const val KEY_BASE_URL = "baseUrl"
    private const val KEY_HEADERS = "headers"
    private const val KEY_SETTINGS = "settings"
    private const val KEY_PROCESS_METHODS = "processMethods"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_USE_COMMON_HEADERS = "useCommonHeaders"
    private const val KEY_USE_LOCAL_DATA = "useLocalData"
    private const val KEY_LIST = "list"
    private const val KEY_DETAIL = "detail"
    private const val KEY_CACHE = "cache"

    private val allSource = FavoriteBackupSource(
        type = SOURCE_TYPE_ALL,
        title = "全部",
        key = "all",
    )

    private val supportedSources = listOf(
        FavoriteBackupSource(
            type = Constants.THEME_TYPE_TRENDSZINE,
            title = "Trendszine",
            key = "trendszine",
            cover = "https://trendszine.com/wp-content/uploads/2026/07/33603291310201.webp.webp",
            baseUrl = "https://trendszine.com/",
        ),
        FavoriteBackupSource(
            type = Constants.THEME_TYPE_MEIZI5,
            title = "Meizi5",
            key = "meizi5",
            cover = "https://meizi5.com/wp-content/uploads/2026/04/VOL_350_face.jpg",
            baseUrl = "https://meizi5.com/",
        ),
        FavoriteBackupSource(
            type = Constants.THEME_TYPE_TAOTU,
            title = "TaoTu",
            key = "taotu",
            cover = "https://res.taotu.org/hot-girls/%e5%b0%8f%e8%94%a1%e5%a4%b4%e5%96%b5%e5%96%b5%e5%96%b5/00069-%e9%bb%91%e4%b8%9d%e8%be%85%e5%af%bc%e5%91%98-29p/thumbnail/0020.jpg",
            baseUrl = "https://taotu.org/",
        ),
    )

    val sourceOptions: List<FavoriteBackupSource>
        get() = listOf(allSource) + supportedSources + getDynamicSources()

    fun createExportFileName(source: FavoriteBackupSource): String {
        return "imageloads_source_${source.key}.json"
    }

    fun createBackupJson(source: FavoriteBackupSource): String {
        val sources = JSONObject()
        val exportSources = if (source.isAll()) {
            supportedSources + getDynamicSources()
        } else {
            listOf(source)
        }
        exportSources.forEach { exportSource ->
            sources.put(
                exportSource.key,
                exportSource.toSourceJson(),
            )
        }
        return JSONObject()
            .put(KEY_VERSION, VERSION)
            .put(KEY_EXPORTED_AT, System.currentTimeMillis())
            .put(KEY_COMMON, createCommonJson())
            .put(KEY_SOURCES, sources)
            .toString(2)
    }

    fun importBackupJson(rawData: String): FavoriteImportResult {
        val sourcesJson = JSONObject(rawData).optJSONObject(KEY_SOURCES)
            ?: throw IllegalArgumentException("来源备份格式错误")
        val rawKeys = sourcesJson.keys().asSequence().toList()
        if (rawKeys.isEmpty()) {
            throw IllegalArgumentException("JSON 中没有可导入来源数据")
        }
        if (rawKeys.size > 1) {
            throw IllegalArgumentException("一次只能导入一个来源")
        }
        val rawKey = rawKeys.first()
        val source = sourceForRawKey(sourcesJson = sourcesJson, rawKey = rawKey)
        return importBackupJson(rawData = rawData, source = source)
    }

    fun importBackupJson(rawData: String, source: FavoriteBackupSource): FavoriteImportResult {
        val sourcesJson = JSONObject(rawData).optJSONObject(KEY_SOURCES)
            ?: throw IllegalArgumentException("来源备份格式错误")
        var restoredSourceCount = 0
        var restoredItemCount = 0
        var restoredHeaderGroupCount = 0
        var restoredSettingsCount = 0
        val missingSources = mutableListOf<String>()
        val importSources = createImportSources(sourcesJson = sourcesJson, selectedSource = source)
        val commonResult = importCommonJson(rawData = rawData)
        restoredHeaderGroupCount += commonResult.headerGroupCount
        restoredSettingsCount += commonResult.settingsCount

        importSources.forEach { importTarget ->
            val importSource = importTarget.source
            val sourceNode = sourcesJson.opt(importTarget.rawKey)
                ?: sourcesJson.opt(importSource.key)
            if (sourceNode == null) {
                missingSources.add(importSource.title)
                return@forEach
            }

            when (sourceNode) {
                is JSONArray -> {
                    val favorites = sourceNode.toFavorites(sourceType = importSource.type)
                    replaceFavorites(source = importSource, favorites = favorites)
                    restoredSourceCount += 1
                    restoredItemCount += favorites.size
                }

                is JSONObject -> {
                    val result = importSourceJson(source = importSource, sourceJson = sourceNode)
                    restoredSourceCount += 1
                    restoredItemCount += result.itemCount
                    restoredHeaderGroupCount += result.headerGroupCount
                    restoredSettingsCount += result.settingsCount
                }
            }
        }

        return FavoriteImportResult(
            restoredSourceCount = restoredSourceCount,
            restoredItemCount = restoredItemCount,
            restoredHeaderGroupCount = restoredHeaderGroupCount,
            restoredSettingsCount = restoredSettingsCount,
            missingSources = missingSources,
        )
    }

    private fun createImportSources(
        sourcesJson: JSONObject,
        selectedSource: FavoriteBackupSource,
    ): List<PendingImportSource> {
        val rawKeys = sourcesJson.keys().asSequence().toList()
        if (selectedSource.isAll()) {
            return rawKeys.map { rawKey ->
                PendingImportSource(
                    source = sourceForRawKey(sourcesJson = sourcesJson, rawKey = rawKey),
                    rawKey = rawKey,
                )
            }
        }

        rawKeys.firstOrNull { rawKey ->
            rawKey == selectedSource.key || rawKey.normalizeBackupSourceKey() == selectedSource.key
        }?.let { rawKey ->
            return listOf(
                PendingImportSource(
                    source = selectedSource,
                    rawKey = rawKey,
                ),
            )
        }

        if (rawKeys.size == 1) {
            val rawKey = rawKeys.first()
            return listOf(
                PendingImportSource(
                    source = sourcesJson.optJSONObject(rawKey)
                        ?.toBackupSource(rawKey = rawKey)
                        ?: selectedSource,
                    rawKey = rawKey,
                ),
            )
        }

        return listOf(PendingImportSource(source = selectedSource, rawKey = selectedSource.key))
    }

    private fun sourceForRawKey(sourcesJson: JSONObject, rawKey: String): FavoriteBackupSource {
        val normalizedKey = rawKey.normalizeBackupSourceKey()
        return sourceOptions.firstOrNull { it.key == rawKey || it.key == normalizedKey }
            ?: sourcesJson.optJSONObject(rawKey)?.toBackupSource(rawKey = rawKey)
            ?: JSONObject().toBackupSource(rawKey = rawKey)
    }

    private fun getFavorites(source: FavoriteBackupSource): List<ImageLoadsInfo> {
        return when (source.type) {
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineFavoriteStore.getFavorites()
            Constants.THEME_TYPE_MEIZI5 -> Meizi5FavoriteStore.getFavorites()
            Constants.THEME_TYPE_TAOTU -> TaoTuFavoriteStore.getFavorites()
            else -> SourceLocalDataStore.getFavorites(targetId = source.key, defaultSourceType = source.type).orEmpty()
        }
    }

    private fun replaceFavorites(source: FavoriteBackupSource, favorites: List<ImageLoadsInfo>) {
        when (source.type) {
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineFavoriteStore.replaceFavorites(favorites)
            Constants.THEME_TYPE_MEIZI5 -> Meizi5FavoriteStore.replaceFavorites(favorites)
            Constants.THEME_TYPE_TAOTU -> TaoTuFavoriteStore.replaceFavorites(favorites)
            else -> SourceLocalDataStore.replaceFavorites(
                targetId = source.key,
                sourceType = source.type,
                favorites = favorites,
            )
        }
    }

    private fun createCommonJson(): JSONObject {
        return JSONObject()
            .put(KEY_HEADERS, HttpHeaderConfigStore.getHeaders(HttpHeaderConfigStore.TARGET_COMMON).toHeaderJsonArray())
            .put(
                KEY_SETTINGS,
                JSONObject()
                    .put(KEY_USE_COMMON_HEADERS, HttpHeaderConfigStore.isCommonHeadersEnabled()),
            )
    }

    private fun FavoriteBackupSource.toSourceJson(): JSONObject {
        if (!DynamicSourceStore.isBuiltInKey(key)) {
            val sourceJson = SourceLocalDataStore.getSourceJson(targetId = key) ?: JSONObject()
            return sourceJson
                .put(KEY_KEY, key)
                .put(KEY_TYPE, type)
                .put(KEY_TITLE, title)
                .put(KEY_COVER, cover.ifBlank { sourceJson.optString(KEY_COVER) })
                .put(KEY_BASE_URL, baseUrl.ifBlank { sourceJson.optString(KEY_BASE_URL) })
                .put(
                    KEY_HEADERS,
                    HttpHeaderConfigStore.getHeaders(key).toHeaderJsonArray(),
                )
                .put(
                    KEY_SETTINGS,
                    JSONObject()
                        .put(KEY_USE_COMMON_HEADERS, HttpHeaderConfigStore.isCommonHeadersEnabledForTarget(key))
                        .put(KEY_USE_LOCAL_DATA, SourceListSettingsStore.isLocalDataEnabled(sourceKey = key)),
                )
                .put(
                    KEY_PROCESS_METHODS,
                    SourceLocalDataStore.getProcessMethods(targetId = key)
                        ?.withoutLocalCacheMethods()
                        ?: JSONObject(),
                )
                .put(KEY_FAVORITES, getFavorites(source = this).toJsonArray(sourceType = type))
        }
        return JSONObject()
            .put(KEY_KEY, key)
            .put(KEY_TYPE, type)
            .put(KEY_TITLE, title)
            .put(KEY_COVER, SourceLocalDataStore.getCover(sourceType = type) ?: cover)
            .put(KEY_BASE_URL, baseUrl)
            .put(
                KEY_HEADERS,
                HttpHeaderConfigStore.getHeaders(HttpHeaderConfigStore.getHeaderTargetId(sourceType = type))
                    .toHeaderJsonArray(),
            )
            .put(
                KEY_SETTINGS,
                JSONObject()
                    .put(KEY_USE_COMMON_HEADERS, HttpHeaderConfigStore.isCommonHeadersEnabled(sourceType = type))
                    .put(KEY_USE_LOCAL_DATA, SourceListSettingsStore.isLocalDataEnabled(sourceType = type)),
            )
            .put(
                KEY_PROCESS_METHODS,
                SourceProcessMethodStore.getOrCacheMethods(sourceType = type)
                    .withoutLocalCacheMethods(),
            )
            .put(KEY_FAVORITES, getFavorites(source = this).toJsonArray(sourceType = type))
    }

    private fun importCommonJson(rawData: String): SourceImportPartResult {
        val commonJson = JSONObject(rawData).optJSONObject(KEY_COMMON) ?: return SourceImportPartResult()
        var headerGroupCount = 0
        var settingsCount = 0
        commonJson.optJSONArray(KEY_HEADERS)?.let { headers ->
            HttpHeaderConfigStore.saveHeaders(
                targetId = HttpHeaderConfigStore.TARGET_COMMON,
                headers = headers.toHeaderItems(),
            )
            headerGroupCount += 1
        }
        commonJson.optJSONObject(KEY_SETTINGS)?.let { settings ->
            if (settings.has(KEY_USE_COMMON_HEADERS)) {
                HttpHeaderConfigStore.setCommonHeadersEnabled(
                    enabled = settings.optBoolean(KEY_USE_COMMON_HEADERS, true),
                )
                settingsCount += 1
            }
        }
        return SourceImportPartResult(
            headerGroupCount = headerGroupCount,
            settingsCount = settingsCount,
        )
    }

    private fun importSourceJson(
        source: FavoriteBackupSource,
        sourceJson: JSONObject,
    ): SourceImportPartResult {
        val normalizedSource = sourceJson.toBackupSource(rawKey = source.key)
        val targetId = normalizedSource.key
        var itemCount = 0
        var headerGroupCount = 0
        var settingsCount = 0
        SourceLocalDataStore.saveSourceJson(targetId = targetId, sourceJson = sourceJson)
        sourceJson.optJSONArray(KEY_HEADERS)?.let { headers ->
            HttpHeaderConfigStore.saveHeaders(
                targetId = if (DynamicSourceStore.isBuiltInKey(targetId)) {
                    HttpHeaderConfigStore.getHeaderTargetId(sourceType = normalizedSource.type)
                } else {
                    targetId
                },
                headers = headers.toHeaderItems(),
            )
            headerGroupCount += 1
        }
        sourceJson.optJSONObject(KEY_SETTINGS)?.let { settings ->
            if (settings.has(KEY_USE_COMMON_HEADERS)) {
                if (DynamicSourceStore.isBuiltInKey(targetId)) {
                    HttpHeaderConfigStore.setCommonHeadersEnabled(
                        sourceType = normalizedSource.type,
                        enabled = settings.optBoolean(KEY_USE_COMMON_HEADERS, true),
                    )
                } else {
                    HttpHeaderConfigStore.setCommonHeadersEnabledForTarget(
                        targetId = targetId,
                        enabled = settings.optBoolean(KEY_USE_COMMON_HEADERS, true),
                    )
                }
                settingsCount += 1
            }
            if (settings.has(KEY_USE_LOCAL_DATA)) {
                SourceListSettingsStore.setLocalDataEnabled(sourceKey = targetId, enabled = settings.optBoolean(KEY_USE_LOCAL_DATA, true))
                settingsCount += 1
            }
        }
        sourceJson.optJSONObject(KEY_PROCESS_METHODS)?.let { processMethods ->
            SourceLocalDataStore.saveProcessMethods(
                targetId = targetId,
                methods = processMethods.withoutLocalCacheMethods(),
            )
            settingsCount += 1
        }
        sourceJson.optJSONArray(KEY_FAVORITES)?.let { favoritesJson ->
            val favorites = favoritesJson.toFavorites(sourceType = normalizedSource.type)
            replaceFavorites(source = normalizedSource, favorites = favorites)
            itemCount = favorites.size
        }
        return SourceImportPartResult(
            itemCount = itemCount,
            headerGroupCount = headerGroupCount,
            settingsCount = settingsCount,
        )
    }

    private fun List<ImageLoadsInfo>.toJsonArray(sourceType: Int): JSONArray {
        val array = JSONArray()
        distinctBy { it.url }.forEach { info ->
            array.put(
                JSONObject()
                    .put("fromType", sourceType)
                    .put("url", info.url)
                    .put("href", info.href)
                    .put("width", info.width)
                    .put("height", info.height)
                    .put("title", info.title),
            )
        }
        return array
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

    private fun FavoriteBackupSource.isAll(): Boolean {
        return type == SOURCE_TYPE_ALL
    }

    private fun getDynamicSources(): List<FavoriteBackupSource> {
        return DynamicSourceStore.getDynamicThemes().map { theme ->
            FavoriteBackupSource(
                type = theme.theme,
                title = theme.title,
                key = theme.sourceKey,
                cover = theme.cover,
                baseUrl = theme.baseUrl,
            )
        }
    }

    private fun JSONObject.toBackupSource(rawKey: String): FavoriteBackupSource {
        val (key, json) = DynamicSourceStore.normalizeSourceJson(rawKey = rawKey, sourceJson = this)
        return FavoriteBackupSource(
            type = json.optInt(KEY_TYPE, key.toDynamicBackupSourceType()),
            title = json.optString(KEY_TITLE).ifBlank { key },
            key = key,
            cover = json.optString(KEY_COVER),
            baseUrl = json.optString(KEY_BASE_URL),
        )
    }

    private fun String.toDynamicBackupSourceType(): Int {
        return DynamicSourceStore.run { this@toDynamicBackupSourceType.toDynamicSourceType() }
    }

    private fun String.normalizeBackupSourceKey(): String {
        return DynamicSourceStore.run { this@normalizeBackupSourceKey.normalizeSourceKey() }
    }

    private fun JSONObject.withoutLocalCacheMethods(): JSONObject {
        return JSONObject(toString()).apply {
            optJSONObject(KEY_LIST)?.remove(KEY_CACHE)
            optJSONObject(KEY_DETAIL)?.remove(KEY_CACHE)
        }
    }

    private fun JSONArray.toFavorites(sourceType: Int): List<ImageLoadsInfo> {
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val url = item.optString("url").trim()
                if (url.isBlank()) {
                    continue
                }
                add(
                    ImageLoadsInfo(
                        fromType = item.optInt("fromType", sourceType),
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
        }
    }

    private data class SourceImportPartResult(
        val itemCount: Int = 0,
        val headerGroupCount: Int = 0,
        val settingsCount: Int = 0,
    )

    private data class PendingImportSource(
        val source: FavoriteBackupSource,
        val rawKey: String,
    )
}

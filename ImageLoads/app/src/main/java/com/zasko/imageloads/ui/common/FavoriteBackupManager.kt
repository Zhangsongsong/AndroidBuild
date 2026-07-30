package com.zasko.imageloads.ui.common

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
)

data class FavoriteImportResult(
    val restoredSourceCount: Int,
    val restoredItemCount: Int,
    val missingSources: List<String>,
)

object FavoriteBackupManager {

    private const val VERSION = 1
    private const val SOURCE_TYPE_ALL = 0
    private const val KEY_VERSION = "version"
    private const val KEY_EXPORTED_AT = "exportedAt"
    private const val KEY_SOURCES = "sources"

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
        ),
        FavoriteBackupSource(
            type = Constants.THEME_TYPE_MEIZI5,
            title = "Meizi5",
            key = "meizi5",
        ),
        FavoriteBackupSource(
            type = Constants.THEME_TYPE_TAOTU,
            title = "TaoTu",
            key = "taotu",
        ),
    )

    val sourceOptions: List<FavoriteBackupSource> = listOf(allSource) + supportedSources

    fun createExportFileName(source: FavoriteBackupSource): String {
        return "imageloads_favorites_${source.key}.json"
    }

    fun createBackupJson(source: FavoriteBackupSource): String {
        val sources = JSONObject()
        val exportSources = if (source.isAll()) {
            supportedSources
        } else {
            listOf(source)
        }
        exportSources.forEach { exportSource ->
            sources.put(
                exportSource.key,
                getFavorites(source = exportSource).toJsonArray(sourceType = exportSource.type),
            )
        }
        return JSONObject()
            .put(KEY_VERSION, VERSION)
            .put(KEY_EXPORTED_AT, System.currentTimeMillis())
            .put(KEY_SOURCES, sources)
            .toString(2)
    }

    fun importBackupJson(rawData: String, source: FavoriteBackupSource): FavoriteImportResult {
        val sourcesJson = JSONObject(rawData).optJSONObject(KEY_SOURCES)
            ?: throw IllegalArgumentException("收藏备份格式错误")
        var restoredSourceCount = 0
        var restoredItemCount = 0
        val missingSources = mutableListOf<String>()
        val importSources = if (source.isAll()) {
            supportedSources
        } else {
            listOf(source)
        }

        importSources.forEach { importSource ->
            val array = sourcesJson.optJSONArray(importSource.key)
            if (array == null) {
                missingSources.add(importSource.title)
            } else {
                val favorites = array.toFavorites(sourceType = importSource.type)
                replaceFavorites(source = importSource, favorites = favorites)
                restoredSourceCount += 1
                restoredItemCount += favorites.size
            }
        }

        return FavoriteImportResult(
            restoredSourceCount = restoredSourceCount,
            restoredItemCount = restoredItemCount,
            missingSources = missingSources,
        )
    }

    private fun getFavorites(source: FavoriteBackupSource): List<ImageLoadsInfo> {
        return when (source.type) {
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineFavoriteStore.getFavorites()
            Constants.THEME_TYPE_MEIZI5 -> Meizi5FavoriteStore.getFavorites()
            Constants.THEME_TYPE_TAOTU -> TaoTuFavoriteStore.getFavorites()
            else -> emptyList()
        }
    }

    private fun replaceFavorites(source: FavoriteBackupSource, favorites: List<ImageLoadsInfo>) {
        when (source.type) {
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineFavoriteStore.replaceFavorites(favorites)
            Constants.THEME_TYPE_MEIZI5 -> Meizi5FavoriteStore.replaceFavorites(favorites)
            Constants.THEME_TYPE_TAOTU -> TaoTuFavoriteStore.replaceFavorites(favorites)
        }
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

    private fun FavoriteBackupSource.isAll(): Boolean {
        return type == SOURCE_TYPE_ALL
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
                        fromType = sourceType,
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
}

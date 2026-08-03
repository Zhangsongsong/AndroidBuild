package com.zasko.imageloads.ui.taotu

import android.content.Context
import com.zasko.imageloads.MApplication
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.utils.Constants
import org.json.JSONArray

object TaoTuFavoriteStore {

    private const val TAG = "TaoTuFavoriteStore"
    private const val PREF_NAME = "taotu_favorite_store"
    private const val KEY_ITEMS = "items"

    fun getFavorites(): List<ImageLoadsInfo> {
        SourceLocalDataStore.getFavorites(sourceType = Constants.THEME_TYPE_TAOTU)?.let { favorites ->
            return favorites
        }
        val rawData = getPreferences().getString(KEY_ITEMS, null) ?: return emptyList()
        val favorites = runCatching {
            val array = JSONArray(rawData)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val url = item.optString("url").trim()
                    if (url.isBlank()) {
                        continue
                    }
                    add(
                        ImageLoadsInfo(
                            url = url,
                            href = item.optString("href"),
                            width = item.optInt("width", -1),
                            height = item.optInt("height", -1),
                            title = item.optString("title"),
                        ),
                    )
                }
            }.distinctBy { it.url }
        }.onFailure { throwable ->
            LogComponent.printE(tag = TAG, message = throwable.toString())
        }.getOrDefault(emptyList())
        SourceLocalDataStore.replaceFavorites(
            sourceType = Constants.THEME_TYPE_TAOTU,
            favorites = favorites,
        )
        return favorites
    }

    fun toggleFavorite(imageInfo: ImageLoadsInfo): Boolean {
        val url = imageInfo.url.trim()
        if (url.isBlank()) {
            return false
        }
        val currentList = getFavorites()
        val isFavorite = currentList.any { it.url == url }
        val newList = if (isFavorite) {
            currentList.filterNot { it.url == url }
        } else {
            listOf(imageInfo.copy(url = url)) + currentList.filterNot { it.url == url }
        }
        saveFavorites(newList)
        return !isFavorite
    }

    fun replaceFavorites(list: List<ImageLoadsInfo>) {
        saveFavorites(list)
    }

    private fun saveFavorites(list: List<ImageLoadsInfo>) {
        SourceLocalDataStore.replaceFavorites(
            sourceType = Constants.THEME_TYPE_TAOTU,
            favorites = list,
        )
    }

    private fun getPreferences() = MApplication.application.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE,
    )
}

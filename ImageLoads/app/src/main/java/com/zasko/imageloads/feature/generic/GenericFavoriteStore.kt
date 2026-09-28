package com.zasko.imageloads.feature.generic

import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.data.model.ImageLoadsInfo
import com.zasko.imageloads.core.ui.common.DynamicSourceConfig

object GenericFavoriteStore {

    fun getFavorites(config: DynamicSourceConfig): List<ImageLoadsInfo> {
        return SourceLocalDataStore.getFavorites(
            targetId = config.key,
            defaultSourceType = config.type,
        ).orEmpty()
    }

    fun replaceFavorites(config: DynamicSourceConfig, favorites: List<ImageLoadsInfo>) {
        SourceLocalDataStore.replaceFavorites(
            targetId = config.key,
            sourceType = config.type,
            favorites = favorites,
        )
    }

    fun toggleFavorite(config: DynamicSourceConfig, imageInfo: ImageLoadsInfo): Boolean {
        val key = imageInfo.url.trim()
        if (key.isBlank()) {
            return false
        }
        val current = getFavorites(config = config).toMutableList()
        val exists = current.any { it.url == key }
        val next = if (exists) {
            current.filterNot { it.url == key }
        } else {
            current + imageInfo.copy(fromType = config.type)
        }
        replaceFavorites(config = config, favorites = next)
        return !exists
    }
}

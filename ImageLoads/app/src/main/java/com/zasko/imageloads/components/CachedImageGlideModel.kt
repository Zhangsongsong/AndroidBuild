package com.zasko.imageloads.components

import android.util.LruCache
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders

object CachedImageGlideModel {
    private const val MAX_CACHE_SIZE = 512

    private val lock = Any()
    private val cache = LruCache<String, GlideUrl>(MAX_CACHE_SIZE)
    private var cacheVersion = Long.MIN_VALUE

    fun get(url: String): GlideUrl {
        val imageUrl = url.trim()
        synchronized(lock) {
            syncVersionLocked()
            cache.get(imageUrl)?.let { return it }
        }

        val model = GlideUrl(
            imageUrl,
            LazyHeaders.Builder().apply {
                HttpHeaderConfigStore.getHeadersForUrl(url = imageUrl).forEach { header ->
                    addHeader(header.name, header.value)
                }
            }.build(),
        )

        synchronized(lock) {
            syncVersionLocked()
            cache.get(imageUrl)?.let { return it }
            cache.put(imageUrl, model)
        }
        return model
    }

    private fun syncVersionLocked() {
        val version = HttpHeaderConfigStore.version
        if (cacheVersion != version) {
            cache.evictAll()
            cacheVersion = version
        }
    }
}

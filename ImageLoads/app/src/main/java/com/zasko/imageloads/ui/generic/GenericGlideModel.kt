package com.zasko.imageloads.ui.generic

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.zasko.imageloads.components.HttpHeaderConfigStore

fun String.toGenericImageModel(): GlideUrl {
    val imageUrl = trim()
    return GlideUrl(
        imageUrl,
        LazyHeaders.Builder().apply {
            HttpHeaderConfigStore.getHeadersForUrl(url = imageUrl).forEach { header ->
                addHeader(header.name, header.value)
            }
        }.build(),
    )
}

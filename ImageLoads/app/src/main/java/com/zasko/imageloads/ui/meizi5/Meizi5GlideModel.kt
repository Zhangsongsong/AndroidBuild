package com.zasko.imageloads.ui.meizi5

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.zasko.imageloads.components.HttpHeaderConfigStore

fun String.toMeizi5ImageModel(): GlideUrl {
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

package com.zasko.imageloads.ui.trendszine

import com.bumptech.glide.load.model.GlideUrl
import com.zasko.imageloads.components.CachedImageGlideModel

fun String.toTrendszineImageModel(): GlideUrl {
    return CachedImageGlideModel.get(url = this)
}

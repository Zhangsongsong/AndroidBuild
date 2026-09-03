package com.zasko.imageloads.ui.generic

import com.bumptech.glide.load.model.GlideUrl
import com.zasko.imageloads.components.CachedImageGlideModel

fun String.toGenericImageModel(): GlideUrl {
    return CachedImageGlideModel.get(url = this)
}

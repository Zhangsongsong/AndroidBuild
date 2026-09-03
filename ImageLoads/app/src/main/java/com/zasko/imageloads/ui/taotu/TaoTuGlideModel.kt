package com.zasko.imageloads.ui.taotu

import com.bumptech.glide.load.model.GlideUrl
import com.zasko.imageloads.components.CachedImageGlideModel

fun String.toTaoTuImageModel(): GlideUrl {
    return CachedImageGlideModel.get(url = this)
}

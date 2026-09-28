package com.zasko.imageloads.feature.meizi5

import com.bumptech.glide.load.model.GlideUrl
import com.zasko.imageloads.components.CachedImageGlideModel

fun String.toMeizi5ImageModel(): GlideUrl {
    return CachedImageGlideModel.get(url = this)
}

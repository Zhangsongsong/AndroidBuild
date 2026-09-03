package com.zasko.imageloads.ui.meizi5

import com.bumptech.glide.load.model.GlideUrl
import com.zasko.imageloads.components.CachedImageGlideModel

fun String.toMeizi5ImageModel(): GlideUrl {
    return CachedImageGlideModel.get(url = this)
}

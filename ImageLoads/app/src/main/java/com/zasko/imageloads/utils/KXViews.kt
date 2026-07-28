package com.zasko.imageloads.utils

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import com.bumptech.glide.Glide


fun ImageView.loadImage(url: String) {
    loadImageModel(url)
}

fun ImageView.loadImage(model: Any?) {
    loadImageModel(model)
}

private fun ImageView.loadImageModel(model: Any?) {
    Glide.with(this.context).load(model.trimIfString()).centerCrop().into(this)
}


fun ImageView.loadImageWithInside(url: String, placeId: Int = -1) {
    loadImageWithInsideModel(model = url, placeId = placeId)
}

fun ImageView.loadImageWithInside(model: Any?, placeId: Int = -1) {
    loadImageWithInsideModel(model = model, placeId = placeId)
}

private fun ImageView.loadImageWithInsideModel(model: Any?, placeId: Int = -1) {
    Glide.with(this.context).load(model.trimIfString()).centerInside().let {
        if (placeId != -1) it.placeholder(placeId)
        it
    }.into(this)
}

private fun Any?.trimIfString(): Any? {
    return if (this is String) trim() else this
}

fun ImageView.setTint(color: Int) {
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(color))
}

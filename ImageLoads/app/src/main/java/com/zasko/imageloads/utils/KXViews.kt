package com.zasko.imageloads.utils

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import com.bumptech.glide.Glide


fun ImageView.loadImage(url: String) {
    Glide.with(this.context).load(url).centerCrop().into(this)
}


fun ImageView.loadImageWithInside(url: String) {
    Glide.with(this.context).load(url).centerInside().into(this)
}

fun ImageView.setTint(color: Int) {
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(color))
}

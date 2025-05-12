package com.zasko.imageloads.utils

import android.widget.ImageView
import com.bumptech.glide.Glide


fun ImageView.loadImage(url: String) {
    Glide.with(this.context).load(url).centerCrop().into(this)
}


fun ImageView.loadImageWithInside(url: String) {
    Glide.with(this.context).load(url).centerInside().into(this)
}
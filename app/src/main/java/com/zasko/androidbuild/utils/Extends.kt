package com.zasko.androidbuild.utils

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide

class Extends {}


inline fun View.onClick(crossinline back: (v: View) -> Unit) {
    setOnClickListener {
        back.invoke(it)
    }
}

fun ImageView.loadImage(url: String) {
    Glide.with(this).load(url).centerCrop().into(this)
}
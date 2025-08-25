package com.zasko.androidbuild.utils

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
class Extends {}


inline fun View.onClick(crossinline back: (v: View) -> Unit) {
    setOnClickListener {
        back.invoke(it)
    }
}

fun ImageView.loadImage(url: String) {
    Glide.with(this).load(url).centerCrop().into(this)
}





fun <T : Any> Single<T>.switchThread(): Single<T> = this.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
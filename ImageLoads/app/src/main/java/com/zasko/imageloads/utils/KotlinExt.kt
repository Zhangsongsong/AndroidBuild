package com.zasko.imageloads.utils

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers


fun <T : Any> Single<T>.switchThread(): Single<T> = this.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())


inline val Dp.dpToPx: Float
    @Composable get() = with(LocalDensity.current) {
        this@dpToPx.toPx()
    }


object ViewClick {
    const val TIME = 300
    var hash: Int = 0
    var lastClickTime = 0L
}

fun View.onClick(callback: (View) -> Unit) {
    this.setOnClickListener {
        if (this.hashCode() != ViewClick.hash) {
            ViewClick.let {
                it.hash = this.hashCode()
                it.lastClickTime = System.currentTimeMillis()
            }
            callback.invoke(this)
        } else {
            val time = System.currentTimeMillis()
            if (time - ViewClick.lastClickTime > ViewClick.TIME) {
                ViewClick.lastClickTime = time
                callback.invoke(this)
            }
        }
    }
}


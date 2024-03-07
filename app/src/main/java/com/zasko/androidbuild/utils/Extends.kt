package com.zasko.androidbuild.utils

import android.view.View

class Extends {}


inline fun View.onClick(crossinline back: (v:View) -> Unit) {
    setOnClickListener {
        back.invoke(it)
    }
}
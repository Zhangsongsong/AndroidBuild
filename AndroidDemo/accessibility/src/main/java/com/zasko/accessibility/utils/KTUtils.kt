package com.zasko.accessibility.utils

import android.view.View

inline fun View.onClick(crossinline handler: (View?) -> Unit) {
    setOnClickListener(object : DebounceOnClickListener(callback = {
        handler.invoke(it)
    }) {})
}

abstract class DebounceOnClickListener(private val callback: (View?) -> Unit) : View.OnClickListener {

    companion object {
        const val DEBOUNCE_TIMEOUT = 500
    }

    private var lastTime = 0L
    override fun onClick(v: View?) {
        val time = System.currentTimeMillis()
        if (time - lastTime >= DEBOUNCE_TIMEOUT) {
            lastTime = time
            callback.invoke(v)
        }
    }
}
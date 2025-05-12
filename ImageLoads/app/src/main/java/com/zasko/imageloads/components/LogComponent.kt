package com.zasko.imageloads.components

import android.util.Log

object LogComponent {

    private const val TAG = "LogComponent"

    fun printD(tag: String = "", message: String = "") {
        Log.d(tag.ifEmpty { TAG }, message)
    }

    fun printE(tag: String = "", message: String = "") {
        Log.e(tag.ifEmpty { TAG }, message)
    }



}
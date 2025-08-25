package com.zasko.androidbuild.components

import android.util.Log

object LogComponent {

    private const val TAG = "LogComponent"


    private const val MAX_LENGTH = 3000

    fun printD(tag: String = "", message: String = "", isShowAll: Boolean = false) {

        if (isShowAll && message.length > 3000) {
            var index = 0
            while (index < message.length) {
                Log.d(tag.ifEmpty { TAG }, "MAX_LENGTH index:${index} length:${message.length}")
                if (index + MAX_LENGTH > message.length) {
                    Log.d(tag.ifEmpty { TAG }, message.substring(index))
                } else {
                    Log.d(tag.ifEmpty { TAG }, message.substring(index, MAX_LENGTH + index))
                }
                index += MAX_LENGTH
            }
        } else {
            Log.d(tag.ifEmpty { TAG }, message)
        }

    }

    fun printE(tag: String = "", message: String = "") {
        Log.e(tag.ifEmpty { TAG }, message)
    }

    fun test() {
        val string = "abcdefghi"
        var index = 0
        val max = 3
        while (index < string.length) {
            printD(tag = TAG, message = "test ${index} length:${string.length}")
            if (index + max > string.length) {
                printD(tag = TAG, message = "test ${string.substring(index)}")
            } else {
                printD(tag = TAG, message = "test ${string.substring(index, index + max)}")
            }
            index += max
        }
    }

}
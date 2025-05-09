package com.zasko.androidbuild.activity

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.ProgressBar
import java.lang.Exception
import java.lang.reflect.Modifier

class TestProgressbar : ProgressBar {

    companion object {
        const val TAG = "TestProgressbar"
    }

    @SuppressLint("SoonBlockedPrivateApi")
    constructor(context: Context) : super(context) {


//        try {
//            val field = ProgressBar::class.java.getDeclaredField("mDuration")
//            field.isAccessible = true
//            val duration = field.get(this) as Int
//            Log.d(TAG, "$duration")
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//
//        }
        val dds = ProgressBar::class.java.declaredFields
        Log.d(TAG, "constructor(context: Context) ${dds.size}")
        for (field in dds) {
            if (Modifier.isPrivate(field.modifiers)) {
                Log.d(TAG, "name-->${field.name}")
            }
        }
    }
}
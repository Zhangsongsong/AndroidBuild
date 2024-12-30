package com.zasko.accessibility.manager

import android.content.Context
import android.view.WindowManager
import com.zasko.accessibility.views.FloatWindowView

object FloatWindowManager {


    private var windowView: FloatWindowView? = null

    fun showFloatWindow(context: Context) {
        windowView = FloatWindowView(context)
        windowView?.let {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.addView(it, it.getWindowParam())
        }
    }

    fun hideFloatWindow(context: Context) {
        windowView?.let {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it)
        }
    }


}
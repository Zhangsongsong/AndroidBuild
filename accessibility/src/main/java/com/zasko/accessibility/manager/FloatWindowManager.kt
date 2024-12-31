package com.zasko.accessibility.manager

import android.content.Context
import android.view.WindowManager
import com.zasko.accessibility.service.DemoAccessibilityService
import com.zasko.accessibility.views.FloatWindowView

object FloatWindowManager {


    private var windowView: FloatWindowView? = null

    var isShowFloatWindow = false

    fun showFloatWindow(context: Context) {
        if (isShowFloatWindow) {
            return
        }
        isShowFloatWindow = true
        windowView = FloatWindowView(context)
        windowView?.let {
            it.setAccessService(accessService)
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.addView(it, it.getWindowParam())
        }
    }

    fun hideFloatWindow(context: Context) {
        windowView?.let {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it)
        }
        windowView = null
        isShowFloatWindow = false
    }


    private var accessService: DemoAccessibilityService? = null

    fun setAccessService(service: DemoAccessibilityService) {
        accessService = service
    }

    fun getFloatWindow(): FloatWindowView? {
        return windowView
    }


}
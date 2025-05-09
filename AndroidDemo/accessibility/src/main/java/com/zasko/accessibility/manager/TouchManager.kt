package com.zasko.accessibility.manager

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi


object TouchManager {

    private const val TAG = "TouchManager"

    @RequiresApi(Build.VERSION_CODES.N)
    fun scroll(accessibilityService: AccessibilityService) {

        // 获取DisplayMetrics来计算屏幕尺寸


        // 初始化手势轨迹
        val path = Path()

        // 起点位于屏幕中间的50%
        val startX = accessibilityService.resources.displayMetrics.widthPixels / 2
        val startY = accessibilityService.resources.displayMetrics.heightPixels / 2

        // 终点位于屏幕底部的50%
        val endX = accessibilityService.resources.displayMetrics.widthPixels / 2
        val endY = accessibilityService.resources.displayMetrics.heightPixels - 400 // 减去50像素以避免触摸系统UI元素


        Log.d(TAG, "scroll: startX:$startX startY:$startY endX:$endX endY:$endY")
        path.moveTo(startX.toFloat(), startY.toFloat())
        path.lineTo(endX.toFloat(), endY.toFloat())

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(path, 0, 800))
        val gestureDescription = gestureBuilder.build()


        // 执行滑动手势
        val isDispatched = accessibilityService.dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                // 手势完成时的回调
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                // 手势取消时的回调
            }
        }, null)
        Log.d(TAG, "scroll: isDispatched:${isDispatched}")
    }


}



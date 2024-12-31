package com.zasko.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.zasko.accessibility.manager.FloatWindowManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class DemoAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DemoAccessibilitySer"

    }


    /**
     *   用户手动到设置里启动无障碍服务，系统绑定服务后，会回调 onServiceConnected()
     *     可在此调用 setServiceInfo() 对服务进行配置调整
     */

    @SuppressLint("NewApi")
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected: ")

        FloatWindowManager.setAccessService(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind intent:${intent}")
        return super.onUnbind(intent)
    }

    /**
     *  接收到系统发送AccessibilityEvent时回调，如：顶部Notification，界面更新，内容变化等，我们可以筛选特定的事件类型，执行不同的响应
     *
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        Log.d(TAG, "onAccessibilityEvent: $event")
//        Log.d(TAG, "onAccessibilityEvent: ${AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED}")
        event?.let { e ->
            when (e.eventType) {
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    handleContentChange(event = e)
                }

                else -> {
                }
            }
        }

    }


    /**
     * 当用户在设置中手动关闭、杀死进程、或开发者调用 disableSelf() 时，服务会被关闭销毁。
     * 中断时候回调
     */
    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt: ")
        mDispose?.let {
            it.dispose()
        }
    }


    private val mHandler = Handler(Looper.getMainLooper())
    private var currentAccessibilityEvent: AccessibilityEvent? = null
    private fun handleContentChange(event: AccessibilityEvent) {
        currentAccessibilityEvent = event
    }

    private var mDispose: Disposable? = null
    private fun startLooper() {
        mDispose?.dispose()
        mDispose = null
        mDispose =
            Observable.interval(0, 3 * 1000L, TimeUnit.MILLISECONDS).observeOn(Schedulers.io()).subscribeOn(AndroidSchedulers.mainThread()).doOnNext {
                Log.d(TAG, "startLooper: doOnNext:${it}")
                mHandler.removeCallbacks(contentChangeRunnable)
                mHandler.post(contentChangeRunnable)
            }.subscribe({}, {})
    }


    private val contentChangeRunnable = Runnable {
        val tmpEvent = currentAccessibilityEvent
        if (tmpEvent == null) {
            touchLoadMore()
        }
        tmpEvent?.let { event ->
            event.source?.let {
                val nodeInfos = it.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/tv_desc")
                Log.d(TAG, "contentChangeRunnable: size:${nodeInfos?.size}")
                nodeInfos?.forEach { nodeInfo ->
                    Log.d(TAG, "contentChangeRunnable: nodeInfo:${nodeInfo}")
                }
                touchLoadMore()
            }
        }
    }


    private var isStartLoad = false
    private fun touchLoadMore() {
        isStartLoad = true
        val path = Path()

        val display = this.resources.displayMetrics
        // 起点位于屏幕中间的50%
        val startX = display.widthPixels / 2
        val startY = display.heightPixels / 2
        // 终点位置
        val endX = display.widthPixels / 2
        val endY = startY - 400

        Log.d(TAG, "touchLoadMore startX:$startX startY:$startY endX:$endX endY:$endY")

        path.moveTo(startX.toFloat(), startY.toFloat())
        path.lineTo(endX.toFloat(), endY.toFloat())
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(path, 0, 800))
        val gestureDescription = gestureBuilder.build()

        val isDispatched = dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                isStartLoad = false
            }
        }, null)
        Log.d(TAG, "touchLoadMore: isDispatched:${isDispatched}")
    }

}
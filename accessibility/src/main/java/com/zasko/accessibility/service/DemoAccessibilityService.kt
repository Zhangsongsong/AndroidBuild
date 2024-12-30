package com.zasko.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent

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
//        GlobalScope.launch(Dispatchers.Main) {
//            delay(5000)
//
//            TouchManager.scroll(accessibilityService = this@DemoAccessibilityService)
//        }

    }

    /**
     *  接收到系统发送AccessibilityEvent时回调，如：顶部Notification，界面更新，内容变化等，我们可以筛选特定的事件类型，执行不同的响应
     *
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        Log.d(TAG, "onAccessibilityEvent: $event")
    }


    /**
     * 当用户在设置中手动关闭、杀死进程、或开发者调用 disableSelf() 时，服务会被关闭销毁。
     * 中断时候回调
     */
    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt: ")
    }


}
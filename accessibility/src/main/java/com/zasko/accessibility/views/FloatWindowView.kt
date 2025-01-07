package com.zasko.accessibility.views

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Context
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import com.zasko.accessibility.R
import com.zasko.accessibility.databinding.FloatWindowLayoutBinding
import com.zasko.accessibility.service.DemoAccessibilityService
import com.zasko.accessibility.service.DemoAccessibilityService.Companion
import com.zasko.accessibility.utils.onClick
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class FloatWindowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), View.OnTouchListener {

    companion object {
        private const val TAG = "FloatWindowView"
    }

    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val layoutParams by lazy {
        WindowManager.LayoutParams().also {
            initLayoutParams(it)
        }
    }

    fun getWindowParam(): WindowManager.LayoutParams {
        return layoutParams
    }


    private fun initLayoutParams(params: WindowManager.LayoutParams) {
        params.type = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 -> {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            else -> {
                WindowManager.LayoutParams.TYPE_TOAST
            }
        }
        params.format = PixelFormat.RGBA_8888
        //设置悬浮窗不获取焦点的原因就是为了传递事件
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.gravity = Gravity.TOP or Gravity.START
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
    }

    private var binding: FloatWindowLayoutBinding

    init {
        val view = View.inflate(context, R.layout.float_window_layout, this)
        binding = FloatWindowLayoutBinding.bind(view)

        binding.startBtn.onClick {
            startChecking()
        }
        binding.endBtn.onClick {
            stopChecking()
        }

        setOnTouchListener(this)
    }


    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f
    private var lastX = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null || v == null) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastY = downY
                lastX = downX
            }

            MotionEvent.ACTION_MOVE -> {
                layoutParams.x += (event.rawX - lastX).toInt()
                layoutParams.y += (event.rawY - lastY).toInt()
                windowManager.updateViewLayout(this, layoutParams)
                lastX = event.rawX
                lastY = event.rawY
            }

            MotionEvent.ACTION_UP -> if (abs(event.rawX - downX) < touchSlop && abs(event.rawY - downY) < touchSlop) {
                v.performClick()
            }
        }
        return true
    }


    private var accessService: DemoAccessibilityService? = null

    fun setAccessService(service: DemoAccessibilityService?) {
        accessService = service
    }

    private fun isCanRun(): Boolean {
        return accessService != null
    }

    private fun startChecking() {
        if (!isCanRun()) {
            return
        }
        startLooper()
    }

    private fun stopChecking() {
        looperDispose?.dispose()
        removeCallbacks(changeRunnable)
    }

    private var looperDispose: Disposable? = null

    private fun startLooper() {
        looperDispose?.dispose()
        looperDispose = null
        looperDispose =
            Observable.interval(0, 3 * 1000L, TimeUnit.MILLISECONDS).observeOn(Schedulers.io()).subscribeOn(AndroidSchedulers.mainThread()).doOnNext {
                Log.d(TAG, "startLooper: doOnNext:${it}")
                removeCallbacks(changeRunnable)
                post(changeRunnable)
            }.subscribe({}, {})
    }


    private val changeRunnable = Runnable {
        val event = accessService?.getAccessibilityEvent()
        if (event == null) {
            touchLoadMore()
        }
        event?.let { event ->
            event.source?.let {
                findTextById(source = it)
//                findText(source = it)
                touchLoadMore()
            }
        }
    }


    private fun findTextById(source: AccessibilityNodeInfo) {
//        val nodeInfos = source.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/tv_desc")
        val nodeInfos = source.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/z3w")
        Log.d(TAG, "findTextById: size:${nodeInfos?.size}")
        nodeInfos?.forEach { info ->
            Log.d(TAG, "findTextById:  isVisibleToUser:${info.isVisibleToUser}")
            if (info.isVisibleToUser) {
                Log.d(TAG, "findTextById: info:${info}")
            }
        }
    }

    private fun findText(source: AccessibilityNodeInfo) {
        val nodeInfos = source.findAccessibilityNodeInfosByText("这是")
        Log.d(TAG, "findText size:${nodeInfos?.size}")
        nodeInfos?.forEach { info ->
            Log.d(TAG, "findText: info:${info} ")
            val parent = info.parent
            val descInfos = parent.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/tv_desc")
            descInfos?.forEach { descInfo ->

                Log.d(TAG, "findText: isVis:${descInfo.isVisibleToUser} descInfo:${descInfo}")
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

        val isDispatched = accessService?.dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                isStartLoad = false
            }
        }, null)
        Log.d(TAG, "touchLoadMore: isDispatched:${isDispatched}")
    }


}
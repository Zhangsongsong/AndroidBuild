package com.zasko.accessibility.views

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import com.zasko.accessibility.R
import com.zasko.accessibility.databinding.FloatWindowLayoutBinding
import com.zasko.accessibility.service.DemoAccessibilityService
import com.zasko.accessibility.utils.onClick
import kotlin.math.abs

class FloatWindowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), View.OnTouchListener {

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

    }


}
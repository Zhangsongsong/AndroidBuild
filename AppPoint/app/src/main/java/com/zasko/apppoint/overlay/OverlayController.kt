package com.zasko.apppoint.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.zasko.apppoint.automation.ActionOutcome
import com.zasko.apppoint.automation.AppProfileStore
import com.zasko.apppoint.automation.AutomationAccessibilityService

object OverlayController {
    private var overlayView: ControlOverlayView? = null
    private var windowManager: WindowManager? = null

    fun show(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            return false
        }
        if (overlayView != null) return true

        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 180
        }
        val view = ControlOverlayView(appContext)
        view.attachWindow(manager, params)
        manager.addView(view, params)
        windowManager = manager
        overlayView = view
        return true
    }

    fun hide() {
        val view = overlayView ?: return
        runCatching { windowManager?.removeView(view) }
        overlayView = null
        windowManager = null
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }
}

private class ControlOverlayView(
    context: Context
) : LinearLayout(context) {

    private var windowManager: WindowManager? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var lastRawX = 0f
    private var lastRawY = 0f

    init {
        orientation = VERTICAL
        setPadding(dp(10), dp(8), dp(10), dp(8))
        background = GradientDrawable().apply {
            setColor(0xEE1D232C.toInt())
            cornerRadius = dp(12).toFloat()
        }

        val selectedProfile = AppProfileStore.loadSelected(context)
        val title = selectedProfile?.appName ?: "未选择应用"
        val header = TextView(context).apply {
            text = "AppPoint · $title"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(dp(4), dp(2), dp(4), dp(8))
            setOnTouchListener { _, event -> handleDrag(event) }
        }
        addView(header, LayoutParams(dp(190), dp(34)))

        addActionButton("点按配置位置") {
            val profile = AppProfileStore.loadSelected(context)
            if (profile == null) {
                showResult(ActionOutcome(false, "请先在 AppPoint 首页选择应用"))
            } else {
                showResult(
                    AutomationAccessibilityService.dispatchTap(
                        profile.packageName,
                        profile.tapX,
                        profile.tapY
                    )
                )
            }
        }
        addActionButton("执行配置滑动") {
            val profile = AppProfileStore.loadSelected(context)
            if (profile == null) {
                showResult(ActionOutcome(false, "请先在 AppPoint 首页选择应用"))
            } else {
                showResult(
                    AutomationAccessibilityService.dispatchSwipe(
                        profile.packageName,
                        profile.swipeStartX,
                        profile.swipeStartY,
                        profile.swipeEndX,
                        profile.swipeEndY,
                        profile.swipeDurationMs
                    )
                )
            }
        }
        addActionButton("隐藏面板") {
            OverlayController.hide()
        }
    }

    fun attachWindow(manager: WindowManager, params: WindowManager.LayoutParams) {
        windowManager = manager
        windowParams = params
    }

    private fun addActionButton(text: String, action: () -> Unit) {
        val button = Button(context).apply {
            this.text = text
            isAllCaps = false
            textSize = 12f
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(4), 0, dp(4), 0)
            setOnClickListener { action() }
        }
        addView(button, LayoutParams(dp(190), dp(40)).apply {
            topMargin = dp(4)
        })
    }

    private fun showResult(outcome: ActionOutcome) {
        Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()
    }

    private fun handleDrag(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val params = windowParams ?: return true
                params.x += (event.rawX - lastRawX).toInt()
                params.y += (event.rawY - lastRawY).toInt()
                windowManager?.updateViewLayout(this, params)
                lastRawX = event.rawX
                lastRawY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> return true
        }
        return false
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

package com.zasko.apppoint.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.zasko.apppoint.automation.DemonRunner
import com.zasko.apppoint.automation.DemonScreen
import com.zasko.apppoint.automation.DemonStore
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

    fun setCaptureHidden(hidden: Boolean) {
        overlayView?.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
    }

    fun hide() {
        DemonRunner.stop("面板已关闭")
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
    private val controls = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    init {
        orientation = VERTICAL
        setPadding(dp(10), dp(8), dp(10), dp(8))
        background = GradientDrawable().apply {
            setColor(0xEE1D232C.toInt())
            cornerRadius = dp(12).toFloat()
        }

        val selectedProfile = AppProfileStore.loadSelected(context)
        val title = selectedProfile?.appName ?: "未选择应用"
        val headerTitle = TextView(context).apply {
            text = "AppPoint · $title"
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            contentDescription = "AppPoint · $title，拖动可移动悬浮窗"
            setPadding(dp(4), dp(2), dp(4), dp(2))
            setOnTouchListener { _, event -> handleDrag(event) }
        }
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(headerTitle, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
        var expanded = true
        val toggleButton = Button(context).apply {
            text = "收起"
            contentDescription = "收起悬浮窗"
            isAllCaps = false
            textSize = 12f
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(4), 0, dp(4), 0)
            setOnClickListener {
                expanded = !expanded
                controls.visibility = if (expanded) View.VISIBLE else View.GONE
                headerTitle.text = if (expanded) "AppPoint · $title" else "AppPoint"
                text = if (expanded) "收起" else "展开"
                contentDescription = if (expanded) "收起悬浮窗" else "展开悬浮窗"
                header.layoutParams = header.layoutParams.apply {
                    width = dp(if (expanded) 190 else 140)
                }
                windowParams?.let { windowManager?.updateViewLayout(this@ControlOverlayView, it) }
            }
        }
        header.addView(toggleButton, LayoutParams(dp(56), LayoutParams.MATCH_PARENT))
        addView(header, LayoutParams(dp(190), dp(40)))
        addView(controls, LayoutParams(dp(190), LayoutParams.WRAP_CONTENT))

        addActionButton("开始封魔 / 重新开始") { AutomationAccessibilityService.startDemon() }
        addActionButton("暂停封魔") { DemonRunner.stop(resumable = true) }
        addActionButton("继续封魔") { DemonRunner.resume() }
        addActionButton("只识别技能（不点击）") { AutomationAccessibilityService.inspectDemonSkills() }
        fun captureTargets(): List<DemonScreen> = runCatching {
            val profile = AppProfileStore.loadSelected(context) ?: return@runCatching DemonScreen.entries.toList()
            DemonStore.load(context, profile.packageName).rules.filter { it.requiresTemplate }.map { it.screen }
        }.getOrDefault(DemonScreen.entries.toList())
        var captureScreen = captureTargets().first()
        val selector = Button(context).apply {
            text = "采集目标：${captureScreen.title}"
            setOnClickListener {
                val targets = captureTargets()
                captureScreen = targets[(targets.indexOf(captureScreen) + 1) % targets.size]
                text = "采集目标：${captureScreen.title}"
            }
        }
        controls.addView(selector, LayoutParams(dp(190), dp(40)))
        addActionButton("采集当前界面模板") { AutomationAccessibilityService.collectDemon(captureScreen) }
        val status = TextView(context).apply { setTextColor(Color.WHITE); textSize = 12f }
        controls.addView(status, LayoutParams(dp(190), LayoutParams.WRAP_CONTENT))
        val handler = Handler(Looper.getMainLooper())
        val refresh = object : Runnable {
            override fun run() {
                status.text = "${DemonRunner.status.value.message} · ${DemonRunner.status.value.rounds}轮"
                if (isAttachedToWindow) handler.postDelayed(this, 500)
            }
        }
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { handler.post(refresh) }
            override fun onViewDetachedFromWindow(v: View) { handler.removeCallbacks(refresh) }
        })

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
            setOnClickListener {
                if (DemonRunner.status.value.running && text in listOf("点按配置位置", "执行配置滑动", "采集当前界面模板", "只识别技能（不点击）")) {
                    Toast.makeText(context, "请先暂停封魔流程", Toast.LENGTH_SHORT).show()
                } else action()
            }
        }
        controls.addView(button, LayoutParams(dp(190), dp(40)).apply {
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

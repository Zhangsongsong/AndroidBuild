package com.zasko.apppoint.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.view.Display
import android.view.accessibility.AccessibilityWindowInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

data class AutomationServiceState(
    val connected: Boolean = false,
    val currentPackage: String? = null,
    val currentAppName: String? = null,
    val lastActionMessage: String = "尚未执行操作"
)

data class ActionOutcome(
    val accepted: Boolean,
    val message: String
)

class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private val stateHolder = mutableStateOf(AutomationServiceState())
        val state: State<AutomationServiceState> = stateHolder

        @Volatile
        private var serviceInstance: AutomationAccessibilityService? = null

        fun startDemon() {
            val service = serviceInstance ?: return
            val pkg = AppProfileStore.selectedPackage(service) ?: return
            DemonRunner.start(service, pkg)
        }

        fun collectDemon(screen: DemonScreen) {
            val service = serviceInstance ?: return
            val pkg = AppProfileStore.selectedPackage(service) ?: return
            DemonRunner.collect(service, pkg, screen)
        }

        fun inspectDemonSkills() {
            val service = serviceInstance ?: return
            val pkg = AppProfileStore.selectedPackage(service) ?: return
            DemonRunner.inspectSkills(service, pkg)
        }

        fun dispatchTap(
            targetPackage: String,
            x: Int,
            y: Int
        ): ActionOutcome {
            val service = serviceInstance
                ?: return ActionOutcome(false, "无障碍服务尚未连接")
            return service.tap(targetPackage, x, y)
        }

        fun dispatchSwipe(
            targetPackage: String,
            startX: Int,
            startY: Int,
            endX: Int,
            endY: Int,
            durationMs: Long
        ): ActionOutcome {
            val service = serviceInstance
                ?: return ActionOutcome(false, "无障碍服务尚未连接")
            return service.swipe(
                targetPackage,
                startX,
                startY,
                endX,
                endY,
                durationMs
            )
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastScreenshotAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInstance = this
        serviceInfo = buildServiceInfo()
        stateHolder.value = AutomationServiceState(
            connected = true,
            lastActionMessage = "服务已连接，等待目标应用进入前台"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            event.packageName
                ?.toString()
                ?.trim()
                ?.ifBlank { null }
                ?.let { packageName ->
                    val appName = runCatching {
                        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                        packageManager.getApplicationLabel(applicationInfo).toString()
                    }.getOrNull()?.ifBlank { null }
                    stateHolder.value = stateHolder.value.copy(
                        currentPackage = packageName,
                        currentAppName = appName ?: packageName
                    )
                }
        }
    }

    override fun onInterrupt() {
        DemonRunner.stop("无障碍服务被中断")
        stateHolder.value = stateHolder.value.copy(lastActionMessage = "服务被系统中断")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        DemonRunner.stop("无障碍服务已断开")
        serviceInstance = null
        stateHolder.value = AutomationServiceState(lastActionMessage = "服务已断开")
        return super.onUnbind(intent)
    }

    private fun buildServiceInfo(): AccessibilityServiceInfo {
        return AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = 0
            notificationTimeout = 100
            packageNames = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        }
    }

    private fun tap(targetPackage: String, x: Int, y: Int): ActionOutcome {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return rejectAction("Android 7.0 以下不支持无障碍手势")
        }
        if (!isTargetPackageForeground(targetPackage)) {
            return rejectAction("目标应用不在前台，未执行点击")
        }
        if (x < 0 || y < 0) {
            return rejectAction("点击坐标不能为负数")
        }
        return dispatchGestureOnMain(
            path = Path().apply { moveTo(x.toFloat(), y.toFloat()) },
            durationMs = 80L,
            description = "点击 ($x, $y)"
        )
    }

    private fun swipe(
        targetPackage: String,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMs: Long
    ): ActionOutcome {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return rejectAction("Android 7.0 以下不支持无障碍手势")
        }
        if (!isTargetPackageForeground(targetPackage)) {
            return rejectAction("目标应用不在前台，未执行滑动")
        }
        if (startX < 0 || startY < 0 || endX < 0 || endY < 0) {
            return rejectAction("滑动坐标不能为负数")
        }
        val safeDuration = durationMs.coerceIn(50L, 5_000L)
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }
        return dispatchGestureOnMain(
            path = path,
            durationMs = safeDuration,
            description = "滑动 ($startX, $startY) -> ($endX, $endY)"
        )
    }

    fun isTargetPackageForeground(targetPackage: String): Boolean {
        // Event packages may be stale or belong to an overlay. Prefer the currently focused app.
        val focused = windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }
        val packageName = focused?.root?.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
        return packageName == targetPackage
    }

    fun captureScreen(targetPackage: String, callback: (Result<Bitmap>) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { captureScreen(targetPackage, callback) }
            return
        }
        captureScreenAttempt(targetPackage, 0, callback)
    }

    private fun captureScreenAttempt(targetPackage: String, retry: Int, callback: (Result<Bitmap>) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            callback(Result.failure(IllegalStateException("屏幕识别需要 Android 11 或以上")))
            return
        }
        if (!isTargetPackageForeground(targetPackage)) {
            callback(Result.failure(IllegalStateException("目标游戏不在前台")))
            return
        }
        // Android 11 devices can enforce a one-second screenshot interval. Apply this
        // across calibration, OCR inspection and flow polling, not just within a run.
        val delay = 1100L - (SystemClock.elapsedRealtime() - lastScreenshotAt)
        if (delay > 0) {
            mainHandler.postDelayed({ captureScreenAttempt(targetPackage, retry, callback) }, delay)
            return
        }
        lastScreenshotAt = SystemClock.elapsedRealtime()
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val buffer = result.hardwareBuffer
                    val image = runCatching {
                        val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                            ?: error("无法读取截图")
                        try {
                            hardware.copy(Bitmap.Config.ARGB_8888, false) ?: error("无法复制截图")
                        } finally { hardware.recycle() }
                    }
                    buffer.close()
                    callback(image)
                }
                override fun onFailure(errorCode: Int) {
                    if (errorCode == ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT && retry < 2) {
                        mainHandler.postDelayed({ captureScreenAttempt(targetPackage, retry + 1, callback) }, 1100L)
                    } else {
                        val reason = when (errorCode) {
                            ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT -> "截图间隔过短，请稍后重试"
                            ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS -> "截图权限不可用，请重新启用无障碍服务"
                            ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY -> "截图显示器不可用"
                            else -> "请检查截图能力或安全窗口限制"
                        }
                        callback(Result.failure(IllegalStateException("截图失败（$errorCode）：$reason")))
                    }
                }
            })
        } catch (e: Exception) { callback(Result.failure(e)) }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.N)
    fun performFlowTap(targetPackage: String, x: Int, y: Int, completed: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isTargetPackageForeground(targetPackage)) {
            completed(false)
            return
        }
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder().addStroke(StrokeDescription(path, 0L, 80L)).build()
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { completed(true) }
            override fun onCancelled(gestureDescription: GestureDescription?) { completed(false) }
        }, mainHandler)
        if (!accepted) completed(false)
    }

    private fun rejectAction(message: String): ActionOutcome {
        stateHolder.value = stateHolder.value.copy(lastActionMessage = message)
        return ActionOutcome(false, message)
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.N)
    private fun dispatchGestureOnMain(
        path: Path,
        durationMs: Long,
        description: String
    ): ActionOutcome {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                dispatchGestureOnMain(path, durationMs, description)
            }
            return ActionOutcome(true, "操作已提交")
        }

        val gesture = GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0L, durationMs))
            .build()

        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    stateHolder.value = stateHolder.value.copy(
                        lastActionMessage = "$description 已完成"
                    )
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    stateHolder.value = stateHolder.value.copy(
                        lastActionMessage = "$description 被取消"
                    )
                }
            },
            null
        )

        if (!dispatched) {
            return rejectAction("$description 提交失败")
        }
        stateHolder.value = stateHolder.value.copy(lastActionMessage = "$description 执行中")
        return ActionOutcome(true, "$description 执行中")
    }
}

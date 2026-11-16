package com.zasko.apppoint.automation

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.android.gms.tasks.Tasks
import androidx.compose.runtime.mutableStateOf
import com.zasko.apppoint.overlay.OverlayController
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object DemonRunner {
    data class Status(val running: Boolean = false, val message: String = "未开始", val rounds: Int = 0)
    val status = mutableStateOf(Status())
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var generation = 0
    private var flow: DemonFlow? = null
    private var activeConfig: DemonConfig? = null
    private var activePackage: String? = null
    private var service: AutomationAccessibilityService? = null
    private var captureBusy = false
    private var canResume = false
    private val textRecognizer by lazy { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }

    fun start(service: AutomationAccessibilityService, pkg: String) {
        if (status.value.running || captureBusy) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { stop("屏幕识别需要 Android 11 或以上"); return }
        val config = runCatching { DemonStore.load(service, pkg).also { it.validate() } }
            .getOrElse { stop(it.message ?: "配置无效"); return }
        if (config.skillPriority.isEmpty()) { stop("请先选择流派或填写技能名称优先级"); return }
        val samples = runCatching { DemonStore.loadSamples(service, pkg, config.rules) }
            .getOrElse { stop(it.message ?: "模板读取失败"); return }
        val missing = config.rules.filter { it.screen != DemonScreen.STOP && it.screen !in samples }
        if (missing.isNotEmpty()) { stop("请先采集模板：${missing.joinToString { it.screen.title }}"); return }
        if (!service.isTargetPackageForeground(pkg)) { stop("请先打开目标游戏，再从悬浮窗开始"); return }
        generation++
        canResume = false
        this.service = service
        activePackage = pkg
        activeConfig = config
        flow = DemonFlow(config, SystemClock.elapsedRealtime())
        status.value = Status(true, "开始识别界面")
        poll(generation, service, pkg, config, samples)
    }

    fun stop(message: String = "已暂停", resumable: Boolean = false) {
        canResume = resumable && flow != null
        generation++
        status.value = status.value.copy(running = false, message = message)
        OverlayController.setCaptureHidden(false)
    }

    fun resume() {
        if (status.value.running || captureBusy) return
        if (!canResume) { stop("请使用开始封魔重新开始"); return }
        val s = service ?: return
        val pkg = activePackage ?: return
        val config = activeConfig ?: return
        if (!s.isTargetPackageForeground(pkg)) { stop("请先返回游戏再继续", resumable = true); return }
        val samples = runCatching { DemonStore.loadSamples(s, pkg, config.rules) }.getOrElse {
            stop(it.message ?: "模板读取失败"); return
        }
        // Resume preserves the in-flight screen latch and round count, but shifts timeout deadlines.
        flow?.resumeAt(SystemClock.elapsedRealtime()) ?: return
        generation++
        status.value = status.value.copy(running = true, message = "继续识别")
        poll(generation, s, pkg, config, samples)
    }

    /** Calibration aid: reads the three title regions without advancing the flow or injecting a tap. */
    fun inspectSkills(service: AutomationAccessibilityService, pkg: String) {
        if (status.value.running || captureBusy) return
        val config = runCatching { DemonStore.load(service, pkg) }.getOrElse {
            stop(it.message ?: "配置无效"); return
        }
        val token = ++generation
        captureBusy = true
        OverlayController.setCaptureHidden(true)
        val watchdog = Runnable {
            if (token == generation) stop("技能识别超时，等待识别任务释放")
        }
        handler.postDelayed(watchdog, 15000)
        handler.postDelayed({
            service.captureScreen(pkg) { result ->
                result.fold(onSuccess = { bitmap ->
                    worker.execute {
                        val names = runCatching { recognizeSkills(bitmap, config) }
                        bitmap.recycle()
                        handler.post {
                            handler.removeCallbacks(watchdog)
                            captureBusy = false
                            if (token == generation) {
                                val message = names.fold(
                                    onSuccess = { "名称OCR：${it.mapIndexed { i, name -> "${i + 1}:${name.ifBlank { "空白" }}" }.joinToString(" / ")}" },
                                    onFailure = { "技能识别失败：${it.message}" }
                                )
                                status.value = status.value.copy(message = message)
                                OverlayController.setCaptureHidden(false)
                            }
                        }
                    }
                }, onFailure = {
                    handler.removeCallbacks(watchdog)
                    captureBusy = false
                    if (token == generation) stop(it.message ?: "截图失败")
                })
            }
        }, 200)
    }

    fun collect(service: AutomationAccessibilityService, pkg: String, screen: DemonScreen) {
        if (status.value.running || captureBusy) { status.value = status.value.copy(message = "请先暂停，等待当前截图结束"); return }
        val rule = runCatching { DemonStore.load(service, pkg).rules.first { it.screen == screen } }
            .getOrElse { stop(it.message ?: "配置无效"); return }
        canResume = false
        captureBusy = true
        val token = ++generation
        OverlayController.setCaptureHidden(true)
        handler.postDelayed({
            if (token != generation) { captureBusy = false; return@postDelayed }
            val watchdog = Runnable {
                if (token == generation) { captureBusy = false; stop("截图超时，已停止") }
            }
            handler.postDelayed(watchdog, 5000)
            service.captureScreen(pkg) { result ->
                handler.removeCallbacks(watchdog)
                result.fold(onSuccess = { image ->
                    try {
                        if (generation == token) {
                            DemonStore.saveTemplate(service, pkg, rule, image)
                            status.value = status.value.copy(message = "已采集${screen.title}模板")
                        }
                    } catch (e: Exception) { stop(e.message ?: "采集失败") }
                    finally { image.recycle() }
                }, onFailure = { if (generation == token) stop(it.message ?: "截图失败") })
                captureBusy = false
                OverlayController.setCaptureHidden(false)
            }
        }, 200)
    }

    private fun poll(token: Int, service: AutomationAccessibilityService, pkg: String, config: DemonConfig, samples: Map<DemonScreen, ScreenTemplate>) {
        if (token != generation || !status.value.running) return
        if (!service.isTargetPackageForeground(pkg)) { stop("目标游戏不在前台，已暂停", resumable = true); return }
        captureBusy = true
        OverlayController.setCaptureHidden(true)
        handler.postDelayed({
            if (token != generation) { captureBusy = false; return@postDelayed }
            val watchdog = Runnable {
                if (token == generation) { captureBusy = false; stop("截图超时，已停止") }
            }
            handler.postDelayed(watchdog, 5000)
            service.captureScreen(pkg) { result ->
                handler.removeCallbacks(watchdog)
                result.fold(onSuccess = { bitmap ->
                    if (token != generation) { bitmap.recycle(); captureBusy = false; return@fold }
                    worker.execute {
                        val match = runCatching {
                            val screen = recognize(bitmap, config, samples)
                            val skills = if (screen == DemonScreen.SKILLS) recognizeSkills(bitmap, config) else emptyList()
                            screen to skills
                        }
                        val width = bitmap.width
                        val height = bitmap.height
                        bitmap.recycle()
                        handler.post {
                            captureBusy = false
                            if (token != generation) return@post
                            match.fold(onSuccess = { (screen, recognizedSkills) ->
                                if (!service.isTargetPackageForeground(pkg)) { stop("目标游戏不在前台，已暂停", resumable = true); return@fold }
                                val decision = flow!!.observe(screen, SystemClock.elapsedRealtime(), recognizedSkills)
                                val statusText = if (screen == DemonScreen.SKILLS) {
                                    "技能识别：${recognizedSkills.joinToString(" / ").ifBlank { "无文字" }}"
                                } else screen?.title ?: "未识别，等待画面稳定"
                                val displayed = if (decision is FlowDecision.Tap) decision.label else statusText
                                status.value = Status(true, displayed, flow!!.completedRounds)
                                when (decision) {
                                    is FlowDecision.Halt -> stop(decision.message, decision.resumable)
                                    is FlowDecision.Tap -> {
                                        val gestureWatchdog = Runnable {
                                            if (token == generation) stop("点击回调超时，已停止")
                                        }
                                        handler.postDelayed(gestureWatchdog, 3000)
                                        service.performFlowTap(pkg,
                                            (decision.point.x * (width - 1)).toInt(),
                                            (decision.point.y * (height - 1)).toInt()
                                        ) { success ->
                                            handler.removeCallbacks(gestureWatchdog)
                                            if (token == generation) {
                                                if (!success) stop("${decision.label}点击失败，已暂停")
                                                else next(token, service, pkg, config, samples)
                                            }
                                        }
                                    }
                                    FlowDecision.Wait -> next(token, service, pkg, config, samples)
                                }
                            }, onFailure = { stop(it.message ?: "识别失败") })
                        }
                    }
                }, onFailure = { captureBusy = false; if (token == generation) stop(it.message ?: "截图失败") })
            }
        }, 200)
    }

    private fun next(token: Int, service: AutomationAccessibilityService, pkg: String, config: DemonConfig, samples: Map<DemonScreen, ScreenTemplate>) {
        OverlayController.setCaptureHidden(false)
        handler.postDelayed({ poll(token, service, pkg, config, samples) }, config.pollMs)
    }

    private fun recognize(image: Bitmap, config: DemonConfig, samples: Map<DemonScreen, ScreenTemplate>): DemonScreen? {
        require(config.matchesDisplay(image.width, image.height)) { "屏幕比例与录像布局不符，请校准后再运行" }
        val matches = config.rules.mapNotNull { rule ->
            val template = samples[rule.screen] ?: return@mapNotNull null
            val crop = DemonStore.crop(image, rule.region)
            val score = try { TemplateScore.similarity(DemonStore.sample(crop), template.primary) } finally {
                if (crop !== image) crop.recycle()
            }
            val guardMatches = rule.guardRegion?.let { region ->
                val expected = template.guard ?: return@mapNotNull null
                val guard = DemonStore.crop(image, region)
                try { TemplateScore.similarity(DemonStore.sample(guard), expected) >= rule.guardThreshold }
                finally { if (guard !== image) guard.recycle() }
            } ?: true
            if (score >= rule.threshold && guardMatches) rule.screen to score else null
        }.sortedByDescending { it.second }
        // Ambiguous classification is not sufficient evidence to inject a touch.
        if (matches.size > 1 && matches[0].second - matches[1].second < .025) return null
        return matches.firstOrNull()?.first
    }

    private fun recognizeSkills(image: Bitmap, config: DemonConfig): List<String> =
        config.skillCardRegions.map { region ->
            val regionImage = DemonStore.crop(image, region)
            val factor = kotlin.math.ceil(80.0 / regionImage.height).toInt().coerceIn(1, 4)
            val crop = if (factor > 1) {
                Bitmap.createScaledBitmap(regionImage, regionImage.width * factor, regionImage.height * factor, true).also {
                    if (regionImage !== image && regionImage !== it) regionImage.recycle()
                }
            } else if (regionImage === image) {
                checkNotNull(image.copy(Bitmap.Config.ARGB_8888, false)) { "无法复制名称截图" }
            } else regionImage
            var submitted = false
            try {
                val task = textRecognizer.process(InputImage.fromBitmap(crop, 0))
                // A timeout does not cancel ML Kit. Keep its bitmap alive until the task completes.
                task.addOnCompleteListener(worker) { crop.recycle() }
                submitted = true
                Tasks.await(task, 3, TimeUnit.SECONDS).text.trim()
            } finally {
                if (!submitted) crop.recycle()
            }
        }
}

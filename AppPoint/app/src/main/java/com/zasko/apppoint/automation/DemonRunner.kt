package com.zasko.apppoint.automation

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.android.gms.tasks.Tasks
import androidx.compose.runtime.mutableStateOf
import com.zasko.apppoint.overlay.OverlayController
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.io.File

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

    private fun trace(message: String) {
        Log.i("DemonRunner", message)
        service?.let { context -> runCatching {
            val log = File(context.filesDir, "demon-debug.log")
            if (log.length() > 256 * 1024) log.writeText("")
            log.appendText("${System.currentTimeMillis()} $message\n")
        } }
    }

    fun start(service: AutomationAccessibilityService, pkg: String) {
        if (status.value.running || captureBusy) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { stop("屏幕识别需要 Android 11 或以上"); return }
        val config = runCatching { DemonStore.load(service, pkg).also { it.validate() } }
            .getOrElse { stop(it.message ?: "配置无效"); return }
        if (config.skillPriority.isEmpty()) { stop("请先选择流派或填写技能名称优先级"); return }
        val samples = runCatching { DemonStore.loadSamples(service, pkg, config.rules) }
            .getOrElse { stop(it.message ?: "模板读取失败"); return }
        val missing = config.rules.filter { it.requiresTemplate && it.screen != DemonScreen.STOP && it.screen !in samples }
        if (missing.isNotEmpty()) { stop("请先采集模板：${missing.joinToString { it.screen.title }}"); return }
        if (!service.isTargetPackageForeground(pkg)) { stop("请先打开目标游戏，再从悬浮窗开始"); return }
        generation++
        canResume = false
        this.service = service
        activePackage = pkg
        activeConfig = config
        flow = DemonFlow(config, SystemClock.elapsedRealtime())
        status.value = Status(true, "开始识别界面")
        trace("start package=$pkg priority=${config.skillPriority} fallback=${config.skillFallback}")
        poll(generation, service, pkg, config, samples)
    }

    fun stop(message: String = "已暂停", resumable: Boolean = false) {
        canResume = resumable && flow != null
        generation++
        status.value = status.value.copy(running = false, message = message)
        trace("stop rounds=${status.value.rounds} reason=$message")
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
        if (!rule.requiresTemplate) {
            status.value = status.value.copy(message = "封魔入口按卡片比例定位，无需采集模板")
            return
        }
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
                            val screenDetection = recognize(bitmap, config, samples)
                            val dismissHint = if (screenDetection?.screen in setOf(
                                    DemonScreen.BATTLE, DemonScreen.SKILLS
                                ) || screenDetection == null
                            ) recognizeDismissHint(bitmap) else null
                            val failureSettlement = if (dismissHint == null && screenDetection?.screen in setOf(
                                    DemonScreen.BATTLE, null
                                ) && config.rules.any { it.screen == DemonScreen.SETTLEMENT }
                            ) recognizeFailureSettlement(bitmap) else null
                            val settledDetection = failureSettlement?.let {
                                Detection(DemonScreen.SETTLEMENT, 1.0, it)
                            } ?: screenDetection
                            val detection = dismissHint?.let {
                                Detection(DemonScreen.DISMISS_OVERLAY, 1.0, it)
                            } ?: settledDetection
                            val screen = detection?.screen
                            val skills = if (screen == DemonScreen.SKILLS) recognizeSkills(bitmap, config) else emptyList()
                            detection to skills
                        }
                        val width = bitmap.width
                        val height = bitmap.height
                        bitmap.recycle()
                        handler.post {
                            captureBusy = false
                            if (token != generation) return@post
                            match.fold(onSuccess = { (detection, recognizedSkills) ->
                                val screen = detection?.screen
                                if (!service.isTargetPackageForeground(pkg)) { stop("目标游戏不在前台，已暂停", resumable = true); return@fold }
                                val decision = flow!!.observe(
                                    screen,
                                    SystemClock.elapsedRealtime(),
                                    recognizedSkills,
                                    dismissPoint = detection?.takeIf { screen == DemonScreen.DISMISS_OVERLAY }?.tap,
                                    entrancePoint = detection?.takeIf { screen == DemonScreen.ENTRANCE }?.tap,
                                    entrancePixelConfirmed = detection?.entrancePixelConfirmed == true,
                                    settlementPoint = detection?.takeIf { screen == DemonScreen.SETTLEMENT }?.tap
                                )
                                val statusText = if (screen == DemonScreen.SKILLS) {
                                    "技能识别：${recognizedSkills.joinToString(" / ").ifBlank { "无文字" }}"
                                } else screen?.title ?: "未识别，等待画面稳定"
                                val displayed = if (decision is FlowDecision.Tap) decision.label else statusText
                                if (decision != FlowDecision.Wait || status.value.message != displayed) {
                                    trace("screen=$screen skills=$recognizedSkills normalized=${recognizedSkills.map(SkillSelector::normalize)} decision=$decision rounds=${flow!!.completedRounds}")
                                }
                                status.value = Status(true, displayed, flow!!.completedRounds)
                                when (decision) {
                                    is FlowDecision.Halt -> stop(decision.message, decision.resumable)
                                    is FlowDecision.Tap -> {
                                        val gestureWatchdog = Runnable {
                                            if (token == generation) stop("点击回调超时，已停止")
                                        }
                                        handler.postDelayed(gestureWatchdog, 3000)
                                        val point = decision.point
                                        trace("tap screen=$screen point=$point image=${width}x$height")
                                        service.performFlowTap(pkg,
                                            (point.x * (width - 1)).toInt(),
                                            (point.y * (height - 1)).toInt()
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

    private data class Detection(
        val screen: DemonScreen,
        val score: Double,
        val tap: ScreenPoint?,
        val entrancePixelConfirmed: Boolean = false
    )

    private fun recognizeDismissHint(image: Bitmap): ScreenPoint? {
        // Keep the small white instruction legible instead of shrinking the entire display.
        val top = (image.height * .65).toInt()
        val bottom = (image.height * .98).toInt()
        val crop = Bitmap.createBitmap(image, 0, top, image.width, bottom - top)
        val scaledWidth = minOf(image.width, 1080)
        val scaled = Bitmap.createScaledBitmap(
            crop, scaledWidth, (crop.height * scaledWidth.toDouble() / crop.width).toInt(), true
        )
        if (crop !== scaled && crop !== image) crop.recycle()
        val scaleX = image.width.toDouble() / scaled.width
        val scaleY = (bottom - top).toDouble() / scaled.height
        var submitted = false
        try {
            val task = textRecognizer.process(InputImage.fromBitmap(scaled, 0))
            task.addOnCompleteListener(worker) { scaled.recycle() }
            submitted = true
            val result = Tasks.await(task, 3, TimeUnit.SECONDS)
            val lines = result.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                ScreenText(line.text, ScreenRegion(
                    box.left * scaleX / image.width,
                    (top + box.top * scaleY) / image.height,
                    box.right * scaleX / image.width,
                    (top + box.bottom * scaleY) / image.height
                ))
            }
            val point = DismissHintLocator.find(lines)
            trace("dismiss point=$point lines=$lines")
            return point
        } finally {
            if (!submitted) scaled.recycle()
        }
    }

    private fun recognizeFailureSettlement(image: Bitmap): ScreenPoint? {
        val top = (image.height * .12).toInt()
        val bottom = (image.height * .96).toInt()
        val crop = Bitmap.createBitmap(image, 0, top, image.width, bottom - top)
        val scaledWidth = minOf(image.width, 1080)
        val scaled = Bitmap.createScaledBitmap(
            crop, scaledWidth, (crop.height * scaledWidth.toDouble() / crop.width).toInt(), true
        )
        if (crop !== scaled && crop !== image) crop.recycle()
        val scaleX = image.width.toDouble() / scaled.width
        val scaleY = (bottom - top).toDouble() / scaled.height
        var submitted = false
        try {
            val task = textRecognizer.process(InputImage.fromBitmap(scaled, 0))
            task.addOnCompleteListener(worker) { scaled.recycle() }
            submitted = true
            val result = Tasks.await(task, 3, TimeUnit.SECONDS)
            val lines = result.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                ScreenText(line.text, ScreenRegion(
                    box.left * scaleX / image.width,
                    (top + box.top * scaleY) / image.height,
                    box.right * scaleX / image.width,
                    (top + box.bottom * scaleY) / image.height
                ))
            }
            val point = FailureSettlementLocator.find(lines)
            trace("failure settlement point=$point lines=$lines")
            return point
        } finally {
            if (!submitted) scaled.recycle()
        }
    }

    private fun recognize(image: Bitmap, config: DemonConfig, samples: Map<DemonScreen, ScreenTemplate>): Detection? {
        require(config.matchesDisplay(image.width, image.height)) { "屏幕比例与录像布局不符，请校准后再运行" }
        val matches = config.rules.filter { it.requiresTemplate }.mapNotNull { rule ->
            val template = samples[rule.screen] ?: return@mapNotNull null
            fun score(region: ScreenRegion, expected: IntArray): Double {
                val crop = DemonStore.crop(image, region)
                return try { TemplateScore.similarity(DemonStore.sample(crop), expected) } finally {
                if (crop !== image) crop.recycle()
                }
            }
            val offsets = if (rule.verticalSearch) (-75..75).map { it * .004 } else listOf(0.0)
            offsets.mapNotNull { offset ->
                val shifted = rule.shifted(offset) ?: return@mapNotNull null
                val primaryScore = score(shifted.region, template.primary)
                if (primaryScore < rule.threshold) return@mapNotNull null
                val guardScore = shifted.guardRegion?.let { region ->
                    score(region, template.guard ?: return@mapNotNull null)
                }
                if (guardScore != null && guardScore < rule.guardThreshold) return@mapNotNull null
                Detection(rule.screen, minOf(primaryScore, guardScore ?: primaryScore), shifted.tap)
            }.maxByOrNull { it.score }
        }.sortedByDescending { it.score }
        // Ambiguous classification is not sufficient evidence to inject a touch.
        if (matches.size > 1 && matches[0].score - matches[1].score < .025) return null
        return matches.firstOrNull() ?: if (config.rules.any { !it.requiresTemplate }) {
            recognizeEntranceDetection(image)
        } else null
    }

    internal fun recognizeEntrance(image: Bitmap): ScreenPoint? = recognizeEntranceDetection(image)?.tap

    private fun recognizeEntranceDetection(image: Bitmap): Detection? {
        val previewWidth = minOf(image.width, 360)
        val preview = Bitmap.createScaledBitmap(image, previewWidth,
            (image.height * previewWidth.toDouble() / image.width).toInt(), true)
        val button = try {
            val pixels = IntArray(preview.width * preview.height)
            preview.getPixels(pixels, 0, preview.width, 0, 0, preview.width, preview.height)
            EntranceButtonLocator.find(pixels, preview.width, preview.height)
        } finally { if (preview !== image) preview.recycle() }
        if (button != null) {
            trace("entrance method=button proportionalTap=$button")
            return Detection(DemonScreen.ENTRANCE, 1.0, button, entrancePixelConfirmed = true)
        }
        val top = (image.height * .10).toInt()
        val crop = Bitmap.createBitmap(image, 0, top, image.width, (image.height * .69).toInt())
        val scaled = Bitmap.createScaledBitmap(crop, 720, (crop.height * 720.0 / crop.width).toInt(), true)
        if (crop !== scaled) crop.recycle()
        val scale = image.width.toDouble() / scaled.width
        var submitted = false
        try {
            val task = textRecognizer.process(InputImage.fromBitmap(scaled, 0))
            task.addOnCompleteListener(worker) { scaled.recycle() }
            submitted = true
            val result = Tasks.await(task, 3, TimeUnit.SECONDS)
            val labels = result.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                ScreenText(line.text, ScreenRegion(
                    box.left * scale / image.width, (top + box.top * scale) / image.height,
                    box.right * scale / image.width, (top + box.bottom * scale) / image.height))
            }
            val candidate = EntranceLocator.find(labels, image.width, image.height)
            val accepted = candidate?.takeIf { hasEntranceButton(image, it) }
            trace("entrance method=ocr candidate=$candidate accepted=${accepted != null} titles=${labels.joinToString { label -> label.text }}")
            return accepted?.let { Detection(DemonScreen.ENTRANCE, 1.0, it) }
        } finally {
            if (!submitted) scaled.recycle()
        }
    }

    /** Verify the calculated position is inside the card's gold button, without storing a template. */
    private fun hasEntranceButton(image: Bitmap, point: ScreenPoint): Boolean {
        var gold = 0
        for (row in -3..3) for (column in -5..5) {
            val x = ((point.x + column * .02) * image.width).toInt().coerceIn(0, image.width - 1)
            val y = ((point.y + row * .005) * image.height).toInt().coerceIn(0, image.height - 1)
            val color = image.getPixel(x, y)
            val r = (color shr 16) and 255
            val g = (color shr 8) and 255
            val b = color and 255
            if (r >= 190 && g >= 160 && b >= 100 && r >= g && g - b >= 15) gold++
        }
        return gold >= 40
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

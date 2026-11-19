package com.zasko.apppoint.automation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ScreenTemplate(val primary: IntArray, val guard: IntArray? = null)

object DemonStore {
    private fun folder(context: Context, pkg: String): File {
        require(pkg.matches(Regex("[A-Za-z0-9_.]+"))) { "无效包名" }
        return File(context.filesDir, "demon/$pkg").also { it.mkdirs() }
    }
    fun load(context: Context, pkg: String): DemonConfig {
        val file = configFile(context, pkg)
        val atomic = AtomicFile(file)
        return if (file.exists() || File(file.path + ".bak").exists()) {
            val text = atomic.readFully().toString(Charsets.UTF_8)
            val storedPollMs = JSONObject(text).optLong("pollMs", 2000L)
            val decoded = decode(text)
            val home = DemonVideoPreset.config().rules.first { it.screen == DemonScreen.HOME }
            val addHome = pkg == DemonVideoPreset.TARGET_PACKAGE &&
                decoded.rules.any { it.screen == DemonScreen.ENTRANCE && it.verticalSearch } &&
                decoded.rules.none { it.screen == DemonScreen.HOME }
            val updatePollInterval = storedPollMs < 2000L
            val config = decoded.copy(
                pollMs = decoded.pollMs.coerceAtLeast(2000L),
                rules = if (addHome) listOf(home) + decoded.rules else decoded.rules
            )
            if (addHome) DemonVideoPreset.installTemplates(context, pkg, config.copy(rules = listOf(home)))
            // Persist the migration once, preserving calibration and non-empty saved priorities.
            if (addHome || updatePollInterval || (JSONObject(text).optJSONArray("skillPriority")?.length() ?: 0) == 0) {
                save(context, pkg, config)
            }
            config
        } else {
            val config = if (pkg == DemonVideoPreset.TARGET_PACKAGE) {
                DemonVideoPreset.config().also { DemonVideoPreset.installTemplates(context, pkg, it) }
            } else DemonConfig()
            save(context, pkg, config)
            config
        }
    }
    fun configFile(context: Context, pkg: String) = File(folder(context, pkg), "flow.json")

    fun saveSkillPriority(context: Context, pkg: String, priority: List<String>, build: String, fallback: SkillFallback) {
        // Read the latest disk version; changing priorities must not overwrite calibration or timing.
        val updated = load(context, pkg).withSkillPriority(priority, build, fallback)
        save(context, pkg, updated)
        check(load(context, pkg) == updated) { "配置保存校验失败，请重新保存" }
    }
    fun save(context: Context, pkg: String, config: DemonConfig) {
        config.validate()
        val contents = encode(config).toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(configFile(context, pkg))
        val output = atomic.startWrite()
        try {
            output.write(contents)
            atomic.finishWrite(output)
        } catch (failure: Exception) {
            atomic.failWrite(output)
            throw failure
        }
    }
    fun templateFile(context: Context, pkg: String, screen: DemonScreen) = File(folder(context, pkg), "${screen.name}.png")
    fun guardFile(context: Context, pkg: String, screen: DemonScreen) = File(folder(context, pkg), "${screen.name}_guard.png")
    fun hasTemplate(context: Context, pkg: String, screen: DemonScreen) = templateFile(context, pkg, screen).isFile

    fun saveTemplate(context: Context, pkg: String, rule: ScreenRule, bitmap: Bitmap) {
        rule.region.validate()
        fun saveRegion(region: ScreenRegion, file: File) {
            val crop = crop(bitmap, region)
            try {
                file.outputStream().use { check(crop.compress(Bitmap.CompressFormat.PNG, 100, it)) { "保存图片失败" } }
            } finally { if (crop !== bitmap) crop.recycle() }
        }
        saveRegion(rule.region, templateFile(context, pkg, rule.screen))
        rule.guardRegion?.let { saveRegion(it, guardFile(context, pkg, rule.screen)) }
    }
    fun sample(bitmap: Bitmap): IntArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, 48, 32, true)
        val pixels = IntArray(48 * 32)
        scaled.getPixels(pixels, 0, 48, 0, 0, 48, 32)
        if (scaled !== bitmap) scaled.recycle()
        return pixels
    }
    fun crop(bitmap: Bitmap, region: ScreenRegion): Bitmap {
        val x = (region.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (region.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (region.right * bitmap.width).toInt().coerceIn(x + 1, bitmap.width)
        val bottom = (region.bottom * bitmap.height).toInt().coerceIn(y + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, x, y, right - x, bottom - y)
    }
    fun loadSamples(context: Context, pkg: String, rules: List<ScreenRule>): Map<DemonScreen, ScreenTemplate> = buildMap {
        fun load(file: File): IntArray {
            val image = requireNotNull(BitmapFactory.decodeFile(file.path)) { "模板图片损坏：${file.name}" }
            try { return sample(image) } finally { image.recycle() }
        }
        rules.forEach { rule ->
            if (!rule.requiresTemplate) return@forEach
            val file = templateFile(context, pkg, rule.screen)
            val guard = guardFile(context, pkg, rule.screen)
            if (file.exists() && (rule.guardRegion == null || guard.exists())) {
                put(rule.screen, ScreenTemplate(load(file), if (rule.guardRegion == null) null else load(guard)))
            }
        }
    }
    private fun point(p: ScreenPoint) = JSONArray(listOf(p.x, p.y))
    private fun readPoint(a: JSONArray) = ScreenPoint(a.getDouble(0), a.getDouble(1))
    fun encode(c: DemonConfig): String = JSONObject().apply {
        put("rounds", c.rounds)
        put("pollMs", c.pollMs)
        put("timeoutMs", c.timeoutMs)
        put("battleTimeoutMs", c.battleTimeoutMs)
        put("skillPoints", JSONArray().apply { c.skillPoints.forEach { put(point(it)) } })
        put("skillCardRegions", JSONArray().apply { c.skillCardRegions.forEach { r -> put(JSONArray(listOf(r.left, r.top, r.right, r.bottom))) } })
        put("skillPriority", JSONArray(c.skillPriority))
        put("skillBuild", c.skillBuild)
        put("skillFallback", c.skillFallback.name)
        c.referenceAspectRatio?.let { put("referenceAspectRatio", it) }
        put("rules", JSONArray().apply { c.rules.forEach { r -> put(JSONObject().apply {
            put("screen", r.screen.name)
            put("region", JSONArray(listOf(r.region.left, r.region.top, r.region.right, r.region.bottom)))
            put("threshold", r.threshold)
            put("verticalSearch", r.verticalSearch)
            r.guardRegion?.let { g ->
                put("guardRegion", JSONArray(listOf(g.left, g.top, g.right, g.bottom)))
                put("guardThreshold", r.guardThreshold)
            }
            r.tap?.let { put("tap", point(it)) }
        }) } })
    }.toString(2)
    fun decode(text: String): DemonConfig {
        val o = JSONObject(text)
        val points = o.getJSONArray("skillPoints")
        val cardRegions = o.optJSONArray("skillCardRegions")
        val priorities = o.optJSONArray("skillPriority")
        val hasSavedSkills = priorities != null && priorities.length() > 0
        val rules = o.getJSONArray("rules")
        return DemonConfig(
            rounds = o.getInt("rounds"), pollMs = o.getLong("pollMs").coerceAtLeast(2000L),
            timeoutMs = o.getLong("timeoutMs"), battleTimeoutMs = o.getLong("battleTimeoutMs"),
            skillPoints = List(points.length()) { readPoint(points.getJSONArray(it)) },
            skillCardRegions = cardRegions?.let { array -> List(array.length()) { i ->
                val r = array.getJSONArray(i)
                ScreenRegion(r.getDouble(0), r.getDouble(1), r.getDouble(2), r.getDouble(3))
            } } ?: DemonConfig().skillCardRegions,
            skillPriority = if (hasSavedSkills) List(priorities!!.length()) { priorities.getString(it) } else SkillBuilds.hurricane.priority,
            skillBuild = if (hasSavedSkills) o.optString("skillBuild", SkillBuilds.CUSTOM) else SkillBuilds.HURRICANE,
            skillFallback = SkillFallback.valueOf(o.optString("skillFallback", SkillFallback.RANDOM.name)),
            referenceAspectRatio = if (o.has("referenceAspectRatio")) o.getDouble("referenceAspectRatio") else null,
            rules = List(rules.length()) { index ->
                val r = rules.getJSONObject(index)
                val a = r.getJSONArray("region")
                ScreenRule(DemonScreen.valueOf(r.getString("screen")),
                    ScreenRegion(a.getDouble(0), a.getDouble(1), a.getDouble(2), a.getDouble(3)),
                    r.getDouble("threshold"), r.optJSONArray("tap")?.let(::readPoint),
                    r.optJSONArray("guardRegion")?.let { g -> ScreenRegion(g.getDouble(0), g.getDouble(1), g.getDouble(2), g.getDouble(3)) },
                    r.optDouble("guardThreshold", .94), r.optBoolean("verticalSearch", false))
            }
        ).also { it.validate() }
    }
}

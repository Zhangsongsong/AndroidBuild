package com.zasko.apppoint.automation

/** Coordinates are fractions of the captured display, independent of its pixel resolution. */
data class ScreenPoint(val x: Double, val y: Double) {
    fun validate() = require(x in 0.0..1.0 && y in 0.0..1.0) { "点击坐标必须在 0~1 之间" }
}

data class ScreenRegion(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    fun validate() = require(left >= 0 && top >= 0 && right <= 1 && bottom <= 1 && left < right && top < bottom) {
        "识别区域必须满足 0≤left<right≤1、0≤top<bottom≤1"
    }
}

enum class DemonScreen(val title: String) {
    HOME("游戏首页"), CHALLENGE("挑战列表"), ENTRANCE("封魔之地入口"),
    BATTLE("自动战斗"), SKILLS("三选一技能"), SETTLEMENT("胜利结算"), STOP("失败或次数不足")
}

data class ScreenRule(
    val screen: DemonScreen,
    val region: ScreenRegion,
    val threshold: Double = 0.94,
    val tap: ScreenPoint? = null,
    val guardRegion: ScreenRegion? = null,
    val guardThreshold: Double = 0.94,
    val verticalSearch: Boolean = false
) {
    val requiresTemplate: Boolean get() = !(screen == DemonScreen.ENTRANCE && verticalSearch)

    fun shifted(offset: Double): ScreenRule? {
        fun move(r: ScreenRegion) = r.copy(top = r.top + offset, bottom = r.bottom + offset)
        val result = copy(region = move(region), guardRegion = guardRegion?.let(::move),
            tap = tap?.let { it.copy(y = it.y + offset) })
        return result.takeIf {
            it.region.top >= 0 && it.region.bottom <= 1 &&
                (it.guardRegion?.top ?: 0.0) >= 0 && (it.guardRegion?.bottom ?: 1.0) <= 1 &&
                (it.tap?.y ?: 0.5) in 0.0..1.0
        }
    }
}

data class DemonConfig(
    val rounds: Int = 0, // 0 means unlimited rounds.
    val pollMs: Long = 2000,
    val timeoutMs: Long = 120_000,
    val battleTimeoutMs: Long = 600_000,
    val skillPoints: List<ScreenPoint> = listOf(ScreenPoint(.25, .55), ScreenPoint(.5, .55), ScreenPoint(.75, .55)),
    val skillCardRegions: List<ScreenRegion> = listOf(
        ScreenRegion(.05, .35, .34, .72), ScreenRegion(.355, .35, .645, .72), ScreenRegion(.66, .35, .95, .72)
    ),
    val skillPriority: List<String> = SkillBuilds.hurricane.priority,
    val skillBuild: String = SkillBuilds.HURRICANE,
    val skillFallback: SkillFallback = SkillFallback.RANDOM,
    val referenceAspectRatio: Double? = null,
    val rules: List<ScreenRule> = defaults()
) {
    fun withSkillPriority(priority: List<String>, build: String, fallback: SkillFallback): DemonConfig {
        require(priority.isNotEmpty()) { "请至少配置一个技能" }
        return copy(skillPriority = priority.toList(), skillBuild = build, skillFallback = fallback).also { it.validate() }
    }

    fun matchesDisplay(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        val reference = referenceAspectRatio ?: return true
        return kotlin.math.abs(width.toDouble() / height / reference - 1.0) <= .03
    }

    fun validate() {
        require(rounds in 0..10000) { "运行轮数必须在 0~10000，0 表示无限" }
        require(pollMs in 2000..10000) { "识别间隔必须在 2000~10000 毫秒" }
        require(timeoutMs in 10000..600000 && battleTimeoutMs in 10000..3600000) { "界面超时 10~600 秒，战斗超时 10~3600 秒" }
        require(skillPoints.size == 3) { "必须配置三个技能卡点击位置" }
        skillPoints.forEach { it.validate() }
        require(skillCardRegions.size == 3) { "必须配置三个技能卡识别区域" }
        skillCardRegions.forEach { it.validate() }
        SkillSelector.validatePriority(skillPriority)
        require(skillBuild in setOf(SkillBuilds.CUSTOM, SkillBuilds.HURRICANE)) { "未知流派" }
        referenceAspectRatio?.let { require(it.isFinite() && it > 0) { "参考屏幕比例无效" } }
        require(rules.map { it.screen }.toSet().size == rules.size) { "每个界面只能配置一条规则" }
        require(rules.map { it.screen }.containsAll(listOf(DemonScreen.ENTRANCE, DemonScreen.BATTLE, DemonScreen.SKILLS, DemonScreen.SETTLEMENT))) { "缺少入口、战斗、选卡或结算规则" }
        rules.forEach {
            require(!it.verticalSearch || it.screen == DemonScreen.ENTRANCE) { "仅入口支持随卡片纵向定位" }
            it.region.validate()
            it.guardRegion?.validate()
            require(it.guardThreshold in .8..1.0) { "辅助识别阈值必须在 0.8~1" }
            require(it.threshold in .8..1.0) { "识别阈值必须在 0.8~1" }
            it.tap?.validate()
            if (it.screen in listOf(DemonScreen.HOME, DemonScreen.CHALLENGE, DemonScreen.ENTRANCE, DemonScreen.SETTLEMENT)) {
                require(it.tap != null) { "${it.screen.title}缺少点击位置" }
            }
        }
    }

    companion object {
        // Calibration placeholders, not coordinates verified against a particular game version.
        fun defaults() = DemonScreen.entries.map { screen ->
            ScreenRule(screen, ScreenRegion(.1, .1, .9, .3), tap = when (screen) {
                DemonScreen.HOME -> ScreenPoint(.5, .9)
                DemonScreen.CHALLENGE -> ScreenPoint(.5, .5)
                DemonScreen.ENTRANCE -> ScreenPoint(.5, .8)
                DemonScreen.SETTLEMENT -> ScreenPoint(.5, .8)
                else -> null
            })
        }
    }
}

sealed class FlowDecision {
    data class Tap(val point: ScreenPoint, val label: String) : FlowDecision()
    data class Halt(val message: String, val resumable: Boolean = false) : FlowDecision()
    data object Wait : FlowDecision()
}

/** Pure state machine: an action cannot repeat until its source screen disappears. */
class DemonFlow(private val config: DemonConfig, startedAt: Long, private val random: kotlin.random.Random = kotlin.random.Random.Default) {
    var completedRounds = 0
        private set
    private var allowed = setOf(DemonScreen.HOME, DemonScreen.CHALLENGE, DemonScreen.ENTRANCE, DemonScreen.BATTLE, DemonScreen.SKILLS)
    private var latched: DemonScreen? = null
    private var candidate: DemonScreen? = null
    private var stableCount = 0
    private var deadline = startedAt + config.timeoutMs
    private var battleDeadline: Long? = null
    private var skillCandidate: List<String>? = null
    private var skillStableCount = 0
    private var pendingSkill: String? = null
    private val selectedSkills = mutableListOf<String>()
    val confirmedSkills: List<String> get() = selectedSkills.toList()
    private var awaitingReturn = false

    fun resumeAt(now: Long) {
        deadline = now + config.timeoutMs
        if (battleDeadline != null) {
            battleDeadline = now + config.battleTimeoutMs
            deadline = battleDeadline!!
        }
        candidate = null
        stableCount = 0
        skillCandidate = null
        skillStableCount = 0
    }

    fun observe(screen: DemonScreen?, now: Long, recognizedSkills: List<String> = emptyList()): FlowDecision {
        if (config.rounds > 0 && completedRounds >= config.rounds) return FlowDecision.Halt("已完成 $completedRounds 轮")
        if (screen == DemonScreen.STOP) return FlowDecision.Halt("识别到失败或次数不足，请检查游戏")
        if (now >= deadline) return FlowDecision.Halt("等待界面超时，已停止")
        if (battleDeadline?.let { now >= it } == true) return FlowDecision.Halt("战斗超时，已停止")
        if (screen == candidate && screen != null) stableCount++ else {
            candidate = screen
            stableCount = if (screen == null) 0 else 1
        }
        if (screen == DemonScreen.SKILLS) {
            val names = recognizedSkills.map(SkillSelector::normalize)
            skillStableCount = if (names == skillCandidate) skillStableCount + 1 else 1
            skillCandidate = names
        } else {
            skillCandidate = null
            skillStableCount = 0
        }
        if (stableCount < 2 || screen == null) return FlowDecision.Wait
        if (latched == screen) return FlowDecision.Wait
        // Unknown frames do not release the latch: a fading/animated dialog must not be clicked twice.
        if (screen !in allowed) return FlowDecision.Wait
        if (screen == DemonScreen.BATTLE || screen == DemonScreen.SETTLEMENT) {
            pendingSkill?.let { selectedSkills.add(it) }
            pendingSkill = null
        }
        latched = null
        if (screen == DemonScreen.ENTRANCE && awaitingReturn) {
            awaitingReturn = false
            completedRounds++
            battleDeadline = null
            if (config.rounds > 0 && completedRounds >= config.rounds) return FlowDecision.Halt("已完成 $completedRounds 轮")
        }
        if (screen == DemonScreen.BATTLE || screen == DemonScreen.SKILLS) {
            if (battleDeadline == null) battleDeadline = now + config.battleTimeoutMs
            deadline = battleDeadline!!
        } else deadline = now + config.timeoutMs
        return when (screen) {
            DemonScreen.HOME -> click(screen,
                if (config.rules.any { it.screen == DemonScreen.CHALLENGE }) setOf(DemonScreen.CHALLENGE, DemonScreen.ENTRANCE)
                else setOf(DemonScreen.ENTRANCE), config.rules.first { it.screen == screen }.tap!!)
            DemonScreen.CHALLENGE -> click(screen, setOf(DemonScreen.ENTRANCE), config.rules.first { it.screen == screen }.tap!!)
            DemonScreen.ENTRANCE -> {
                selectedSkills.clear()
                pendingSkill = null
                battleDeadline = now + config.battleTimeoutMs
                deadline = battleDeadline!!
                click(screen, setOf(DemonScreen.BATTLE, DemonScreen.SKILLS, DemonScreen.SETTLEMENT), config.rules.first { it.screen == screen }.tap!!)
            }
            DemonScreen.BATTLE -> {
                allowed = setOf(DemonScreen.BATTLE, DemonScreen.SKILLS, DemonScreen.SETTLEMENT)
                FlowDecision.Wait
            }
            DemonScreen.SKILLS -> {
                if (skillStableCount < 2) return FlowDecision.Wait
                if (recognizedSkills.size != 3 || recognizedSkills.any { SkillSelector.normalize(it).isEmpty() }) {
                    return FlowDecision.Halt("技能名称识别不完整，已暂停", resumable = true)
                }
                val preferred = SkillSelector.select(recognizedSkills, config.skillPriority, config.skillBuild, selectedSkills)
                val card = preferred ?: SkillSelector.fallback(recognizedSkills, config.skillBuild, selectedSkills, config.skillFallback, random) ?: run {
                    return FlowDecision.Halt("未匹配可选优先级技能，已暂停：${recognizedSkills.joinToString(" / ")}", resumable = true)
                }
                pendingSkill = recognizedSkills[card - 1]
                val decision = click(screen, setOf(DemonScreen.BATTLE, DemonScreen.SETTLEMENT), config.skillPoints[card - 1])
                (decision as FlowDecision.Tap).copy(label =
                    "${if (preferred == null) (if (config.skillFallback == SkillFallback.RANDOM) "未匹配优先级，随机选第 $card 张" else "未匹配优先级，选左卡") else "优先选择第 $card 张"}：${recognizedSkills[card - 1]}")
            }
            DemonScreen.SETTLEMENT -> {
                awaitingReturn = true
                battleDeadline = null
                deadline = now + config.timeoutMs
                click(screen, setOf(DemonScreen.HOME, DemonScreen.ENTRANCE), config.rules.first { it.screen == screen }.tap!!)
            }
            DemonScreen.STOP -> FlowDecision.Halt("已停止")
        }
    }

    private fun click(screen: DemonScreen, next: Set<DemonScreen>, point: ScreenPoint): FlowDecision {
        latched = screen
        allowed = next
        return FlowDecision.Tap(point, screen.title)
    }
}

/** RGB mean absolute error on a uniformly sampled, fixed region of the screen. */
object TemplateScore {
    fun similarity(a: IntArray, b: IntArray): Double {
        require(a.isNotEmpty() && a.size == b.size)
        var error = 0L
        for (i in a.indices) for (shift in intArrayOf(16, 8, 0)) {
            error += kotlin.math.abs(((a[i] shr shift) and 255) - ((b[i] shr shift) and 255))
        }
        return 1.0 - error.toDouble() / (a.size * 3.0 * 255)
    }
}

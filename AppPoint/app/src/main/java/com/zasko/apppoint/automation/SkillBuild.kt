package com.zasko.apppoint.automation

import java.text.Normalizer
import java.util.Locale

data class SkillBuild(val id: String, val title: String, val priority: List<String>)
enum class SkillFallback { RANDOM, LEFTMOST, PAUSE }

object SkillBuilds {
    const val CUSTOM = "custom"
    const val HURRICANE = "hurricane"
    const val FIREBALL = "fireball"

    // Transcribed from the user's skill graph. This is an editable example, not an optimal-build claim.
    val hurricaneSkills = listOf(
        "学习飓风|学习飓风术|解锁飓风术|飓风术", "风暴聚集", "多重飓风II|多重飓风Ⅱ|多重飓风2",
        "多重飓风", "风力回旋", "乱流加剧", "乱流", "飓风增幅",
        "贯通之风", "风阻", "强袭飓风", "真空塌陷", "真空"
    )
    val tornadoSkills = listOf(
        "学习龙卷风|解锁龙卷风|龙卷风", "风暴汇聚", "双重龙卷", "风力拉满",
        "龙卷风延续", "龙卷扩张", "强力龙卷", "龙卷风伤害增幅", "强袭龙卷", "闪电龙卷"
    )
    val frostNovaSkills = listOf(
        "学习冰霜新星|解锁冰霜新星|冰霜新星", "绝对零度", "寒霜之心", "连环霜爆",
        "极寒新星", "新星扩张", "新星伤害增幅", "极寒领域", "刺骨寒霜", "冰晶碎裂", "寒冰法术精通"
    )
    // User-requested order: every hurricane card before tornado, then frost nova.
    // Within each family this example order remains editable in the UI.
    val hurricane = SkillBuild(HURRICANE, "飓风流",
        hurricaneSkills + tornadoSkills + frostNovaSkills)
    val fireballSkills = listOf(
        "学习火球术|学习火球|解锁火球术|火球术", "火球术爆发|火球爆发", "爆破", "炎爆术", "渗透",
        "爆燃", "猛烈燃烧", "传火", "火球冲击", "烈焰火球", "冲击", "火花", "连发",
        "学习连珠火球|解锁连珠火球|连珠火球"
    )
    val fireball = SkillBuild(FIREBALL, "火球流", fireballSkills)
    val builtIns = listOf(hurricane, fireball)

    // Only explicit exclusions in the supplied hurricane/tornado charts are enforced here.
    // Upgrade availability/prerequisites are controlled by the game, not guessed from OCR.
    fun excludes(build: String, name: String, selected: List<String>): Boolean {
        if (build != HURRICANE) return false
        val key = SkillSelector.normalize(name)
        val owned = selected.map(SkillSelector::normalize)
        return (key in setOf("真空", "真空塌陷") && "强袭飓风" in owned) ||
            (key == "强袭飓风" && owned.any { it in setOf("真空", "真空塌陷") }) ||
            (key == "双重龙卷" && "闪电龙卷" in owned) ||
            (key == "闪电龙卷" && "双重龙卷" in owned)
    }
}

object SkillSelector {
    /** Normalize ML Kit's common Traditional/shape confusions before matching a saved name. */
    fun normalize(value: String): String {
        val compact = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
            .replace('巻', '卷')
            .replace('増', '增')
        return when {
            compact == "学习臭术飞弹" || compact == "解锁臭术飞弹" -> compact.replace("臭术", "奥术")
            compact == "炎燥术" || compact == "学习炎燥术" || compact == "解锁炎燥术" -> compact.replace("燥", "爆")
            compact.contains("龙卷") && compact.contains("打张") -> compact.replace("打张", "扩张")
            else -> compact
        }
    }

    fun validatePriority(priority: List<String>) {
        val groups = priority.map { entry ->
            entry.split('|').map { alias -> normalize(alias).also {
                require(it.length >= 2) { "技能名称或别名至少两个字，且不能为空" }
            } }.toSet()
        }
        val names = groups.flatMap { it }
        require(names.distinct().size == names.size) { "不同优先级存在重复技能或别名" }
    }

    /** Return the 1-based card position, never the rank in the user's priority list. */
    fun select(names: List<String>, priority: List<String>, build: String = SkillBuilds.CUSTOM, selected: List<String> = emptyList()): Int? {
        if (names.size != 3 || priority.isEmpty()) return null
        val normalized = names.map(::normalize)
        // An unreadable card might be the best one: do not choose using incomplete information.
        if (normalized.any { it.isEmpty() }) return null
        return priority.firstNotNullOfOrNull { entry ->
            val aliases = entry.split('|').map(::normalize).filter { it.length >= 2 }
            val card = normalized.indices.firstOrNull {
                normalized[it] in aliases && !SkillBuilds.excludes(build, names[it], selected)
            }
            card?.plus(1)
        }
    }

    fun fallback(names: List<String>, build: String, selected: List<String>, mode: SkillFallback,
        random: kotlin.random.Random = kotlin.random.Random.Default): Int? {
        if (names.size != 3 || names.any { normalize(it).isEmpty() }) return null
        val valid = names.indices.filter { !SkillBuilds.excludes(build, names[it], selected) }
        return when (mode) {
            SkillFallback.RANDOM -> valid.takeIf { it.isNotEmpty() }?.random(random)?.plus(1)
            SkillFallback.LEFTMOST -> if (0 in valid) 1 else null
            SkillFallback.PAUSE -> null
        }
    }
}

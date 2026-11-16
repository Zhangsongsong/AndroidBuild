package com.zasko.apppoint.automation

import android.content.Context

/** Layout measured from the user supplied 544x1200 recording, 2026-11-16. */
object DemonVideoPreset {
    private const val assets = "demon_video_20261116"

    fun config() = DemonConfig(
        pollMs = 500L,
        referenceAspectRatio = 544.0 / 1200,
        skillBuild = SkillBuilds.HURRICANE,
        skillPriority = SkillBuilds.hurricane.priority,
        skillFallback = SkillFallback.RANDOM,
        skillPoints = listOf(ScreenPoint(.18, .50), ScreenPoint(.50, .50), ScreenPoint(.82, .50)),
        skillCardRegions = listOf(
            ScreenRegion(.075, .372, .291, .400),
            ScreenRegion(.395, .372, .612, .400),
            ScreenRegion(.718, .372, .935, .400)
        ),
        // The video starts on the challenge list. ENTRANCE is the visible card in that list;
        // no separate challenge-card tap or guessed home-screen action is necessary.
        rules = listOf(
            ScreenRule(DemonScreen.ENTRANCE, ScreenRegion(.16, .385, .78, .433), .94, ScreenPoint(.50, .594)),
            ScreenRule(DemonScreen.BATTLE, ScreenRegion(.075, .045, .146, .074), .86,
                guardRegion = ScreenRegion(.46, .808, .54, .824), guardThreshold = .86),
            ScreenRule(DemonScreen.SKILLS, ScreenRegion(.072, .62, .295, .639), .94,
                guardRegion = ScreenRegion(.717, .62, .94, .639), guardThreshold = .94),
            ScreenRule(DemonScreen.SETTLEMENT, ScreenRegion(.383, .782, .62, .81), .94, ScreenPoint(.50, .796))
        )
    )

    fun install(context: Context, pkg: String) {
        val saved = DemonStore.load(context, pkg)
        val config = calibratedWithSavedSkills(saved).also { it.validate() }
        config.rules.forEach { rule ->
            context.assets.open("$assets/${rule.screen.name}.png").use { source ->
                DemonStore.templateFile(context, pkg, rule.screen).outputStream().use { source.copyTo(it) }
            }
            if (rule.guardRegion != null) {
                context.assets.open("$assets/${rule.screen.name}_guard.png").use { source ->
                    DemonStore.guardFile(context, pkg, rule.screen).outputStream().use { source.copyTo(it) }
                }
            }
        }
        DemonStore.save(context, pkg, config)
        DemonRunner.stop("录像布局已加载，请在封魔之地入口先试跑一轮")
    }

    fun calibratedWithSavedSkills(saved: DemonConfig): DemonConfig = config().let { calibrated ->
        if (saved.skillPriority.isEmpty()) calibrated
        else calibrated.withSkillPriority(saved.skillPriority, saved.skillBuild, saved.skillFallback)
    }
}

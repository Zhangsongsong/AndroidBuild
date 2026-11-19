package com.zasko.apppoint.automation

import android.content.Context

/** Battle layout from the supplied recording; entrance calibrated on the connected phone. */
object DemonVideoPreset {
    const val TARGET_PACKAGE = "com.openew.game.td"
    private const val assets = "demon_video_20261116"

    fun config() = DemonConfig(
        pollMs = 2000L,
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
            // After settlement the game can return to the battle homepage. Use the Challenge tab.
            ScreenRule(DemonScreen.HOME, ScreenRegion(.025, .120, .118, .165), .94, ScreenPoint(.9, .965),
                guardRegion = ScreenRegion(.165, .12, .275, .165), guardThreshold = .94),
            // Locate the card text and calculate the button proportionally; no entrance PNG required.
            ScreenRule(DemonScreen.ENTRANCE, ScreenRegion(.27, .268, .74, .319), .94, ScreenPoint(.50, .482),
                guardRegion = ScreenRegion(.43, .485, .61, .508), guardThreshold = .94, verticalSearch = true),
            ScreenRule(DemonScreen.BATTLE, ScreenRegion(.062, .040, .154, .079), .94,
                guardRegion = ScreenRegion(.176, .043, .269, .080), guardThreshold = .94),
            ScreenRule(DemonScreen.SKILLS, ScreenRegion(.072, .629, .295, .641), .94,
                guardRegion = ScreenRegion(.717, .629, .94, .641), guardThreshold = .94),
            ScreenRule(DemonScreen.SETTLEMENT, ScreenRegion(.383, .782, .62, .81), .94, ScreenPoint(.50, .796))
        )
    )

    fun install(context: Context, pkg: String) {
        val saved = DemonStore.load(context, pkg)
        val config = calibratedWithSavedSkills(saved).also { it.validate() }
        installTemplates(context, pkg, config)
        DemonStore.save(context, pkg, config)
        DemonRunner.stop("录像布局已加载，默认无限轮，请在封魔之地入口开始运行")
    }

    fun installTemplates(context: Context, pkg: String, config: DemonConfig = config()) {
        config.rules.filter { it.requiresTemplate }.forEach { rule ->
            context.assets.open("$assets/${rule.screen.name}.png").use { source ->
                DemonStore.templateFile(context, pkg, rule.screen).outputStream().use { source.copyTo(it) }
            }
            if (rule.guardRegion != null) {
                context.assets.open("$assets/${rule.screen.name}_guard.png").use { source ->
                    DemonStore.guardFile(context, pkg, rule.screen).outputStream().use { source.copyTo(it) }
                }
            }
        }
    }

    fun calibratedWithSavedSkills(saved: DemonConfig): DemonConfig = config().let { calibrated ->
        if (saved.skillPriority.isEmpty()) calibrated
        else calibrated.withSkillPriority(saved.skillPriority, saved.skillBuild, saved.skillFallback)
    }
}

package com.zasko.apppoint.automation

import org.junit.Assert.*
import org.junit.Test

class DemonFlowTest {
    private var now = 1000L
    private fun stable(flow: DemonFlow, screen: DemonScreen, skills: List<String> = listOf("雷电", "火球", "冰锥")): FlowDecision {
        flow.observe(screen, ++now, skills)
        return flow.observe(screen, ++now, skills)
    }

    @Test fun completesOnlyAfterConfirmedReturnAndSelectsConfiguredCards() {
        val config = DemonConfig(rounds = 1, skillPriority = listOf("冰锥", "雷电"))
        val flow = DemonFlow(config, now)
        assertTrue(stable(flow, DemonScreen.HOME) is FlowDecision.Tap)
        assertTrue(stable(flow, DemonScreen.CHALLENGE) is FlowDecision.Tap)
        assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Tap)
        stable(flow, DemonScreen.BATTLE)
        assertEquals(config.skillPoints[2], (stable(flow, DemonScreen.SKILLS, listOf("雷电", "火球", "冰锥")) as FlowDecision.Tap).point)
        stable(flow, DemonScreen.BATTLE)
        assertEquals(config.skillPoints[0], (stable(flow, DemonScreen.SKILLS, listOf("雷电", "火球", "未知")) as FlowDecision.Tap).point)
        assertTrue(stable(flow, DemonScreen.SETTLEMENT) is FlowDecision.Tap)
        assertEquals(0, flow.completedRounds)
        assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Halt)
        assertEquals(1, flow.completedRounds)
    }

    @Test fun ignoresAnimationAndDoesNotRepeatClickOnSameDialog() {
        val flow = DemonFlow(DemonConfig(skillPriority = listOf("雷电")), now)
        assertTrue(stable(flow, DemonScreen.SKILLS) is FlowDecision.Tap)
        repeat(5) { assertEquals(FlowDecision.Wait, flow.observe(null, ++now)) }
        assertEquals(FlowDecision.Wait, stable(flow, DemonScreen.SKILLS))
        stable(flow, DemonScreen.BATTLE)
        assertTrue(stable(flow, DemonScreen.SKILLS) is FlowDecision.Tap)
    }

    @Test fun dismissesEliteRewardOverlayWithoutChoosingASkillAndReturnsToBattle() {
        val flow = DemonFlow(DemonConfig(), now)
        stable(flow, DemonScreen.ENTRANCE)
        stable(flow, DemonScreen.BATTLE)
        val blankPoint = ScreenPoint(.5, .84)
        assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.DISMISS_OVERLAY, ++now, dismissPoint = blankPoint))
        assertEquals(FlowDecision.Tap(blankPoint, "关闭提示界面"),
            flow.observe(DemonScreen.DISMISS_OVERLAY, ++now, dismissPoint = blankPoint))
        assertEquals(FlowDecision.Wait, stable(flow, DemonScreen.DISMISS_OVERLAY))
        assertEquals(FlowDecision.Wait, stable(flow, DemonScreen.BATTLE))
        assertEquals(0, flow.completedRounds)
    }

    @Test fun rejectsSingleFrameAndOutOfOrderSettlement() {
        val flow = DemonFlow(DemonConfig(skillPriority = listOf("雷电")), now)
        assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.ENTRANCE, ++now))
        assertEquals(FlowDecision.Wait, stable(flow, DemonScreen.SETTLEMENT))
        assertEquals(0, flow.completedRounds)
    }

    @Test fun dismissesRouletteWhenStartingOnItsScreen() {
        val flow = DemonFlow(DemonConfig(), now)
        val point = ScreenPoint(.50, .855)
        assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.DISMISS_OVERLAY, ++now, dismissPoint = point))
        assertEquals(FlowDecision.Tap(point, "关闭提示界面"),
            flow.observe(DemonScreen.DISMISS_OVERLAY, ++now, dismissPoint = point))
        assertEquals(FlowDecision.Wait, stable(flow, DemonScreen.DISMISS_OVERLAY))
        assertEquals(0, flow.completedRounds)
    }

    @Test fun dismissesRouletteImmediatelyAfterSkillSelectionWithoutABattleFrame() {
        val flow = DemonFlow(DemonConfig(skillPriority = listOf("雷电")), now)
        assertTrue(stable(flow, DemonScreen.SKILLS) is FlowDecision.Tap)
        val point = ScreenPoint(.50, .855)
        flow.observe(DemonScreen.DISMISS_OVERLAY, ++now, dismissPoint = point)
        assertEquals(FlowDecision.Tap(point, "关闭提示界面"),
            flow.observe(DemonScreen.DISMISS_OVERLAY, ++now, dismissPoint = point))
        assertEquals(listOf("雷电"), flow.confirmedSkills)
        assertEquals(FlowDecision.Wait, stable(flow, DemonScreen.DISMISS_OVERLAY))
        stable(flow, DemonScreen.BATTLE)
        assertTrue(stable(flow, DemonScreen.SKILLS) is FlowDecision.Tap)
    }

    @Test fun dismissesRouletteAfterEntranceWithoutABattleFrame() {
        val flow = DemonFlow(DemonConfig(), now)
        assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Tap)
        val point = ScreenPoint(.50, .855)
        flow.observe(DemonScreen.DISMISS_OVERLAY, ++now, dismissPoint = point)
        assertEquals(FlowDecision.Tap(point, "关闭提示界面"),
            flow.observe(DemonScreen.DISMISS_OVERLAY, ++now, dismissPoint = point))
    }

    @Test fun confirmsTheSameEntranceAcrossOneMissAndUsesItsDetectedButton() {
        val flow = DemonFlow(DemonConfig(), now)
        val point = ScreenPoint(.494, .584)
        assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.ENTRANCE, now + 1, entrancePoint = point))
        assertEquals(FlowDecision.Wait, flow.observe(null, now + 2001))
        assertEquals(FlowDecision.Tap(point, "封魔之地入口"),
            flow.observe(DemonScreen.ENTRANCE, now + 4001, entrancePoint = point))
        assertEquals(FlowDecision.Wait, flow.observe(null, now + 6001))
        assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.ENTRANCE, now + 8001, entrancePoint = point))
        assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.ENTRANCE, now + 10001, entrancePoint = point))
    }

    @Test fun clicksPixelLocatedEntranceOnItsFirstScreenshotWithoutRepeating() {
        val flow = DemonFlow(DemonConfig(), now)
        val point = ScreenPoint(.494, .584)
        assertEquals(FlowDecision.Tap(point, "封魔之地入口"),
            flow.observe(DemonScreen.ENTRANCE, now + 1, entrancePoint = point, entrancePixelConfirmed = true))
        assertEquals(FlowDecision.Wait,
            flow.observe(DemonScreen.ENTRANCE, now + 2001, entrancePoint = point, entrancePixelConfirmed = true))
        assertEquals(FlowDecision.Wait, flow.observe(null, now + 4001))
        assertEquals(FlowDecision.Wait,
            flow.observe(DemonScreen.ENTRANCE, now + 6001, entrancePoint = point, entrancePixelConfirmed = true))
    }

    @Test fun rejectsStaleOrMovedEntranceEvidenceAndMultipleMisses() {
        val point = ScreenPoint(.494, .584)
        val moved = point.copy(y = .4)
        val flow = DemonFlow(DemonConfig(), now)
        flow.observe(DemonScreen.ENTRANCE, now + 1, entrancePoint = point)
        flow.observe(null, now + 2001)
        assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.ENTRANCE, now + 4001, entrancePoint = moved))
        val stale = DemonFlow(DemonConfig(), now)
        stale.observe(DemonScreen.ENTRANCE, now + 1, entrancePoint = point)
        stale.observe(null, now + 2001)
        assertEquals(FlowDecision.Wait, stale.observe(DemonScreen.ENTRANCE, now + 20001, entrancePoint = point))
        val missed = DemonFlow(DemonConfig(), now)
        missed.observe(DemonScreen.ENTRANCE, now + 1, entrancePoint = point)
        missed.observe(null, now + 2001)
        missed.observe(null, now + 4001)
        assertEquals(FlowDecision.Wait, missed.observe(DemonScreen.ENTRANCE, now + 6001, entrancePoint = point))
    }

    @Test fun timeoutStopsUnknownScreensAndStuckDialogs() {
        val config = DemonConfig(skillPriority = listOf("雷电"))
        val flow = DemonFlow(config, now)
        assertTrue(flow.observe(null, now + config.timeoutMs) is FlowDecision.Halt)
        val stuck = DemonFlow(config, now)
        stable(stuck, DemonScreen.SKILLS)
        assertTrue(stuck.observe(DemonScreen.SKILLS, now + config.battleTimeoutMs) is FlowDecision.Halt)
    }

    @Test fun failureStopsWithoutCountingRound() {
        val flow = DemonFlow(DemonConfig(skillPriority = listOf("雷电")), now)
        stable(flow, DemonScreen.BATTLE)
        assertTrue(flow.observe(DemonScreen.STOP, ++now) is FlowDecision.Halt)
        assertEquals(0, flow.completedRounds)
    }

    @Test fun preservesPriorityBetweenRoundsAndStopsAtLimit() {
        val config = DemonConfig(rounds = 2, skillPriority = listOf("火球", "冰锥"))
        val flow = DemonFlow(config, now)
        stable(flow, DemonScreen.ENTRANCE)
        assertEquals(config.skillPoints[1], (stable(flow, DemonScreen.SKILLS, listOf("雷电", "火球", "冰锥")) as FlowDecision.Tap).point)
        stable(flow, DemonScreen.SETTLEMENT)
        assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Tap)
        assertEquals(1, flow.completedRounds)
        assertEquals(config.skillPoints[1], (stable(flow, DemonScreen.SKILLS, listOf("雷电", "火球", "冰锥")) as FlowDecision.Tap).point)
        stable(flow, DemonScreen.SETTLEMENT)
        assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Halt)
        assertEquals(2, flow.completedRounds)
        flow.resumeAt(++now)
        assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Halt)
    }

    @Test fun defaultConfigsRunBeyondFiniteRoundLimitWithoutStopping() {
        listOf(DemonConfig(), DemonVideoPreset.config()).forEach { config ->
            config.validate()
            assertEquals(0, config.rounds)
            val flow = DemonFlow(config, now)
            assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Tap)
            repeat(10001) { round ->
                stable(flow, DemonScreen.BATTLE)
                assertTrue(stable(flow, DemonScreen.SETTLEMENT) is FlowDecision.Tap)
                assertEquals(round, flow.completedRounds)
                assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Tap)
                assertEquals(round + 1, flow.completedRounds)
                assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.ENTRANCE, ++now))
            }
        }
    }

    @Test fun resumeKeepsLatchButExtendsTimeout() {
        val flow = DemonFlow(DemonConfig(skillPriority = listOf("雷电")), now)
        stable(flow, DemonScreen.ENTRANCE)
        now += 1_000_000
        flow.resumeAt(now)
        assertEquals(FlowDecision.Wait, stable(flow, DemonScreen.ENTRANCE))
        assertTrue(stable(flow, DemonScreen.SKILLS) is FlowDecision.Tap)
    }

    @Test fun validatesInputAndComparesTemplates() {
        DemonConfig(rounds = 0).validate()
        DemonConfig(rounds = 10000).validate()
        assertEquals(2000L, DemonConfig().pollMs)
        assertEquals(2000L, DemonVideoPreset.config().pollMs)
        assertFalse(DemonConfig.defaults().any { it.screen == DemonScreen.DISMISS_OVERLAY })
        DemonConfig(pollMs = 2000).validate()
        assertThrows(IllegalArgumentException::class.java) { DemonConfig(pollMs = 1999).validate() }
        assertThrows(IllegalArgumentException::class.java) { DemonConfig(rounds = -1).validate() }
        assertThrows(IllegalArgumentException::class.java) { DemonConfig(rounds = 10001).validate() }
        assertThrows(IllegalArgumentException::class.java) { DemonConfig(skillPriority = listOf("|")).validate() }
        assertThrows(IllegalArgumentException::class.java) { ScreenRegion(0.5, 0.0, 0.2, 1.0).validate() }
        assertThrows(IllegalArgumentException::class.java) { ScreenPoint(Double.NaN, .5).validate() }
        assertEquals(1.0, TemplateScore.similarity(intArrayOf(0x123456), intArrayOf(0x123456)), .0001)
        assertEquals(0.0, TemplateScore.similarity(intArrayOf(0), intArrayOf(0xffffff)), .0001)
    }

    @Test fun selectsHighestConfiguredPriorityAndSupportsAliases() {
        assertEquals(2, SkillSelector.select(listOf("暴风雪", "火球术", "雷电"), listOf("冰锥", "火球|火球术", "雷电")))
        assertEquals(1, SkillSelector.select(listOf("闪电链", "火球术", "冰锥术"), listOf("雷电|闪电链")))
        assertNull(SkillSelector.select(listOf("火球", "冰锥", "雷电"), emptyList()))
        assertNull(SkillSelector.select(listOf("未知技能", "未知", "未知2"), listOf("雷电", "冰锥")))
    }

    @Test fun stopsSkillChoiceWhenOcrDoesNotMatchPriority() {
        val flow = DemonFlow(DemonConfig(skillPriority = listOf("雷电")), now)
        stable(flow, DemonScreen.ENTRANCE)
        stable(flow, DemonScreen.BATTLE)
        assertTrue(stable(flow, DemonScreen.SKILLS, listOf("无法识别", "", "未知")) is FlowDecision.Halt)
    }

    @Test fun priorityRankDoesNotBecomeCardPosition() {
        assertEquals(3, SkillSelector.select(listOf("火球术", "冰锥术", "飓风术"),
            listOf("多重飓风", "风力回旋", "乱流", "风阻", "飓风术")))
    }

    @Test fun matchesOnlyNamesAndRejectsPartialOcrAndBlankCards() {
        val priority = listOf("飓风术", "多重飓风II")
        assertEquals(2, SkillSelector.select(listOf("火球术", "飓 风" + 10.toChar() + "术", "冰锥术"), priority))
        assertEquals(1, SkillSelector.select(listOf("多重飓风Ⅱ", "火球术", "冰锥术"), priority))
        assertNull(SkillSelector.select(listOf("飓风", "火球术", "冰锥术"), priority))
        assertNull(SkillSelector.select(listOf("火球术 额外释放飓风术", "火球", "冰锥"), priority))
        assertNull(SkillSelector.select(listOf("飓风术", "", "冰锥术"), priority))
    }

    @Test fun hurricanePresetSelectsRelatedSkillsWithoutConfusingTornado() {
        DemonConfig(skillPriority = SkillBuilds.hurricane.priority, skillBuild = SkillBuilds.HURRICANE).validate()
        assertEquals(2, SkillSelector.select(listOf("火球术", "多重飓风", "龙卷风"), SkillBuilds.hurricane.priority, SkillBuilds.HURRICANE))
        assertEquals(1, SkillSelector.select(listOf("龙卷风", "火球术", "冰锥术"), SkillBuilds.hurricane.priority, SkillBuilds.HURRICANE))
    }

    @Test fun filtersMutuallyExclusiveBranchesOnlyForHurricaneBuild() {
        val names = listOf("真空", "强袭飓风", "飓风增幅")
        assertEquals(3, SkillSelector.select(names, listOf("真空", "飓风增幅"), SkillBuilds.HURRICANE, listOf("强袭飓风")))
        assertEquals(3, SkillSelector.select(names, listOf("强袭飓风", "飓风增幅"), SkillBuilds.HURRICANE, listOf("真空")))
        assertEquals(1, SkillSelector.select(names, listOf("真空", "飓风增幅"), SkillBuilds.CUSTOM, listOf("强袭飓风")))
    }

    @Test fun requiresTwoConsistentNameFrames() {
        val flow = DemonFlow(DemonConfig(skillPriority = listOf("飓风术", "多重飓风")), now)
        val left = listOf("飓风术", "火球术", "冰锥术")
        val right = listOf("火球术", "冰锥术", "多重飓风")
        assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.SKILLS, ++now, left))
        assertEquals(FlowDecision.Wait, flow.observe(DemonScreen.SKILLS, ++now, right))
        assertTrue(flow.observe(DemonScreen.SKILLS, ++now, right) is FlowDecision.Tap)
    }

    @Test fun confirmsSelectionAfterReturnToBattleAndResetsHistoryPerRound() {
        val config = DemonConfig(rounds = 2, skillBuild = SkillBuilds.HURRICANE,
            skillPriority = listOf("强袭飓风", "真空", "飓风增幅"))
        val flow = DemonFlow(config, now)
        stable(flow, DemonScreen.ENTRANCE)
        stable(flow, DemonScreen.SKILLS, listOf("强袭飓风", "火球术", "冰锥术"))
        assertTrue(flow.confirmedSkills.isEmpty())
        stable(flow, DemonScreen.BATTLE)
        assertEquals(listOf("强袭飓风"), flow.confirmedSkills)
        val choice = stable(flow, DemonScreen.SKILLS, listOf("真空", "火球术", "飓风增幅")) as FlowDecision.Tap
        assertEquals(config.skillPoints[2], choice.point)
        stable(flow, DemonScreen.SETTLEMENT)
        stable(flow, DemonScreen.ENTRANCE)
        assertTrue(flow.confirmedSkills.isEmpty())
    }

    @Test fun pauseOnUnlistedNamesCanResumeAfterManualSelection() {
        val flow = DemonFlow(DemonConfig(skillPriority = listOf("飓风术"), skillFallback = SkillFallback.PAUSE), now)
        val halt = stable(flow, DemonScreen.SKILLS, listOf("火球术", "冰锥术", "雷击术")) as FlowDecision.Halt
        assertTrue(halt.resumable)
        flow.resumeAt(++now)
        stable(flow, DemonScreen.BATTLE)
        assertTrue(stable(flow, DemonScreen.SKILLS, listOf("火球术", "飓风术", "雷击术")) is FlowDecision.Tap)
    }

    @Test fun usesLeftCardFallbackOnlyForCompleteStableNames() {
        val config = DemonConfig(skillBuild = SkillBuilds.HURRICANE, skillPriority = SkillBuilds.hurricane.priority, skillFallback = SkillFallback.LEFTMOST)
        val flow = DemonFlow(config, now)
        val tap = stable(flow, DemonScreen.SKILLS, listOf("火球术", "冰锥术", "雷击术")) as FlowDecision.Tap
        assertEquals(config.skillPoints[0], tap.point)
        assertTrue(tap.label.contains("左卡"))
        val unreadable = DemonFlow(config, now)
        assertTrue(stable(unreadable, DemonScreen.SKILLS, listOf("", "飓风术", "雷击术")) is FlowDecision.Halt)
    }

    @Test fun respectsHurricaneThenTornadoThenFrostNovaFamilyOrder() {
        val priority = SkillBuilds.hurricane.priority
        assertEquals(3, SkillSelector.select(listOf("冰霜新星", "龙卷风", "飓风增幅"), priority, SkillBuilds.HURRICANE))
        assertEquals(2, SkillSelector.select(listOf("绝对零度", "强力龙卷", "火球术"), priority, SkillBuilds.HURRICANE))
        assertEquals(1, SkillSelector.select(listOf("连环霜爆", "雷击术", "火球术"), priority, SkillBuilds.HURRICANE))
    }

    @Test fun randomFallbackCanChooseAnyCardAndNeverConflictingCards() {
        val names = listOf("火球术", "雷击术", "冰锥术")
        val positions = (1..100).map { seed ->
            SkillSelector.fallback(names, SkillBuilds.HURRICANE, emptyList(), SkillFallback.RANDOM, kotlin.random.Random(seed))
        }.toSet()
        assertEquals(setOf(1, 2, 3), positions)
        val config = DemonConfig(skillBuild = SkillBuilds.HURRICANE, skillPriority = SkillBuilds.hurricane.priority)
        val flow = DemonFlow(config, now, kotlin.random.Random(5))
        val tap = stable(flow, DemonScreen.SKILLS, names) as FlowDecision.Tap
        assertTrue(tap.point in config.skillPoints)
        assertTrue(tap.label.contains("随机"))
        repeat(20) { seed ->
            assertEquals(3, SkillSelector.fallback(listOf("真空", "闪电龙卷", "火球术"),
                SkillBuilds.HURRICANE, listOf("强袭飓风", "双重龙卷"), SkillFallback.RANDOM, kotlin.random.Random(seed)))
        }
        assertNull(SkillSelector.fallback(listOf("", "火球术", "冰锥术"), SkillBuilds.HURRICANE, emptyList(), SkillFallback.RANDOM))
    }

    @Test fun videoPresetStartsDirectlyOnChallengeListAndFinishesAtSameEntrance() {
        val config = DemonVideoPreset.config().copy(rounds = 1)
        config.validate()
        assertTrue(config.rules.any { it.screen == DemonScreen.HOME })
        assertFalse(config.rules.any { it.screen == DemonScreen.CHALLENGE })
        val flow = DemonFlow(config, now)
        val enter = stable(flow, DemonScreen.ENTRANCE) as FlowDecision.Tap
        assertEquals(ScreenPoint(.50, .482), enter.point)
        stable(flow, DemonScreen.BATTLE)
        assertTrue(stable(flow, DemonScreen.SETTLEMENT) is FlowDecision.Tap)
        assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Halt)
        assertEquals(1, flow.completedRounds)
        assertTrue(config.matchesDisplay(544, 1200))
        assertTrue(config.matchesDisplay(1088, 2400))
        assertFalse(config.matchesDisplay(1200, 544))
        assertFalse(config.matchesDisplay(1080, 1920))
    }

    @Test fun entranceScrollMovesTitleEnergyGuardAndTapTogether() {
        val rule = DemonVideoPreset.config().rules.first { it.screen == DemonScreen.ENTRANCE }
        assertTrue(rule.verticalSearch)
        val shifted = rule.shifted(.08)!!
        assertEquals(rule.region.top + .08, shifted.region.top, .000001)
        assertEquals(rule.guardRegion!!.top + .08, shifted.guardRegion!!.top, .000001)
        assertEquals(rule.tap!!.y + .08, shifted.tap!!.y, .000001)
        assertNull(rule.shifted(.7))
        assertNull(rule.shifted(-.5))
    }

    @Test fun replayRecordedChoicesWithRequestedFamilyPriorities() {
        val config = DemonVideoPreset.config()
        val flow = DemonFlow(config, now)
        stable(flow, DemonScreen.ENTRANCE)
        val choices = listOf(
            listOf("学习火球术", "风暴聚集", "学习滚木") to 2,
            listOf("新星伤害增幅", "龙卷风延续", "学习电磁网") to 2,
            listOf("学习电磁网", "学习冰锥", "龙卷风伤害增幅") to 3,
            listOf("龙卷风伤害增幅", "强力龙卷", "龙卷扩张") to 3,
            listOf("双重龙卷", "连环霜爆", "学习电网") to 1,
            listOf("学习脉冲激光", "学习聚焦激光", "贯通之风") to 3,
            listOf("飓风增幅", "学习雷击", "强袭龙卷") to 1
        )
        choices.forEach { (names, expected) ->
            stable(flow, DemonScreen.BATTLE)
            val choice = stable(flow, DemonScreen.SKILLS, names) as FlowDecision.Tap
            assertEquals(config.skillPoints[expected - 1], choice.point)
            // One enlarged card animation does not cause another choice or confirm history.
            repeat(3) { assertEquals(FlowDecision.Wait, flow.observe(null, ++now)) }
        }
        assertEquals(6, flow.confirmedSkills.size)
        stable(flow, DemonScreen.BATTLE)
        assertEquals(7, flow.confirmedSkills.size)
    }

    @Test fun recognizesLearnLabelsSeenInRecording() {
        val names = listOf("学习飓风", "学习龙卷风", "学习冰霜新星")
        assertEquals(1, SkillSelector.select(names, SkillBuilds.hurricane.priority, SkillBuilds.HURRICANE))
        assertEquals(2, SkillSelector.select(listOf("学习火球术", "学习龙卷风", "学习冰霜新星"), SkillBuilds.hurricane.priority))
    }

    @Test fun normalizesCommonOcrConfusionsBeforePriorityMatching() {
        val priority = listOf("龙卷扩张")
        assertEquals(2, SkillSelector.select(listOf("学习臭术飞弹", "龙巻打张", "学习闪电链"), priority))
        assertEquals("龙卷扩张", SkillSelector.normalize("龙巻打张"))
        assertEquals("学习奥术飞弹", SkillSelector.normalize("学习臭术飞弹"))
    }

    @Test fun newConfigUsesHurricanePriorityWithoutSetup() {
        val config = DemonConfig()
        assertEquals(SkillBuilds.HURRICANE, config.skillBuild)
        assertEquals(SkillBuilds.hurricane.priority, config.skillPriority)
        assertEquals(2, SkillSelector.select(listOf("新星伤害增幅", "飓风增幅", "龙卷扩张"), config.skillPriority))
    }

    @Test fun settlementReturnsThroughChallengeTabWithoutStartingExtraRound() {
        val config = DemonVideoPreset.config().copy(rounds = 1)
        val flow = DemonFlow(config, now)
        val home = stable(flow, DemonScreen.HOME) as FlowDecision.Tap
        assertEquals(ScreenPoint(.9, .965), home.point)
        assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Tap)
        stable(flow, DemonScreen.BATTLE)
        stable(flow, DemonScreen.SETTLEMENT)
        assertTrue(stable(flow, DemonScreen.HOME) is FlowDecision.Tap)
        assertEquals(0, flow.completedRounds)
        assertTrue(stable(flow, DemonScreen.ENTRANCE) is FlowDecision.Halt)
        assertEquals(1, flow.completedRounds)
    }
}

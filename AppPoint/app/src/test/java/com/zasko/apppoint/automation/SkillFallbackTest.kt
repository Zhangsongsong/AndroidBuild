package com.zasko.apppoint.automation

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class SkillFallbackTest {
    @Test fun sixthLoggedChoiceAlwaysPrefersTheUpgradeInAnyCardPosition() {
        val choices = listOf(
            listOf("连环霸爆燥", "学习雪击", "学习飞轮鋸") to 1,
            listOf("学习雪击", "连环霸爆燥", "学习飞轮鋸") to 2,
            listOf("学习雪击", "学习飞轮鋸", "连环霸爆燥") to 3
        )
        choices.forEach { (cards, expected) ->
            assertNull(SkillSelector.select(cards, SkillBuilds.hurricane.priority, SkillBuilds.HURRICANE))
            repeat(40) { seed ->
                assertEquals(expected, SkillSelector.fallback(cards, SkillBuilds.HURRICANE,
                    emptyList(), SkillFallback.RANDOM, Random(seed)))
            }
        }
    }

    @Test fun multipleUpgradesAreRandomizedWithoutSelectingANewSkill() {
        val cards = listOf("学习雷击", "陨石连击", "激光延续")
        listOf(SkillBuilds.HURRICANE, SkillBuilds.FIREBALL, SkillBuilds.CUSTOM).forEach { build ->
            val chosen = (1..100).map { seed ->
                SkillSelector.fallback(cards, build, emptyList(), SkillFallback.RANDOM, Random(seed))
            }.toSet()
            assertEquals(setOf(2, 3), chosen)
        }
    }

    @Test fun leftmostModeUsesTheLeftmostUpgradeInsteadOfTheLeftmostNewSkill() {
        assertEquals(2, SkillSelector.fallback(listOf("学习雷击", "陨石连击", "激光延续"),
            SkillBuilds.HURRICANE, emptyList(), SkillFallback.LEFTMOST))
    }

    @Test fun allNewSkillsStillUseTheConfiguredFallback() {
        val cards = listOf("学习月牙", "学习雷击", "学习飞轮鋸")
        val chosen = (1..100).map { seed ->
            SkillSelector.fallback(cards, SkillBuilds.HURRICANE, emptyList(), SkillFallback.RANDOM, Random(seed))
        }.toSet()
        assertEquals(setOf(1, 2, 3), chosen)
        assertEquals(1, SkillSelector.fallback(cards, SkillBuilds.HURRICANE, emptyList(), SkillFallback.LEFTMOST))
        assertNull(SkillSelector.fallback(cards, SkillBuilds.HURRICANE, emptyList(), SkillFallback.PAUSE))
    }

    @Test fun conflictsAreRemovedBeforeLookingForAnUpgrade() {
        val cards = listOf("真空", "学习雷击", "学习月牙")
        val selected = listOf("强袭飓风")
        val chosen = (1..100).map { seed ->
            SkillSelector.fallback(cards, SkillBuilds.HURRICANE, selected, SkillFallback.RANDOM, Random(seed))
        }.toSet()
        assertEquals(setOf(2, 3), chosen)
        assertEquals(2, SkillSelector.fallback(cards, SkillBuilds.HURRICANE, selected, SkillFallback.LEFTMOST))
        assertEquals(3, SkillSelector.fallback(listOf("真空", "学习雷击", "激光延续"),
            SkillBuilds.HURRICANE, selected, SkillFallback.LEFTMOST))
    }

    @Test fun incompleteNamesAndPauseModeNeverAutoSelect() {
        SkillFallback.entries.forEach { mode ->
            assertNull(SkillSelector.fallback(listOf("", "学习雷击", "陨石连击"),
                SkillBuilds.HURRICANE, emptyList(), mode))
            assertNull(SkillSelector.fallback(listOf("学习雷击", "陨石连击"),
                SkillBuilds.HURRICANE, emptyList(), mode))
        }
        assertNull(SkillSelector.fallback(listOf("陨石连击", "学习雷击", "激光延续"),
            SkillBuilds.HURRICANE, emptyList(), SkillFallback.PAUSE))
    }

    @Test fun learningLabelIsDetectedAfterWhitespaceNormalization() {
        assertEquals(2, SkillSelector.fallback(listOf("学 习 雷击", "连环霸爆燥", "学\n习飞轮鋸"),
            SkillBuilds.HURRICANE, emptyList(), SkillFallback.LEFTMOST))
    }
}

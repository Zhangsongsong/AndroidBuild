package com.zasko.apppoint.automation

import org.junit.Assert.*
import org.junit.Test

class SkillCatalogTest {
    @Test fun catalogGroupsAllConfiguredFamiliesWithoutChangingOrder() {
        val groups = SkillCatalog.groups(emptyList())
        assertEquals(listOf("飓风系", "龙卷风系", "冰霜新星系", "火球系"), groups.map { it.title })
        assertEquals(SkillBuilds.hurricane.priority + SkillBuilds.fireball.priority, groups.flatMap { it.skills })
    }

    @Test fun selectingAppendsAndRemovingRenumbersWithoutChangingRemainingPriorities() {
        val original = listOf("乱流", "多重飓风")
        val appended = SkillCatalog.toggle(original, "飓风增幅")
        assertEquals(listOf("乱流", "多重飓风", "飓风增幅"), appended)
        assertEquals(original, SkillCatalog.toggle(appended, "飓风增幅"))
        assertEquals(1, SkillCatalog.selectedIndex(SkillCatalog.toggle(appended, "多重飓风"), "飓风增幅"))
    }

    @Test fun aliasesSelectOneCardInsteadOfAddingDuplicateEntries() {
        val learn = SkillBuilds.hurricaneSkills.first()
        assertEquals(0, SkillCatalog.selectedIndex(listOf("飓风术"), learn))
        assertTrue(SkillCatalog.toggle(listOf("飓风术"), learn).isEmpty())
        assertEquals("飓风术", SkillCatalog.title(learn))
    }

    @Test fun fireballPresetPrioritizesTheFireballUpgrade() {
        assertEquals(2, SkillSelector.select(
            listOf("学习冰霜新星", "学习火球术", "风暴聚集"),
            SkillBuilds.fireball.priority,
            SkillBuilds.FIREBALL
        ))
        assertTrue(SkillCatalog.groups(emptyList()).last().skills.contains("学习连珠火球|解锁连珠火球|连珠火球"))
        assertEquals(1, SkillSelector.select(
            listOf("学习连珠火球", "学习冰霜新星", "风暴聚集"),
            SkillBuilds.fireball.priority,
            SkillBuilds.FIREBALL
        ))
    }

    @Test fun fireballPriorityMatchesTheObservedOcrConfusionForFlameBurst() {
        val priority = listOf("炎爆术", "连珠火球")
        assertEquals("炎爆术", SkillSelector.normalize("炎燥术"))
        assertEquals(1, SkillSelector.select(listOf("炎燥术", "连珠火球", "稳定光线"), priority))
        assertEquals(2, SkillSelector.select(listOf("稳定光线", "炎燥术", "连珠火球"), priority))
    }

    @Test fun hurricanePriorityMatchesJapaneseVariantCharacterInHurricaneAmplification() {
        val cards = listOf("强袭龙卷", "飓风増幅", "龙卷风延续")
        assertEquals("飓风增幅", SkillSelector.normalize("飓风増幅"))
        assertEquals(2, SkillSelector.select(cards, SkillBuilds.hurricane.priority, SkillBuilds.HURRICANE))
    }

    @Test fun removedCustomSkillsRemainAvailableDuringEditing() {
        val custom = "自定技能|自定义别名"
        val groups = SkillCatalog.groups(listOf(custom, custom))
        assertEquals("自定义技能", groups.last().title)
        assertEquals(listOf(custom), groups.last().skills)
        val selected = SkillCatalog.toggle(emptyList(), groups.last().skills.single())
        assertEquals(listOf(custom), selected)
    }

    @Test fun editedAliasesSurviveRemovingAndReaddingKnownSkill() {
        val edited = "乱流|乱流别名"
        val groups = SkillCatalog.groups(listOf("乱流", edited))
        val catalogEntry = groups.first().skills.first { SkillCatalog.selectedIndex(listOf(it), "乱流") >= 0 }
        assertEquals(edited, catalogEntry)
        assertEquals(listOf(edited), SkillCatalog.toggle(emptyList(), catalogEntry))
        assertEquals(4, groups.size)
    }
}

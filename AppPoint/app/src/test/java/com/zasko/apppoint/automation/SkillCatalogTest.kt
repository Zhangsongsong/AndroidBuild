package com.zasko.apppoint.automation

import org.junit.Assert.*
import org.junit.Test

class SkillCatalogTest {
    @Test fun catalogGroupsAllConfiguredFamiliesWithoutChangingOrder() {
        val groups = SkillCatalog.groups(emptyList())
        assertEquals(listOf("飓风系", "龙卷风系", "冰霜新星系"), groups.map { it.title })
        assertEquals(SkillBuilds.hurricane.priority, groups.flatMap { it.skills })
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
        assertEquals(3, groups.size)
    }
}

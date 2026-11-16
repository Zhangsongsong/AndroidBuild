package com.zasko.apppoint.automation

import org.junit.Assert.*
import org.junit.Test

class SkillPriorityDraftTest {
    @Test fun movingRowsChangesSelectionPriority() {
        val original = listOf("飓风术", "乱流", "多重飓风")
        val moved = SkillPriorityDraft.move(original, 2, 0)
        assertEquals(listOf("多重飓风", "飓风术", "乱流"), moved)
        assertEquals(listOf("飓风术", "乱流", "多重飓风"), original)
        assertEquals(3, SkillSelector.select(listOf("乱流", "飓风术", "多重飓风"), moved))
        assertEquals(original, SkillPriorityDraft.move(moved, 0, 2))
    }

    @Test fun editingAndAddingPreserveAliasesAndRejectDuplicateSkills() {
        val entries = SkillPriorityDraft.replace(listOf("飓风术"), null, "多重飓风II", " 多重飓风Ⅱ,多重飓风2 | 多重飓风 II")
        assertEquals(2, entries.size)
        assertEquals(2, SkillSelector.select(listOf("火球术", "多重飓风2", "冰锥术"), entries))
        val edited = SkillPriorityDraft.replace(entries, 0, "学习飓风", "飓风术")
        assertEquals("学习飓风|飓风术", edited.first())
        assertThrows(IllegalArgumentException::class.java) { SkillPriorityDraft.replace(entries, null, "飓风术", "") }
        assertThrows(IllegalArgumentException::class.java) { SkillPriorityDraft.entry("", "乱流") }
        assertThrows(IllegalArgumentException::class.java) { SkillPriorityDraft.entry("乱流,风阻", "") }
    }

    @Test fun savingSkillSettingsPreservesCoordinatesTemplatesAndTiming() {
        val initial = DemonVideoPreset.config().copy(rounds = 17, pollMs = 800, timeoutMs = 80000)
        val reordered = SkillPriorityDraft.move(initial.skillPriority, 3, 0)
        val saved = initial.withSkillPriority(reordered, SkillBuilds.HURRICANE, SkillFallback.RANDOM)
        assertEquals(reordered, saved.skillPriority)
        assertEquals(initial, saved.copy(skillPriority = initial.skillPriority))
        assertEquals(17, saved.rounds)
        assertEquals(initial.rules, saved.rules)
        assertEquals(initial.skillCardRegions, saved.skillCardRegions)
        assertEquals(initial.referenceAspectRatio, saved.referenceAspectRatio)
        assertThrows(IllegalArgumentException::class.java) { initial.withSkillPriority(emptyList(), SkillBuilds.CUSTOM, SkillFallback.RANDOM) }
    }

    @Test fun loadingVideoCalibrationDoesNotResetUserPriorities() {
        val saved = DemonConfig(skillPriority = listOf("乱流", "多重飓风", "飓风增幅"),
            skillBuild = SkillBuilds.HURRICANE, skillFallback = SkillFallback.PAUSE)
        val calibrated = DemonVideoPreset.calibratedWithSavedSkills(saved)
        assertEquals(saved.skillPriority, calibrated.skillPriority)
        assertEquals(saved.skillFallback, calibrated.skillFallback)
        assertEquals(DemonVideoPreset.config().rules, calibrated.rules)
        assertEquals(DemonVideoPreset.config(), DemonVideoPreset.calibratedWithSavedSkills(DemonConfig()))
    }
}

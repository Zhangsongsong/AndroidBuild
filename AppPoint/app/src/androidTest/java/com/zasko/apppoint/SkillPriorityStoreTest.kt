package com.zasko.apppoint

import androidx.test.platform.app.InstrumentationRegistry
import com.zasko.apppoint.automation.*
import org.junit.Assert.*
import org.junit.Test

class SkillPriorityStoreTest {
    @Test fun firstLoadPersistsHurricaneAndRestartKeepsEditedOrder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pkg = "com.zasko.apppoint.test.defaults"
        val file = DemonStore.configFile(context, pkg)
        val previous = file.takeIf { it.exists() }?.readBytes()
        try {
            file.delete()
            val initial = DemonStore.load(context, pkg)
            assertTrue(file.isFile)
            assertEquals(SkillBuilds.HURRICANE, initial.skillBuild)
            assertEquals(SkillBuilds.hurricane.priority, initial.skillPriority)
            val edited = listOf("乱流", "飓风增幅")
            DemonStore.saveSkillPriority(context, pkg, edited, SkillBuilds.HURRICANE, SkillFallback.LEFTMOST)
            assertEquals(edited, DemonStore.load(context, pkg).skillPriority)
            assertEquals(SkillFallback.LEFTMOST, DemonStore.load(context, pkg).skillFallback)
            val empty = org.json.JSONObject(DemonStore.encode(initial)).put("skillPriority", org.json.JSONArray())
            file.writeText(empty.toString())
            assertEquals(SkillBuilds.hurricane.priority, DemonStore.load(context, pkg).skillPriority)
            assertTrue(org.json.JSONObject(file.readText()).getJSONArray("skillPriority").length() > 0)
        } finally {
            if (previous == null) file.delete() else file.writeBytes(previous)
        }
    }

    @Test fun migratesExistingFastPollIntervalToTwoSeconds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pkg = "com.zasko.apppoint.test.poll.interval"
        val file = DemonStore.configFile(context, pkg)
        val previous = file.takeIf { it.exists() }?.readBytes()
        try {
            DemonStore.save(context, pkg, DemonVideoPreset.config())
            val legacy = DemonVideoPreset.config().copy(pollMs = 500L)
            file.writeText(DemonStore.encode(legacy))

            val loaded = DemonStore.load(context, pkg)

            assertEquals(2000L, loaded.pollMs)
            assertEquals(2000L, DemonStore.decode(file.readText()).pollMs)
        } finally {
            if (previous == null) file.delete() else file.writeBytes(previous)
        }
    }

    @Test fun savesReloadsAndRunsExactlyTheJsonOrderWithoutChangingCalibration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pkg = "com.zasko.apppoint.test.priorities"
        val file = DemonStore.configFile(context, pkg)
        val previous = file.takeIf { it.exists() }?.readBytes()
        try {
            val base = DemonVideoPreset.config().copy(rounds = 7)
            DemonStore.save(context, pkg, base)
            val order = listOf("乱流", "多重飓风", "飓风增幅")
            DemonStore.saveSkillPriority(context, pkg, order, SkillBuilds.HURRICANE, SkillFallback.RANDOM)
            val loaded = DemonStore.load(context, pkg)
            assertEquals(order, loaded.skillPriority)
            assertEquals(base.rules, loaded.rules)
            assertEquals(base.skillPoints, loaded.skillPoints)
            assertEquals(7, loaded.rounds)
            assertEquals(loaded, DemonStore.decode(file.readText()))
            val flow = DemonFlow(loaded, 1000)
            val cards = listOf("飓风增幅", "多重飓风", "乱流")
            flow.observe(DemonScreen.SKILLS, 1001, cards)
            val tap = flow.observe(DemonScreen.SKILLS, 1002, cards) as FlowDecision.Tap
            assertEquals(base.skillPoints[2], tap.point)
            assertThrows(IllegalArgumentException::class.java) {
                DemonStore.saveSkillPriority(context, pkg, listOf("乱流", "乱 流"), SkillBuilds.HURRICANE, SkillFallback.RANDOM)
            }
            assertEquals(loaded, DemonStore.load(context, pkg))
        } finally {
            if (previous == null) file.delete() else file.writeBytes(previous)
        }
    }

    @Test fun oldJsonWithoutNewOptionalFieldsStillLoadsInOriginalOrder() {
        val json = org.json.JSONObject(DemonStore.encode(DemonVideoPreset.config()))
        json.remove("skillCardRegions")
        json.remove("skillBuild")
        json.remove("skillFallback")
        json.remove("referenceAspectRatio")
        assertEquals(DemonVideoPreset.config().skillPriority, DemonStore.decode(json.toString()).skillPriority)
    }
}

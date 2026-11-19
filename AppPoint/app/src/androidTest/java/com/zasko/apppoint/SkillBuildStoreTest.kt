package com.zasko.apppoint

import androidx.test.platform.app.InstrumentationRegistry
import com.zasko.apppoint.automation.DemonStore
import com.zasko.apppoint.automation.DemonVideoPreset
import com.zasko.apppoint.automation.SkillBuild
import com.zasko.apppoint.automation.SkillBuildStore
import com.zasko.apppoint.automation.SkillBuilds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillBuildStoreTest {
    @Test fun savesEditsAndAddsPresetsWithoutChangingBuiltIns() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = SkillBuildStore.configFile(context)
        val previous = file.takeIf { it.exists() }?.readBytes()
        try {
            val defaults = SkillBuildStore.load(context)
            val fireball = defaults.first { it.id == SkillBuilds.FIREBALL }
                .copy(priority = listOf("学习火球术|火球术", "火球增幅"))
            val custom = SkillBuild("custom_test", "测试流派", listOf("雷电"))
            SkillBuildStore.save(context, defaults.map { if (it.id == fireball.id) fireball else it } + custom)

            val loaded = SkillBuildStore.load(context)

            assertEquals(fireball, loaded.first { it.id == SkillBuilds.FIREBALL })
            assertEquals(custom, loaded.first { it.id == custom.id })
            assertEquals(SkillBuilds.hurricane, loaded.first { it.id == SkillBuilds.HURRICANE })
        } finally {
            if (previous == null) file.delete() else file.writeBytes(previous)
        }
    }

    @Test fun applyingBuildChangesOnlyBuildAndSkillPriority() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pkg = "com.zasko.apppoint.test.apply.build"
        val file = DemonStore.configFile(context, pkg)
        val previous = file.takeIf { it.exists() }?.readBytes()
        try {
            val base = DemonVideoPreset.config().copy(rounds = 7, pollMs = 4000)
            DemonStore.save(context, pkg, base)

            val applied = DemonStore.applySkillBuild(context, pkg, SkillBuilds.fireball)

            assertEquals(base.copy(skillBuild = SkillBuilds.FIREBALL, skillPriority = SkillBuilds.fireball.priority), applied)
            assertEquals(applied, DemonStore.load(context, pkg))
            assertTrue(applied.rules == base.rules)
        } finally {
            if (previous == null) file.delete() else file.writeBytes(previous)
        }
    }
}

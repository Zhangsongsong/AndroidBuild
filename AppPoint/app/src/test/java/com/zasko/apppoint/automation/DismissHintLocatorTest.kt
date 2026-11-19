package com.zasko.apppoint.automation

import org.junit.Assert.*
import org.junit.Test

class DismissHintLocatorTest {
    @Test fun tapsTheDismissInstructionTextItself() {
        val point = DismissHintLocator.find(listOf(
            ScreenText("点击空白处关闭界面", ScreenRegion(.31, .79, .69, .815))
        ))
        assertNotNull(point)
        assertEquals(.5, point!!.x, .000001)
        assertEquals(.8025, point.y, .000001)
    }

    @Test fun acceptsOcrWhitespaceButRejectsAmbiguousOrUnrelatedHints() {
        val noisy = ScreenText("点 击 空 白 处 关 闭 界 面", ScreenRegion(.3, .79, .7, .82))
        assertEquals(.805, DismissHintLocator.find(listOf(noisy))!!.y, .000001)
        assertNull(DismissHintLocator.find(listOf(noisy, noisy.copy(region = ScreenRegion(.3, .8, .7, .83)))))
        assertNull(DismissHintLocator.find(listOf(ScreenText("选择技能", noisy.region))))
        assertNull(DismissHintLocator.find(listOf(noisy.copy(region = ScreenRegion(.3, .95, .7, .99)))))
    }
}

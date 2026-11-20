package com.zasko.apppoint.automation

import org.junit.Assert.*
import org.junit.Test

class FailureSettlementLocatorTest {
    @Test fun locatesEitherConfirmationLabelBelowTheFailureTitle() {
        listOf("确定", "确认").forEach { text ->
            val point = FailureSettlementLocator.find(listOf(
                ScreenText("失败", ScreenRegion(.39, .20, .61, .26)),
                ScreenText(text, ScreenRegion(.43, .78, .57, .82))
            ))
            assertNotNull(text, point)
            assertEquals(.5, point!!.x, .000001)
            assertEquals(.8, point.y, .000001)
        }
    }

    @Test fun rejectsConfirmWithoutFailureTitleOrAmbiguousPanels() {
        val failure = ScreenText("失败", ScreenRegion(.39, .20, .61, .26))
        val confirm = ScreenText("确定", ScreenRegion(.43, .78, .57, .82))
        assertNull(FailureSettlementLocator.find(listOf(confirm)))
        assertNull(FailureSettlementLocator.find(listOf(failure, confirm,
            ScreenText("失败", ScreenRegion(.38, .21, .62, .27)))))
        assertNull(FailureSettlementLocator.find(listOf(failure,
            ScreenText("确定", ScreenRegion(.43, .24, .57, .28)))))
        assertNull(FailureSettlementLocator.find(listOf(failure,
            ScreenText("继续", ScreenRegion(.43, .78, .57, .82)))))
    }
}

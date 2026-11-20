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
        val bottomPrompt = noisy.copy(region = ScreenRegion(.3, .95, .7, .98))
        assertEquals(.965, DismissHintLocator.find(listOf(bottomPrompt))!!.y, .000001)
        assertNull(DismissHintLocator.find(listOf(noisy.copy(region = ScreenRegion(.3, .985, .7, 1.0)))))
    }

    @Test fun joinsAdjacentOcrFragmentsRegardlessOfResultOrder() {
        val point = DismissHintLocator.find(listOf(
            ScreenText("关闭界面", ScreenRegion(.52, .845, .68, .865)),
            ScreenText("烈焰重创", ScreenRegion(.52, .11, .68, .14)),
            ScreenText("点击空白处", ScreenRegion(.31, .845, .51, .865))
        ))
        assertNotNull(point)
        assertEquals(.495, point!!.x, .000001)
        assertEquals(.855, point.y, .000001)
        assertNotNull(DismissHintLocator.find(listOf(
            ScreenText("界面", ScreenRegion(.62, .842, .70, .862)),
            ScreenText("点击", ScreenRegion(.31, .845, .39, .865)),
            ScreenText("空白处", ScreenRegion(.40, .847, .52, .867)),
            ScreenText("关闭", ScreenRegion(.53, .844, .61, .864))
        )))
    }

    @Test fun toleratesOneOcrErrorWhileRejectingOtherActions() {
        val region = ScreenRegion(.31, .845, .69, .865)
        listOf("点击空自处关闭界面", "点击空白处关团界面", "点击空白关闭界面").forEach { text ->
            assertNotNull(text, DismissHintLocator.find(listOf(ScreenText(text, region))))
        }
        listOf("点击空白处开启界面", "点击按钮关闭界面", "点击领取奖励").forEach { text ->
            assertNull(text, DismissHintLocator.find(listOf(ScreenText(text, region))))
        }
    }

    @Test fun joinsWrappedPromptAndDoesNotAppendNearbyNumbersToACompleteHint() {
        val point = DismissHintLocator.find(listOf(
            ScreenText("点击空白处", ScreenRegion(.39, .84, .61, .86)),
            ScreenText("关闭界面", ScreenRegion(.41, .865, .59, .885))
        ))
        assertNotNull(point)
        assertEquals(.50, point!!.x, .000001)
        assertEquals(.8625, point.y, .000001)
        assertNotNull(DismissHintLocator.find(listOf(
            ScreenText("点击空白处关闭界面", ScreenRegion(.31, .845, .69, .865)),
            ScreenText("52336", ScreenRegion(.70, .845, .82, .865))
        )))
    }

    @Test fun doesNotJoinTextFromDifferentRowsOrDistantBoxes() {
        val left = ScreenText("点击空白处", ScreenRegion(.31, .845, .51, .865))
        assertNull(DismissHintLocator.find(listOf(left,
            ScreenText("关闭界面", ScreenRegion(.52, .80, .68, .82)))))
        assertNull(DismissHintLocator.find(listOf(left,
            ScreenText("关闭界面", ScreenRegion(.70, .845, .86, .865)))))
    }

    @Test fun locatesTheDamagedRouletteInstructionFromTheXiaomiTimeoutLog() {
        val region = ScreenRegion(.30185185185185187, .8420833333333333,
            .7064814814814815, .86375)
        val counter = ScreenText("621", ScreenRegion(.7138888888888889, .8616666666666667,
            .7703703703703704, .8754166666666666))
        listOf("点太堂自处关闭界面3", "点太堂自处关闭界面3621", "点击空白处关闭界面3").forEach { text ->
            val point = DismissHintLocator.find(listOf(ScreenText(text, region), counter))
            assertNotNull(text, point)
            assertEquals(.5041666667, point!!.x, .000001)
            assertEquals(.8529166667, point.y, .000001)
        }
    }

    @Test fun damagedWordingNeedsTheLowerCenteredInstructionLayoutAndUniqueMatch() {
        val prompt = ScreenText("点太堂自处关闭界面3", ScreenRegion(.3, .84, .7, .865))
        assertNull(DismissHintLocator.find(listOf(prompt.copy(region = ScreenRegion(.3, .3, .7, .325)))))
        assertNull(DismissHintLocator.find(listOf(prompt.copy(region = ScreenRegion(.04, .84, .3, .865)))))
        assertNull(DismissHintLocator.find(listOf(prompt.copy(region = ScreenRegion(.48, .84, .52, .865)))))
        listOf("关闭界面", "点太堂自处开启界面3", "点击按钮关闭界面", "领取奖励关闭界面").forEach { text ->
            assertNull(text, DismissHintLocator.find(listOf(prompt.copy(text = text))))
        }
        assertNull(DismissHintLocator.find(listOf(prompt, prompt.copy(region = ScreenRegion(.3, .90, .7, .925)))))
    }

    @Test fun locatesTheNewOcrVariantAndLeadingCounterNoiseFromTheRepeatTimeout() {
        val firstOcrPass = ScreenText("5a点去堂自处关间界面、",
            ScreenRegion(.0083333333, .8408333333, .7037037037, .87))
        val secondOcrPass = ScreenText("点去室自处关闭界面。",
            ScreenRegion(.0981481481, .8408333333, .7018518519, .86875))
        assertEquals(.3560185185, DismissHintLocator.find(listOf(firstOcrPass))!!.x, .000001)
        assertEquals(.4, DismissHintLocator.find(listOf(secondOcrPass))!!.x, .000001)
    }

    @Test fun damagedPromptRequiresExactlyNineChineseCharacters() {
        val region = ScreenRegion(.3, .84, .7, .865)
        listOf("点去室自处关间界面。", "5a点去堂自处关间界面、").forEach { text ->
            assertNotNull(text, DismissHintLocator.find(listOf(ScreenText(text, region))))
        }
        listOf("点去室自处关间界面额", "点去室处关间界面").forEach { text ->
            assertNull(text, DismissHintLocator.find(listOf(ScreenText(text, region))))
        }
    }
}

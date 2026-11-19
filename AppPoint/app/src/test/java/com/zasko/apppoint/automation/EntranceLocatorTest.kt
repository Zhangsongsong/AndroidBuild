package com.zasko.apppoint.automation

import org.junit.Assert.*
import org.junit.Test

class EntranceLocatorTest {
    private fun labels(width: Int, height: Int, cardTop: Double): List<ScreenText> {
        val cardHeight = (width.toDouble() / height) * .89 * (388.0 / 502)
        fun label(text: String, center: Double) = ScreenText(text,
            ScreenRegion(.4, center - .01, .6, center + .01))
        return listOf(label("封魔之地", cardTop + cardHeight * .215),
            label("进入游戏", cardTop + cardHeight * .80))
    }

    @Test fun calculatesSameRelativeButtonOnDifferentResolutionPhones() {
        listOf(1080 to 2400, 720 to 1600, 1440 to 3200, 1080 to 1920).forEach { (w, h) ->
            val tap = EntranceLocator.find(labels(w, h, .2), w, h)!!
            assertEquals(.5, tap.x, .000001)
            assertEquals(.2 + (w.toDouble() / h) * .89 * (388.0 / 502) * .83, tap.y, .000001)
        }
    }

    @Test fun scrollingMovesTheButtonByExactlyTheSameRatio() {
        val first = EntranceLocator.find(labels(1080, 2400, .15), 1080, 2400)!!
        val scrolled = EntranceLocator.find(labels(1080, 2400, .35), 1080, 2400)!!
        assertEquals(.20, scrolled.y - first.y, .000001)
    }

    @Test fun doesNotTapAnotherCardOrAnOffscreenButton() {
        val text = labels(1080, 2400, .2)
        assertNull(EntranceLocator.find(text.drop(1), 1080, 2400))
        assertNull(EntranceLocator.find(listOf(ScreenText("学徒试炼", ScreenRegion(.4, .2, .6, .3))), 1080, 2400))
        assertNull(EntranceLocator.find(labels(1080, 2400, .6), 1080, 2400))
        assertNull(EntranceLocator.find(text + text, 1080, 2400))
    }

    @Test fun entranceDoesNotRequireSavedTemplate() {
        val entrance = DemonVideoPreset.config().rules.first { it.screen == DemonScreen.ENTRANCE }
        assertFalse(entrance.requiresTemplate)
        assertTrue(DemonVideoPreset.config().rules.filter { it.screen != DemonScreen.ENTRANCE }.all { it.requiresTemplate })
    }
}

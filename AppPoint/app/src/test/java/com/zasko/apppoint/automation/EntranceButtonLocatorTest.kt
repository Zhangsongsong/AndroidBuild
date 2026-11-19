package com.zasko.apppoint.automation

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

class EntranceButtonLocatorTest {
    private fun image(name: String): BufferedImage = ImageIO.read(
        checkNotNull(javaClass.getResource("/entrance/$name.png"))
    )

    private fun locate(image: BufferedImage): ScreenPoint? = EntranceButtonLocator.find(
        image.getRGB(0, 0, image.width, image.height, null, 0, image.width), image.width, image.height
    )

    @Test fun locatesTheActualButtonInThreeRealScrollingPositions() {
        listOf("entrance-upper" to .436, "entrance-lower" to .606, "entrance-current" to .584).forEach { (name, y) ->
            val tap = checkNotNull(locate(image(name))) { name }
            assertEquals(name, .50, tap.x, .02)
            assertEquals(name, y, tap.y, .01)
        }
    }

    @Test fun keepsRelativePositionAcrossResolutions() {
        val source = image("entrance-current")
        val expected = checkNotNull(locate(source))
        listOf(180 to 400, 360 to 800, 540 to 1200).forEach { (width, height) ->
            val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val graphics = resized.createGraphics()
            try { graphics.drawImage(source, 0, 0, width, height, null) } finally { graphics.dispose() }
            val actual = checkNotNull(locate(resized))
            assertEquals(expected.x, actual.x, .01)
            assertEquals(expected.y, actual.y, .01)
        }
    }

    @Test fun doesNotClickThroughTheExpandedOverlayOrMistakeBattleForEntrance() {
        assertNull(locate(image("entrance-obscured")))
        assertNull(locate(image("battle")))
    }

    @Test fun rejectsGoldButtonsWithoutTheEnergyDiamondOrEntranceTitle() {
        val withoutEnergy = image("entrance-current")
        val graphics = withoutEnergy.createGraphics()
        try {
            graphics.color = java.awt.Color(250, 223, 165)
            graphics.fillRect(150, 473, 28, 17)
        } finally { graphics.dispose() }
        assertNull(locate(withoutEnergy))
        val withoutTitle = image("entrance-current")
        val titleGraphics = withoutTitle.createGraphics()
        try {
            titleGraphics.color = java.awt.Color.DARK_GRAY
            titleGraphics.fillRect(60, 285, 240, 64)
        } finally { titleGraphics.dispose() }
        assertNull(locate(withoutTitle))
    }
}

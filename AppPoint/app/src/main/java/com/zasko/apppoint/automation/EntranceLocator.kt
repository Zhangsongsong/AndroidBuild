package com.zasko.apppoint.automation

data class ScreenText(val text: String, val region: ScreenRegion)

/** Locate the scrolling card, then calculate its button using the supplied 502 x 388 layout. */
object EntranceLocator {
    fun find(text: List<ScreenText>, width: Int, height: Int): ScreenPoint? {
        if (width <= 0 || height <= 0) return null
        fun normalized(value: String) = value.replace(Regex("\\s+"), "")
        val titles = text.filter { normalized(it.text) == "封魔之地" }
        // The card occupies 89% of the display width. Vertical position comes from its title,
        // so scrolling does not change the button's position within the card.
        val cardHeight = (width.toDouble() / height) * .89 * (388.0 / 502)
        val points = titles.mapNotNull { title ->
            val centerX = (title.region.left + title.region.right) / 2
            val centerY = (title.region.top + title.region.bottom) / 2
            if (centerX !in .35.. .65) return@mapNotNull null
            val cardTop = centerY - cardHeight * .215
            val tap = ScreenPoint(.5, cardTop + cardHeight * .83)
            if (cardTop < 0 || tap.y !in .1.. .78) return@mapNotNull null
            tap
        }
        return points.singleOrNull()
    }
}

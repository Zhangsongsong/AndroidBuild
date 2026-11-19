package com.zasko.apppoint.automation

/** Finds the entrance button from the current pixels; all geometry is relative to its card. */
object EntranceButtonLocator {
    fun find(pixels: IntArray, width: Int, height: Int): ScreenPoint? {
        require(width > 0 && height > 0 && pixels.size == width * height)
        fun gold(color: Int): Boolean {
            val r = color shr 16 and 255
            val g = color shr 8 and 255
            val b = color and 255
            return r >= 185 && g >= 150 && b >= 80 && r >= g && r - g < 80 && g - b >= 14
        }
        val visited = BooleanArray(pixels.size)
        val queue = IntArray(pixels.size)
        val candidates = mutableListOf<ScreenPoint>()
        for (origin in pixels.indices) {
            if (visited[origin] || !gold(pixels[origin])) continue
            var head = 0
            var tail = 1
            queue[0] = origin
            visited[origin] = true
            var left = origin % width
            var right = left
            var top = origin / width
            var bottom = top
            while (head < tail) {
                val index = queue[head++]
                val x = index % width
                val y = index / width
                left = minOf(left, x)
                right = maxOf(right, x)
                top = minOf(top, y)
                bottom = maxOf(bottom, y)
                fun visit(next: Int) {
                    if (!visited[next] && gold(pixels[next])) {
                        visited[next] = true
                        queue[tail++] = next
                    }
                }
                if (x > 0) visit(index - 1)
                if (x + 1 < width) visit(index + 1)
                if (y > 0) visit(index - width)
                if (y + 1 < height) visit(index + width)
            }
            val buttonWidth = right - left + 1
            val buttonHeight = bottom - top + 1
            val centerX = (left + right) / 2.0
            val centerY = (top + bottom) / 2.0
            if (buttonWidth.toDouble() / width !in .25.. .43 ||
                buttonWidth.toDouble() / buttonHeight !in 1.8..3.5 ||
                centerX / width !in .42.. .58 || centerY / height !in .18.. .78 ||
                tail.toDouble() / (buttonWidth * buttonHeight) < .55) continue

            fun fraction(x0: Int, y0: Int, x1: Int, y1: Int, predicate: (Int) -> Boolean): Double {
                if (x0 < 0 || y0 < 0 || x1 >= width || y1 >= height || x0 > x1 || y0 > y1) return 0.0
                var matches = 0
                for (y in y0..y1) for (x in x0..x1) if (predicate(pixels[y * width + x])) matches++
                return matches.toDouble() / ((x1 - x0 + 1) * (y1 - y0 + 1))
            }
            // The energy diamond distinguishes this button from other gold actions.
            val energy = fraction(left, top, right, bottom) { color ->
                val r = color shr 16 and 255
                val g = color shr 8 and 255
                val b = color and 255
                r < 150 && g > 105 && b > 100 && g - r > 30
            }
            if (energy < .008) continue
            val cardWidth = buttonWidth / .36
            val cardHeight = cardWidth * .80
            val cardLeft = centerX - cardWidth / 2
            val cardTop = centerY - cardHeight * .80
            // Check the purple entrance title above this button, not an adjacent card.
            val title = fraction(
                (cardLeft + cardWidth * .20).toInt(), (cardTop + cardHeight * .10).toInt(),
                (cardLeft + cardWidth * .80).toInt(), (cardTop + cardHeight * .31).toInt()
            ) { color ->
                val r = color shr 16 and 255
                val g = color shr 8 and 255
                val b = color and 255
                r > 100 && b > 100 && b - g > 20 && r - g > 10
            }
            if (title < .04) continue
            candidates += ScreenPoint(centerX / width, centerY / height)
        }
        return candidates.singleOrNull()
    }
}

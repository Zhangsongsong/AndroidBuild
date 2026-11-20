package com.zasko.apppoint.automation

object DismissHintLocator {
    private const val hint = "点击空白处关闭界面"

    fun find(lines: List<ScreenText>): ScreenPoint? {
        val candidates = lines.toMutableList()
        // Join neighboring boxes from the same instruction, including a wrapped second row.
        for (start in lines.indices) {
            val parts = mutableListOf(lines[start])
            var region = lines[start].region
            while (parts.size < hint.length && normalize(parts.joinToString("") { it.text }).length < hint.length) {
                val remaining = lines.filter { it !in parts }
                val sameRow = remaining.filter { line ->
                    val box = line.region
                    val overlap = minOf(region.bottom, box.bottom) - maxOf(region.top, box.top)
                    box.left >= region.right - .005 && box.left - region.right <= .06 &&
                        overlap >= minOf(region.bottom - region.top, box.bottom - box.top) * .5
                }.minByOrNull { it.region.left }
                val next = sameRow ?: remaining.filter { line ->
                    val box = line.region
                    val horizontalOverlap = minOf(region.right, box.right) - maxOf(region.left, box.left)
                    box.top >= region.bottom - .005 && box.top - region.bottom <= .025 &&
                        horizontalOverlap >= minOf(region.right - region.left, box.right - box.left) * .5
                }.minWithOrNull(compareBy<ScreenText> { it.region.top }.thenBy { it.region.left }) ?: break
                parts += next
                region = ScreenRegion(minOf(region.left, next.region.left), minOf(region.top, next.region.top),
                    maxOf(region.right, next.region.right), maxOf(region.bottom, next.region.bottom))
                candidates += ScreenText(parts.joinToString("") { it.text }, region)
            }
        }
        val exact = candidates.filter { normalize(it.text).contains(hint) }
        val matches = if (exact.isNotEmpty()) exact else candidates.filter { line ->
            matchesDamagedPrompt(line) || matchesSingleOcrError(line)
        }
        val line = matches.singleOrNull() ?: return null
        val centerX = (line.region.left + line.region.right) / 2
        val centerY = (line.region.top + line.region.bottom) / 2
        if (centerX !in 0.05.. .95 || centerY !in 0.1.. .98) return null
        return ScreenPoint(centerX, centerY)
    }

    private fun normalize(text: String) = text.filter(Char::isLetterOrDigit)

    private fun matchesDamagedPrompt(line: ScreenText): Boolean {
        val text = line.text.filter { it in '\u4e00'..'\u9fff' }
        if (text.length != hint.length || text[4] != '处' ||
            editDistance(text.substring(5), "关闭界面") > 1
        ) return false
        val centerX = (line.region.left + line.region.right) / 2
        val centerY = (line.region.top + line.region.bottom) / 2
        val width = line.region.right - line.region.left
        return centerX in .25.. .75 && centerY in .75.. .93 && width in .2.. .85
    }

    private fun matchesSingleOcrError(line: ScreenText): Boolean {
        val text = normalize(line.text).dropLastWhile(Char::isDigit)
        if (((text.contains("点击") && text.contains("关闭")) ||
                (text.contains("空白") && text.contains("界面"))) &&
            text.length in hint.length - 1..hint.length + 1 && editDistance(text, hint) <= 1
        ) return true
        return false
    }

    private fun editDistance(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        left.forEachIndexed { index, char ->
            val current = IntArray(right.length + 1)
            current[0] = index + 1
            for (column in right.indices) {
                current[column + 1] = minOf(current[column] + 1, previous[column + 1] + 1,
                    previous[column] + if (char == right[column]) 0 else 1)
            }
            previous = current
        }
        return previous.last()
    }
}

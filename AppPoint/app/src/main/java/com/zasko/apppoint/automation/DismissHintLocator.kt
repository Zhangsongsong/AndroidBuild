package com.zasko.apppoint.automation

object DismissHintLocator {
    private const val hint = "点击空白处关闭界面"

    fun find(lines: List<ScreenText>): ScreenPoint? {
        val matches = lines.filter { line ->
            line.text.filter(Char::isLetterOrDigit).contains(hint)
        }
        val line = matches.singleOrNull() ?: return null
        val centerX = (line.region.left + line.region.right) / 2
        val centerY = (line.region.top + line.region.bottom) / 2
        if (centerX !in 0.05.. .95 || centerY !in 0.1.. .9) return null
        return ScreenPoint(centerX, centerY)
    }
}

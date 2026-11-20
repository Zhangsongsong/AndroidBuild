package com.zasko.apppoint.automation

/** Finds the confirmation button only when it belongs to a centered failure result panel. */
object FailureSettlementLocator {
    fun find(lines: List<ScreenText>): ScreenPoint? {
        val failures = lines.filter { line ->
            val text = line.text.filter(Char::isLetterOrDigit)
            text.contains("失败") && line.region.centerX() in .25.. .75 && line.region.centerY() in .12.. .55
        }
        if (failures.size != 1) return null
        val failure = failures.single()
        val confirmations = lines.filter { line ->
            val text = line.text.filter(Char::isLetterOrDigit)
            (text == "确定" || text == "确认") &&
                line.region.centerX() in .30.. .70 &&
                line.region.centerY() in .50.. .96 &&
                line.region.top > failure.region.bottom
        }
        val button = confirmations.singleOrNull() ?: return null
        return ScreenPoint(button.region.centerX(), button.region.centerY())
    }

    private fun ScreenRegion.centerX() = (left + right) / 2
    private fun ScreenRegion.centerY() = (top + bottom) / 2
}

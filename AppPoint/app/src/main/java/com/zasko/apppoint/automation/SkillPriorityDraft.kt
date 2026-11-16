package com.zasko.apppoint.automation

/** Order in this list is the priority used at runtime. */
object SkillPriorityDraft {
    fun entry(name: String, aliases: String): String {
        require(name.none { it == '|' || it == ',' || it == '，' || it == 10.toChar() }) { "名称不能包含分隔符，请把其他名称填入别名" }
        val values = listOf(name.trim()) + aliases.split('|', ',', '，', 10.toChar()).map { it.trim() }.filter { it.isNotEmpty() }
        return values.joinToString("|").also { SkillSelector.validatePriority(listOf(it)) }
    }

    fun move(entries: List<String>, from: Int, to: Int): List<String> {
        require(from in entries.indices && to in entries.indices)
        return entries.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun replace(entries: List<String>, index: Int?, name: String, aliases: String): List<String> {
        val value = entry(name, aliases)
        return entries.toMutableList().apply {
            if (index == null) add(value) else set(index, value)
            SkillSelector.validatePriority(this)
        }
    }
}

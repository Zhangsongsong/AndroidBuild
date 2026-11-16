package com.zasko.apppoint.automation

data class SkillGroup(val id: String, val title: String, val skills: List<String>)

/** Catalog membership uses aliases; the saved list still alone determines runtime priority. */
object SkillCatalog {
    private val builtIns get() = listOf(
        SkillGroup("hurricane", "飓风系", SkillBuilds.hurricaneSkills),
        SkillGroup("tornado", "龙卷风系", SkillBuilds.tornadoSkills),
        SkillGroup("frost_nova", "冰霜新星系", SkillBuilds.frostNovaSkills)
    )

    private fun names(entry: String) = entry.split('|').map(SkillSelector::normalize).filter { it.isNotBlank() }.toSet()

    fun selectedIndex(selected: List<String>, skill: String): Int {
        val aliases = names(skill)
        return selected.indexOfFirst { entry -> names(entry).any { it in aliases } }
    }

    fun toggle(selected: List<String>, skill: String): List<String> {
        val index = selectedIndex(selected, skill)
        return if (index >= 0) selected.filterIndexed { i, _ -> i != index }
        else (selected + skill).also(SkillSelector::validatePriority)
    }

    fun groups(extraEntries: List<String>): List<SkillGroup> {
        val groups = builtIns.map { group ->
            group.copy(skills = group.skills.map { skill ->
                extraEntries.lastOrNull { selectedIndex(listOf(it), skill) >= 0 } ?: skill
            })
        }
        val known = groups.flatMap { it.skills }
        val custom = mutableListOf<String>()
        extraEntries.asReversed().forEach { entry ->
            if (selectedIndex(known, entry) < 0 && selectedIndex(custom, entry) < 0) custom.add(entry)
        }
        return if (custom.isEmpty()) groups else groups + SkillGroup("custom", "自定义技能", custom)
    }

    fun title(entry: String): String {
        val aliases = entry.split('|')
        return aliases.firstOrNull { alias ->
            !alias.startsWith("学习") && !alias.startsWith("解锁")
        } ?: aliases.first()
    }
}

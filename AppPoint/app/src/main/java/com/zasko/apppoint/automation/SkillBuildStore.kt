package com.zasko.apppoint.automation

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

object SkillBuildStore {
    fun configFile(context: Context): File = File(context.filesDir, "skill-builds.json")
        .also { it.parentFile?.mkdirs() }

    fun load(context: Context): List<SkillBuild> {
        val path = configFile(context)
        if (!path.exists()) return SkillBuilds.builtIns
        val array = JSONArray(AtomicFile(path).readFully().toString(Charsets.UTF_8))
        val saved = List(array.length()) { index ->
            val item = array.getJSONObject(index)
            SkillBuild(item.getString("id"), item.getString("title"),
                List(item.getJSONArray("priority").length()) { item.getJSONArray("priority").getString(it) })
        }
        val byId = saved.associateBy { it.id }
        return SkillBuilds.builtIns.map { byId[it.id] ?: it } + saved.filter { build ->
            SkillBuilds.builtIns.none { it.id == build.id }
        }
    }

    fun save(context: Context, builds: List<SkillBuild>) {
        validate(builds)
        val contents = JSONArray().apply {
            builds.forEach { build -> put(JSONObject().apply {
                put("id", build.id)
                put("title", build.title.trim())
                put("priority", JSONArray(build.priority))
            }) }
        }.toString(2).toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(configFile(context))
        val output = atomic.startWrite()
        try {
            output.write(contents)
            atomic.finishWrite(output)
        } catch (failure: Exception) {
            atomic.failWrite(output)
            throw failure
        }
    }

    fun validate(builds: List<SkillBuild>) {
        val ids = builds.map { it.id }
        require(ids.distinct().size == ids.size) { "流派标识重复" }
        require(SkillBuilds.builtIns.all { it.id in ids }) { "内置流派不能删除" }
        val titles = builds.map { it.title.trim().lowercase(Locale.ROOT) }
        require(titles.none { it.isBlank() }) { "流派名称不能为空" }
        require(titles.distinct().size == titles.size) { "流派名称重复" }
        builds.forEach { build ->
            require(build.id.matches(Regex("[A-Za-z0-9_.-]{1,80}"))) { "流派标识无效" }
            require(build.priority.isNotEmpty()) { "${build.title}至少需要一个优先技能" }
            SkillSelector.validatePriority(build.priority)
        }
    }
}

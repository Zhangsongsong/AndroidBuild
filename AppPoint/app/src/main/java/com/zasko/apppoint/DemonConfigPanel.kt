package com.zasko.apppoint

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zasko.apppoint.automation.*

@Composable
fun DemonConfigPanel(packageName: String) {
    val context = LocalContext.current
    var calibrationVersion by remember(packageName) { mutableIntStateOf(0) }
    androidx.compose.runtime.key(packageName, calibrationVersion) {
        val loaded = remember(packageName, calibrationVersion) { runCatching { DemonStore.load(context, packageName) } }
        val initial = loaded.getOrDefault(DemonConfig())
        val initialBuilds = remember(packageName) {
            val presets = runCatching { SkillBuildStore.load(context) }.getOrDefault(SkillBuilds.builtIns)
            if (presets.any { it.id == initial.skillBuild }) presets
            else presets + SkillBuild(initial.skillBuild, "自定义配置", initial.skillPriority)
        }
        var rounds by remember(packageName) { mutableStateOf(if (initial.rounds == 0) "" else initial.rounds.toString()) }
        var skillPriority by remember(packageName) { mutableStateOf(initial.skillPriority.toList()) }
        var showPriorityEditor by remember(packageName) { mutableStateOf(false) }
        var showSavedJson by remember(packageName) { mutableStateOf(false) }
        var savedJson by remember(packageName) { mutableStateOf("") }
        var skillBuild by remember(packageName) { mutableStateOf(initial.skillBuild) }
        var buildPresets by remember(packageName) { mutableStateOf(initialBuilds) }
        var selectedBuildId by remember(packageName) { mutableStateOf(initial.skillBuild) }
        var buildMenuExpanded by remember(packageName) { mutableStateOf(false) }
        var skillFallback by remember(packageName) { mutableStateOf(initial.skillFallback) }
        var savedSkillSettings by remember(packageName) { mutableStateOf(Triple(initial.skillPriority, initial.skillBuild, initial.skillFallback)) }
        var interval by remember(packageName) { mutableStateOf(initial.pollMs.toString()) }
        var timeout by remember(packageName) { mutableStateOf((initial.timeoutMs / 1000).toString()) }
        var battleTimeout by remember(packageName) { mutableStateOf((initial.battleTimeoutMs / 1000).toString()) }
        val pointFields = remember(packageName) { initial.skillPoints.map { mutableStateOf("${it.x},${it.y}") } }
        val cardRegionFields = remember(packageName) { initial.skillCardRegions.map { mutableStateOf("${it.left},${it.top},${it.right},${it.bottom}") } }
        val regionFields = remember(packageName) { initial.rules.map { mutableStateOf("${it.region.left},${it.region.top},${it.region.right},${it.region.bottom}") } }
        val tapFields = remember(packageName) { initial.rules.map { mutableStateOf(it.tap?.let { p -> "${p.x},${p.y}" }.orEmpty()) } }
        val thresholdFields = remember(packageName) { initial.rules.map { mutableStateOf(it.threshold.toString()) } }
        val guardFields = remember(packageName) { initial.rules.map { rule ->
            mutableStateOf(rule.guardRegion?.let { "${it.left},${it.top},${it.right},${it.bottom}" }.orEmpty())
        } }
        val guardThresholdFields = remember(packageName) { initial.rules.map { mutableStateOf(it.guardThreshold.toString()) } }
        var message by remember(packageName) { mutableStateOf(loaded.exceptionOrNull()?.let { "配置读取失败：${it.message}" } ?: "已加载本地配置；新配置默认使用飓风流") }
        val status = DemonRunner.status.value
        fun savePriorities(priority: List<String>) {
            check(!DemonRunner.status.value.running) { "请先暂停自动流程再保存" }
            DemonStore.saveSkillPriority(context, packageName, priority, skillBuild, skillFallback)
            skillPriority = priority.toList()
            savedSkillSettings = Triple(priority.toList(), skillBuild, skillFallback)
            DemonRunner.stop("技能优先级已保存，请重新开始以加载 JSON")
            message = "技能优先级已保存到 flow.json；下次开始从该文件加载"
        }
        fun applyBuild(build: SkillBuild) {
            runCatching { DemonStore.applySkillBuild(context, packageName, build) }
                .onSuccess { config ->
                    skillBuild = config.skillBuild
                    skillPriority = config.skillPriority.toList()
                    selectedBuildId = config.skillBuild
                    savedSkillSettings = Triple(config.skillPriority, config.skillBuild, config.skillFallback)
                    message = "已应用${build.title}，其他流程配置保持不变"
                }
                .onFailure { message = "应用流派失败：${it.message}" }
        }
        if (showPriorityEditor) {
            SkillPriorityEditor(initial = skillPriority, onDismiss = { showPriorityEditor = false }, onSave = { priority ->
                savePriorities(priority)
                showPriorityEditor = false
            })
        }
        if (showSavedJson) {
            AlertDialog(onDismissRequest = { showSavedJson = false }, title = { Text("已保存的本地 JSON") },
                text = {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(savedJson, modifier = Modifier.heightIn(max = 440.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState()))
                    }
                }, confirmButton = { TextButton(onClick = { showSavedJson = false }) { Text("关闭") } })
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("封魔之地自动流程", style = MaterialTheme.typography.titleMedium)
                Button(enabled = !status.running, onClick = {
                    runCatching { DemonVideoPreset.install(context, packageName) }
                        .onSuccess { calibrationVersion++ }
                        .onFailure { Toast.makeText(context, "加载失败：${it.message}", Toast.LENGTH_LONG).show() }
                }) { Text("加载你提供录像的布局与模板") }
                Text("录像布局：544×1200，从挑战列表的封魔卡片直接进入。加载名称区域、点击位置和模板，轮数重置为无限；保留已保存的技能顺序。其他布局请校准，首次仍需真机试跑。")
                Text("Android 11+。流程：挑战 → 封魔入口 → 战斗 / 选技能 → 胜利结算 → 返回入口。")
                Text("封魔入口按卡片比例自动定位“进入游戏”，无需采集入口模板。其他界面请先保存配置，再从悬浮窗采集模板。")
                Text("坐标按屏幕比例填写：左上角 0,0，右下角 1,1。区域格式：左,上,右,下；位置格式：X,Y。")
                OutlinedTextField(rounds, { rounds = it }, label = { Text("运行轮数") },
                    placeholder = { Text("无限") },
                    supportingText = { Text("默认无限：留空或填 0；填 1~10000 可指定轮数") },
                    modifier = Modifier.fillMaxWidth())
                val selectedBuild = buildPresets.firstOrNull { it.id == selectedBuildId }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        Button(enabled = !status.running, onClick = { buildMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedBuild?.title ?: "选择流派")
                        }
                        DropdownMenu(expanded = buildMenuExpanded, onDismissRequest = { buildMenuExpanded = false }) {
                            buildPresets.forEach { build ->
                                DropdownMenuItem(text = { Text(build.title) }, onClick = {
                                    selectedBuildId = build.id
                                    buildMenuExpanded = false
                                })
                            }
                        }
                    }
                    Button(enabled = !status.running && selectedBuild != null,
                        onClick = { selectedBuild?.let(::applyBuild) }) { Text("应用流派") }
                }
                Text("选择并应用流派后，当前配置只更新流派和技能优先级；坐标、模板及运行参数保留。")
                Text("已配置 ${skillPriority.size} 项技能。前 5 项：${skillPriority.take(5).joinToString(" → ") { it.substringBefore('|') }}")
                val skillsDirty = Triple(skillPriority, skillBuild, skillFallback) != savedSkillSettings
                Text(if (skillsDirty) "有未保存的技能配置；运行仍使用上次保存的 JSON" else "技能列表与本地配置一致",
                    color = if (skillsDirty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Button(enabled = !status.running, onClick = { showPriorityEditor = true }) { Text("调整技能优先级列表") }
                Text("上方显示已选顺序，点选后可上移、下移、置顶或编辑；下方按技能类型展示卡片，点击加入或移除。改完保存到本地 JSON，取消不保存。")
                Button(enabled = !status.running, onClick = {
                    skillFallback = SkillFallback.entries[(skillFallback.ordinal + 1) % SkillFallback.entries.size]
                }) { Text("无匹配技能：${when (skillFallback) { SkillFallback.RANDOM -> "随机一张"; SkillFallback.LEFTMOST -> "选左边第一张"; SkillFallback.PAUSE -> "暂停" }}（点此切换）") }
                Text("文字为空或识别不稳定时不会触发随机兜底。")
                Button(enabled = !status.running && skillPriority.isNotEmpty(), onClick = {
                    runCatching { savePriorities(skillPriority) }
                        .onFailure { message = "保存失败：${it.message}" }
                }) { Text("保存技能配置到 JSON") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = !status.running, onClick = {
                        runCatching { DemonStore.load(context, packageName) }.onSuccess { config ->
                            skillPriority = config.skillPriority.toList()
                            skillBuild = config.skillBuild
                            skillFallback = config.skillFallback
                            savedSkillSettings = Triple(config.skillPriority, config.skillBuild, config.skillFallback)
                            message = "已从本地 JSON 重新加载技能配置"
                        }.onFailure { message = "读取失败：${it.message}" }
                    }) { Text("重新加载 JSON") }
                    TextButton(onClick = {
                        runCatching {
                            check(DemonStore.configFile(context, packageName).exists()) { "还没有保存的 JSON" }
                            savedJson = DemonStore.encode(DemonStore.load(context, packageName))
                            showSavedJson = true
                        }.onFailure { message = "读取失败：${it.message}" }
                    }) { Text("查看已保存 JSON") }
                }
                Text("文件：${DemonStore.configFile(context, packageName).absolutePath}", style = MaterialTheme.typography.bodySmall)
                Text("保存后重新点击“开始封魔”，会读取此 JSON；“继续”只恢复当前会话。")
                pointFields.forEachIndexed { i, field ->
                    OutlinedTextField(field.value, { field.value = it }, label = { Text("技能卡 ${i + 1} 点击位置") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(cardRegionFields[i].value, { cardRegionFields[i].value = it }, label = { Text("技能卡 ${i + 1} 名称OCR区域：左,上,右,下") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(interval, { interval = it }, label = { Text("识别间隔（毫秒，至少 2000）") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(timeout, { timeout = it }, label = { Text("普通界面超时（秒）") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(battleTimeout, { battleTimeout = it }, label = { Text("单轮战斗超时（秒）") }, modifier = Modifier.fillMaxWidth())
                Text("每个区域选该界面独有且固定的标志。技能页选固定标题/边框，避开随机卡面；结算页只采集胜利画面。失败/不足模板可选。修改区域后需重新采集。")
                initial.rules.forEachIndexed { i, rule ->
                    Text(rule.screen.title, style = MaterialTheme.typography.titleSmall)
                    if (!rule.requiresTemplate) {
                        Text("根据封魔之地卡片位置计算按钮：卡片横向 50%、纵向 83%，随列表滚动，无需模板。")
                        return@forEachIndexed
                    }
                    OutlinedTextField(regionFields[i].value, { regionFields[i].value = it }, label = { Text("识别区域：左,上,右,下") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(thresholdFields[i].value, { thresholdFields[i].value = it }, label = { Text("相似度阈值（0.8~1）") }, modifier = Modifier.fillMaxWidth())
                    if (rule.guardRegion != null) {
                        OutlinedTextField(guardFields[i].value, { guardFields[i].value = it }, label = { Text("辅助识别区域：左,上,右,下") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(guardThresholdFields[i].value, { guardThresholdFields[i].value = it }, label = { Text("辅助识别阈值") }, modifier = Modifier.fillMaxWidth())
                    }
                    if (rule.tap != null) OutlinedTextField(tapFields[i].value, { tapFields[i].value = it }, label = { Text("点击位置：X,Y") }, modifier = Modifier.fillMaxWidth())
                }
                Button(enabled = !status.running, onClick = {
                    runCatching {
                        fun numbers(s: String, n: Int): List<Double> = s.split(',').map { it.trim().toDouble() }.also { require(it.size == n) { "坐标数量不正确" } }
                        fun point(s: String) = numbers(s, 2).let { ScreenPoint(it[0], it[1]) }
                        val config = DemonConfig(
                            rounds = rounds.trim().ifEmpty { "0" }.toInt(), pollMs = interval.toLong(), timeoutMs = timeout.toLong() * 1000,
                            battleTimeoutMs = battleTimeout.toLong() * 1000,
                            skillPoints = pointFields.map { point(it.value) },
                            skillCardRegions = cardRegionFields.map { field -> numbers(field.value, 4).let { ScreenRegion(it[0], it[1], it[2], it[3]) } },
                            skillPriority = skillPriority,
                            skillBuild = skillBuild,
                            skillFallback = skillFallback,
                            referenceAspectRatio = initial.referenceAspectRatio,
                            rules = initial.rules.mapIndexed { i, r ->
                                val v = numbers(regionFields[i].value, 4)
                                r.copy(region = ScreenRegion(v[0], v[1], v[2], v[3]), threshold = thresholdFields[i].value.toDouble(),
                                    tap = if (r.tap == null) null else point(tapFields[i].value),
                                    guardRegion = if (r.guardRegion == null) null else numbers(guardFields[i].value, 4).let { ScreenRegion(it[0], it[1], it[2], it[3]) },
                                    guardThreshold = guardThresholdFields[i].value.toDouble())
                            })
                        val old = runCatching { DemonStore.load(context, packageName) }.getOrNull()
                        DemonStore.save(context, packageName, config)
                        savedSkillSettings = Triple(config.skillPriority, config.skillBuild, config.skillFallback)
                        config.rules.forEach { r ->
                            if (old?.rules?.firstOrNull { it.screen == r.screen }?.let { it.region == r.region && it.guardRegion == r.guardRegion } != true) {
                                DemonStore.templateFile(context, packageName, r.screen).delete()
                                DemonStore.guardFile(context, packageName, r.screen).delete()
                            }
                        }
                        DemonRunner.stop("配置已更新，请重新开始")
                        message = "配置已保存；请从悬浮窗采集各界面模板"
                    }.onFailure { message = "保存失败：${it.message}" }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }) { Text("保存封魔流程配置") }
                Text(message)
                Text("${status.message} · 已完成 ${status.rounds} 轮")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { DemonRunner.stop(resumable = true) }) { Text("暂停") }
                    Button(onClick = { DemonRunner.resume() }, enabled = !status.running) { Text("继续") }
                }
                Text("开始前保持游戏在前台，在悬浮窗点击“开始封魔”。隐藏面板将停止运行。")
            }
        }
    }
}

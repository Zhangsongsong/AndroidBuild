package com.zasko.apppoint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zasko.apppoint.automation.SkillCatalog
import com.zasko.apppoint.automation.SkillPriorityDraft
import kotlinx.coroutines.launch

/** Selected order and catalog scroll independently. Only Save persists edits. */
@Composable
fun SkillPriorityEditor(
    initial: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    saveButtonText: String = "保存到本地 JSON"
) {
    var entries by rememberSaveable { mutableStateOf(initial.toList()) }
    var catalogExtras by rememberSaveable { mutableStateOf(initial.toList()) }
    var focusedEntry by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var aliases by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val groups = remember(catalogExtras, entries) { SkillCatalog.groups(catalogExtras + entries) }
    val focusedIndex = entries.indexOf(focusedEntry)
    val priorityListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun moveFocused(to: Int) {
        entries = SkillPriorityDraft.move(entries, focusedIndex, to)
        scope.launch { priorityListState.animateScrollToItem(to) }
    }

    fun edit(index: Int?) {
        editIndex = index
        val entry = index?.let { entries[it] }
        name = entry?.substringBefore('|').orEmpty()
        aliases = entry?.substringAfter('|', "").orEmpty()
        error = null
        editing = true
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().systemBarsPadding().padding(8.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("技能优先级", style = MaterialTheme.typography.titleLarge)
                Text("上方排顺序，下方点卡片添加；数字越小越优先。", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("已选优先级 · " + entries.size + " 项", style = MaterialTheme.typography.titleMedium)
                    if (focusedIndex >= 0) Text("当前第 " + (focusedIndex + 1) + " 项",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Surface(Modifier.fillMaxWidth().weight(.43f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f),
                    shape = MaterialTheme.shapes.medium) {
                    if (entries.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("还没有选择技能，点击下方卡片按顺序加入。")
                        }
                    } else {
                        LazyColumn(state = priorityListState, contentPadding = PaddingValues(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemsIndexed(entries, key = { _, entry -> entry }) { index, entry ->
                                Card(onClick = { focusedEntry = entry }, modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = if (entry == focusedEntry)
                                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                                    Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text((index + 1).toString(), modifier = Modifier.widthIn(min = 32.dp),
                                            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(SkillCatalog.title(entry), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        TextButton(modifier = Modifier.semantics { contentDescription = "移除" + SkillCatalog.title(entry) },
                                            onClick = {
                                                entries = entries.filterIndexed { i, _ -> i != index }
                                                if (focusedEntry == entry) focusedEntry = null
                                            }) { Text("移除") }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(enabled = focusedIndex > 0, onClick = {
                        moveFocused(focusedIndex - 1)
                    }) { Text("上移") }
                    TextButton(enabled = focusedIndex >= 0 && focusedIndex < entries.lastIndex, onClick = {
                        moveFocused(focusedIndex + 1)
                    }) { Text("下移") }
                    TextButton(enabled = focusedIndex > 0, onClick = {
                        moveFocused(0)
                    }) { Text("置顶") }
                    TextButton(enabled = focusedIndex >= 0, onClick = { edit(focusedIndex) }) { Text("编辑") }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("按类型选技能", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { edit(null) }) { Text("＋ 自定义") }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(.57f), contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { group ->
                        item(key = "group_" + group.id) {
                            val count = group.skills.count { SkillCatalog.selectedIndex(entries, it) >= 0 }
                            Text(group.title + "  " + count + "/" + group.skills.size, modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        items(group.skills.chunked(2), key = { group.id + "_" + it.first() }) { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { skill ->
                                    val index = SkillCatalog.selectedIndex(entries, skill)
                                    val selected = index >= 0
                                    val displayed = if (selected) entries[index] else skill
                                    OutlinedCard(onClick = {
                                        runCatching { SkillCatalog.toggle(entries, skill) }.onSuccess { updated ->
                                            entries = updated
                                            focusedEntry = if (selected) null else updated.last()
                                            if (!selected) scope.launch { priorityListState.animateScrollToItem(updated.lastIndex) }
                                            error = null
                                        }.onFailure { error = it.message ?: "无法添加技能" }
                                    }, modifier = Modifier.weight(1f),
                                        colors = CardDefaults.outlinedCardColors(containerColor = if (selected)
                                            MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(SkillCatalog.title(displayed), style = MaterialTheme.typography.titleSmall)
                                            Text(if (selected) "已选 #" + (index + 1) + " · 点击移除" else "＋ 点击加入优先级",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(enabled = entries.isNotEmpty(), onClick = {
                        runCatching { onSave(entries.toList()) }.onFailure { error = it.message ?: "保存失败" }
                    }) { Text(saveButtonText) }
                }
            }
        }
    }
    if (editing) {
        AlertDialog(onDismissRequest = { editing = false; error = null },
            title = { Text(if (editIndex == null) "添加自定义技能" else "编辑技能") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("技能名称（完整标题）") }, singleLine = true)
                    OutlinedTextField(aliases, { aliases = it }, label = { Text("别名（逗号或 | 分隔）") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }, confirmButton = {
                TextButton(onClick = {
                    runCatching { SkillPriorityDraft.replace(entries, editIndex, name, aliases) }.onSuccess { updated ->
                        entries = updated
                        val changed = updated[editIndex ?: updated.lastIndex]
                        catalogExtras = catalogExtras + changed
                        focusedEntry = changed
                        scope.launch { priorityListState.animateScrollToItem(editIndex ?: updated.lastIndex) }
                        editing = false
                        error = null
                    }.onFailure { error = it.message ?: "名称无效" }
                }) { Text("确定") }
            }, dismissButton = { TextButton(onClick = { editing = false; error = null }) { Text("取消") } })
    }
}

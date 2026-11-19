package com.zasko.apppoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zasko.apppoint.automation.SkillBuild
import com.zasko.apppoint.automation.SkillBuilds
import com.zasko.apppoint.automation.SkillBuildStore
import java.util.UUID

@Composable
fun SkillBuildManager(
    initial: List<SkillBuild>,
    onDismiss: () -> Unit,
    onSave: (List<SkillBuild>) -> Unit
) {
    var builds by remember(initial) { mutableStateOf(initial.toList()) }
    var editingBuildId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newBuildTitle by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val editingBuild = builds.firstOrNull { it.id == editingBuildId }
    val validationError = runCatching { SkillBuildStore.validate(builds) }.exceptionOrNull()?.message
    val canSave = validationError == null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(8.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("流派预设", style = MaterialTheme.typography.titleLarge)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(builds, key = { it.id }) { build ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = build.title,
                                        onValueChange = { title ->
                                            builds = builds.map { if (it.id == build.id) it.copy(title = title) else it }
                                            error = null
                                        },
                                        label = { Text("流派名称") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (build.id !in SkillBuilds.builtIns.map { it.id }) {
                                        TextButton(onClick = {
                                            builds = builds.filterNot { it.id == build.id }
                                            error = null
                                        }) { Text("删除") }
                                    }
                                }
                                Button(onClick = { editingBuildId = build.id }) {
                                    Text("编辑技能顺序 · ${build.priority.size} 项")
                                }
                            }
                        }
                    }
                }
                (error ?: validationError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { newBuildTitle = ""; showCreateDialog = true }) { Text("新增流派") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Button(enabled = canSave, onClick = {
                            runCatching { onSave(builds) }
                                .onFailure { error = it.message ?: "保存失败" }
                        }) { Text("保存流派") }
                    }
                }
            }
        }
    }

    editingBuild?.let { build ->
        SkillPriorityEditor(
            initial = build.priority,
            onDismiss = { editingBuildId = null },
            onSave = { priority ->
                builds = builds.map { if (it.id == build.id) it.copy(priority = priority) else it }
                editingBuildId = null
                error = null
            },
            saveButtonText = "保存到流派"
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新增流派") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(newBuildTitle, { newBuildTitle = it }, label = { Text("流派名称") }, singleLine = true)
                    Text("创建后添加技能并调整优先顺序。", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = "custom_${UUID.randomUUID()}"
                    builds = builds + SkillBuild(id, newBuildTitle.trim(), emptyList())
                    editingBuildId = id
                    showCreateDialog = false
                    error = null
                }, enabled = newBuildTitle.isNotBlank()) { Text("继续选技能") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("取消") } }
        )
    }
}

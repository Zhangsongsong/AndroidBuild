package com.zasko.imageloads.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.components.HttpHeaderConfigStore
import com.zasko.imageloads.components.HttpHeaderItem
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HttpHeadersSettingsActivity : BaseComposeActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, HttpHeadersSettingsActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadsTheme {
                HttpHeadersSettingsScreen(
                    onBack = ::finish,
                    showToast = ::showToast,
                )
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun HttpHeadersSettingsScreen(
    onBack: () -> Unit,
    showToast: (String) -> Unit,
) {
    var selectedTargetId by rememberSaveable { mutableStateOf(HttpHeaderConfigStore.TARGET_COMMON) }
    val headerRows = remember { mutableStateListOf<HeaderDraft>() }
    var nextHeaderId by remember { mutableStateOf(0) }
    val selectedTarget = HttpHeaderConfigStore.getTarget(id = selectedTargetId)

    LaunchedEffect(selectedTargetId) {
        val rows = withContext(Dispatchers.IO) {
            HttpHeaderConfigStore.getHeaders(targetId = selectedTargetId).toHeaderDrafts()
        }
        headerRows.replaceWith(rows)
        nextHeaderId = headerRows.nextId()
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ImageLoadsTopBar(
                title = "${selectedTarget.title} Headers",
                onBack = onBack,
                actions = {
                    HeaderTargetMenu(
                        selectedTargetId = selectedTargetId,
                        onTargetSelected = { targetId ->
                            selectedTargetId = targetId
                        },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "每一项对应一个 Header，来源同名项会覆盖公共项",
                color = colorResource(id = R.color.color_h2),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                onClick = {
                    headerRows.add(HeaderDraft(id = nextHeaderId))
                    nextHeaderId += 1
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_add_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "新增 Header",
                    modifier = Modifier.padding(start = 6.dp),
                    maxLines = 1,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                headerRows.forEachIndexed { index, item ->
                    HeaderEditorItem(
                        index = index,
                        item = item,
                        onNameChanged = { value ->
                            headerRows[index] = item.copy(name = value)
                        },
                        onValueChanged = { value ->
                            headerRows[index] = item.copy(value = value)
                        },
                        onDelete = {
                            if (headerRows.size > 1) {
                                headerRows.removeAt(index)
                            } else {
                                headerRows[index] = item.copy(name = "", value = "")
                            }
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        HttpHeaderConfigStore.resetHeaders(targetId = selectedTargetId)
                        headerRows.replaceWith(
                            HttpHeaderConfigStore.getHeaders(targetId = selectedTargetId).toHeaderDrafts(),
                        )
                        nextHeaderId = headerRows.nextId()
                        showToast("已恢复默认")
                    },
                ) {
                    Text(text = "恢复默认", maxLines = 1)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        val result = headerRows.parseHeaderItems()
                        if (result.invalidItems.isNotEmpty()) {
                            showToast("第 ${result.invalidItems.joinToString()} 项格式错误")
                        } else {
                            HttpHeaderConfigStore.saveHeaders(
                                targetId = selectedTargetId,
                                headers = result.headers,
                            )
                            showToast("已保存")
                        }
                    },
                ) {
                    Text(text = "保存", maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun HeaderEditorItem(
    index: Int,
    item: HeaderDraft,
    onNameChanged: (String) -> Unit,
    onValueChanged: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFD))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Header ${index + 1}",
                color = colorResource(id = R.color.color_h1),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onDelete,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_delete_24),
                    contentDescription = "删除",
                    tint = Color(0xFF5F6368),
                )
            }
        }
        OutlinedTextField(
            value = item.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = {
                Text(text = "Name")
            },
        )
        OutlinedTextField(
            value = item.value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5,
            label = {
                Text(text = "Value")
            },
        )
    }
}

@Composable
private fun HeaderTargetMenu(
    selectedTargetId: String,
    onTargetSelected: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = "来源")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            HttpHeaderConfigStore.targets.forEach { target ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = target.title,
                            fontWeight = if (target.id == selectedTargetId) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onTargetSelected(target.id)
                    },
                )
            }
        }
    }
}

private data class HeaderParseResult(
    val headers: List<HttpHeaderItem>,
    val invalidItems: List<Int>,
)

private data class HeaderDraft(
    val id: Int,
    val name: String = "",
    val value: String = "",
)

private fun List<HttpHeaderItem>.toHeaderDrafts(): List<HeaderDraft> {
    return mapIndexed { index, header ->
        HeaderDraft(id = index, name = header.name, value = header.value)
    }.ifEmpty {
        listOf(HeaderDraft(id = 0))
    }
}

private fun List<HeaderDraft>.parseHeaderItems(): HeaderParseResult {
    val headers = mutableListOf<HttpHeaderItem>()
    val invalidItems = mutableListOf<Int>()
    forEachIndexed { index, item ->
        val name = item.name.trim()
        val value = item.value.trim()
        if (name.isBlank() && value.isBlank()) {
            return@forEachIndexed
        }
        if (name.isValidHeaderName() && value.isNotBlank()) {
            headers.add(HttpHeaderItem(name = name, value = value))
        } else {
            invalidItems.add(index + 1)
        }
    }
    return HeaderParseResult(headers = headers, invalidItems = invalidItems)
}

private fun MutableList<HeaderDraft>.replaceWith(items: List<HeaderDraft>) {
    clear()
    addAll(items)
}

private fun List<HeaderDraft>.nextId(): Int {
    return (maxOfOrNull { it.id } ?: -1) + 1
}

private fun String.isValidHeaderName(): Boolean {
    return matches(Regex("""^[A-Za-z0-9!#$%&'*+.^_`|~-]+$"""))
}

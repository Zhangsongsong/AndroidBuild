package com.zasko.imageloads.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.ui.common.SourceProcessMethodStore
import com.zasko.imageloads.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ProcessMethodsSettingsActivity : BaseComposeActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ProcessMethodsSettingsActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadsTheme {
                ProcessMethodsSettingsScreen(
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

private data class ProcessMethodSource(
    val type: Int,
    val title: String,
)

private enum class ProcessMethodValueType {
    STRING,
    NUMBER,
    BOOLEAN,
}

private data class ProcessMethodDraft(
    val id: Int,
    val path: String,
    val group: String,
    val remark: String,
    val value: String,
    val type: ProcessMethodValueType,
)

private data class ProcessMethodBuildResult(
    val json: JSONObject?,
    val invalidPaths: List<String>,
)

private val processMethodSources = listOf(
    ProcessMethodSource(type = Constants.THEME_TYPE_TRENDSZINE, title = "Trendszine"),
    ProcessMethodSource(type = Constants.THEME_TYPE_MEIZI5, title = "Meizi5"),
    ProcessMethodSource(type = Constants.THEME_TYPE_TAOTU, title = "TaoTu"),
)

private const val GROUP_LIST_PREVIEW = "封面预览页面"
private const val GROUP_LIST_PAGINATION = "封面预览分页"
private const val GROUP_DETAIL_PAGE = "详情页面"
private const val GROUP_DETAIL_PAGINATION = "详情分页"
private const val GROUP_OTHER = "其他处理配置"

private val processMethodGroupOrder = listOf(
    GROUP_LIST_PREVIEW,
    GROUP_LIST_PAGINATION,
    GROUP_DETAIL_PAGE,
    GROUP_DETAIL_PAGINATION,
    GROUP_OTHER,
)

@Composable
private fun ProcessMethodsSettingsScreen(
    onBack: () -> Unit,
    showToast: (String) -> Unit,
) {
    var selectedSourceType by rememberSaveable { mutableStateOf(processMethodSources.first().type) }
    val selectedSource = processMethodSources.firstOrNull { it.type == selectedSourceType }
        ?: processMethodSources.first()
    val rows = remember { mutableStateListOf<ProcessMethodDraft>() }

    LaunchedEffect(selectedSourceType) {
        val draftRows = withContext(Dispatchers.IO) {
            SourceProcessMethodStore.getOrCacheMethods(sourceType = selectedSourceType)
                .toProcessMethodDrafts()
        }
        rows.replaceWith(draftRows)
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ImageLoadsTopBar(
                title = "${selectedSource.title} 处理方法",
                onBack = onBack,
                actions = {
                    ProcessMethodSourceMenu(
                        selectedSource = selectedSource,
                        onSourceSelected = { source ->
                            selectedSourceType = source.type
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
                text = "逐项编辑处理方法，保存时才会写回来源 JSON",
                color = colorResource(id = R.color.color_h2),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                processMethodGroupOrder.forEach { group ->
                    val groupRows = rows.withIndex()
                        .filter { indexedRow -> indexedRow.value.group == group }
                    if (groupRows.isNotEmpty()) {
                        ProcessMethodGroupHeader(title = group)
                        groupRows.forEach { indexedRow ->
                            ProcessMethodEditorItem(
                                index = indexedRow.index,
                                row = indexedRow.value,
                                onValueChanged = { value ->
                                    rows[indexedRow.index] = indexedRow.value.copy(value = value)
                                },
                            )
                        }
                    }
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
                        val defaultRows = SourceProcessMethodStore
                            .resetDefaultMethods(sourceType = selectedSource.type)
                            .toProcessMethodDrafts()
                        rows.replaceWith(defaultRows)
                        showToast("已恢复默认")
                    },
                ) {
                    Text(text = "恢复默认", maxLines = 1)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        val result = rows.toProcessMethodsJson()
                        val json = result.json
                        if (json == null) {
                            showToast("格式错误: ${result.invalidPaths.joinToString()}")
                        } else {
                            SourceProcessMethodStore.saveMethods(
                                sourceType = selectedSource.type,
                                methods = json,
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
private fun ProcessMethodGroupHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE8F0FE))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        color = Color(0xFF1967D2),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ProcessMethodEditorItem(
    index: Int,
    row: ProcessMethodDraft,
    onValueChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFD))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = row.remark.ifBlank { "处理项 ${index + 1}" },
            color = colorResource(id = R.color.color_h1),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = row.path,
            color = Color(0xFF5F6368),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = row.value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 5,
            label = {
                Text(text = row.type.name.lowercase())
            },
        )
    }
}

@Composable
private fun ProcessMethodSourceMenu(
    selectedSource: ProcessMethodSource,
    onSourceSelected: (ProcessMethodSource) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = selectedSource.title)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            processMethodSources.forEach { source ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = source.title,
                            fontWeight = if (source.type == selectedSource.type) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onSourceSelected(source)
                    },
                )
            }
        }
    }
}

private fun JSONObject.toProcessMethodDrafts(): List<ProcessMethodDraft> {
    val rows = mutableListOf<ProcessMethodDraft>()
    appendProcessMethodRows(value = this, path = "", rows = rows)
    return rows.mapIndexed { index, row -> row.copy(id = index) }
}

private fun appendProcessMethodRows(
    value: Any?,
    path: String,
    rows: MutableList<ProcessMethodDraft>,
) {
    if (path.isHiddenProcessMethod()) {
        return
    }
    when (value) {
        is JSONObject -> {
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val childPath = if (path.isBlank()) key else "$path.$key"
                appendProcessMethodRows(value = value.opt(key), path = childPath, rows = rows)
            }
        }

        is JSONArray -> {
            for (index in 0 until value.length()) {
                appendProcessMethodRows(
                    value = value.opt(index),
                    path = "$path[$index]",
                    rows = rows,
                )
            }
        }

        is Number -> {
            rows.add(
                ProcessMethodDraft(
                    id = rows.size,
                    path = path,
                    group = path.toProcessMethodGroup(),
                    remark = path.toProcessMethodRemark(),
                    value = value.toString(),
                    type = ProcessMethodValueType.NUMBER,
                ),
            )
        }

        is Boolean -> {
            rows.add(
                ProcessMethodDraft(
                    id = rows.size,
                    path = path,
                    group = path.toProcessMethodGroup(),
                    remark = path.toProcessMethodRemark(),
                    value = value.toString(),
                    type = ProcessMethodValueType.BOOLEAN,
                ),
            )
        }

        else -> {
            rows.add(
                ProcessMethodDraft(
                    id = rows.size,
                    path = path,
                    group = path.toProcessMethodGroup(),
                    remark = path.toProcessMethodRemark(),
                    value = value?.toString().orEmpty(),
                    type = ProcessMethodValueType.STRING,
                ),
            )
        }
    }
}

private fun List<ProcessMethodDraft>.toProcessMethodsJson(): ProcessMethodBuildResult {
    val root = JSONObject()
    val invalidPaths = mutableListOf<String>()
    forEach { row ->
        val value = row.parseValueOrNull()
        if (value == null && row.type != ProcessMethodValueType.STRING) {
            invalidPaths.add(row.path)
        } else {
            runCatching {
                root.putPath(path = row.path, value = value ?: row.value)
            }.onFailure {
                invalidPaths.add(row.path)
            }
        }
    }
    return ProcessMethodBuildResult(
        json = if (invalidPaths.isEmpty()) root else null,
        invalidPaths = invalidPaths,
    )
}

private fun ProcessMethodDraft.parseValueOrNull(): Any? {
    return when (type) {
        ProcessMethodValueType.STRING -> value
        ProcessMethodValueType.NUMBER -> value.trim().toLongOrNull()
            ?: value.trim().toDoubleOrNull()

        ProcessMethodValueType.BOOLEAN -> when (value.trim().lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }
}

private fun JSONObject.putPath(path: String, value: Any) {
    val tokens = path.toPathTokens()
    if (tokens.isEmpty()) {
        return
    }
    putPathValue(container = this, tokens = tokens, tokenIndex = 0, value = value)
}

private fun putPathValue(
    container: Any,
    tokens: List<ProcessPathToken>,
    tokenIndex: Int,
    value: Any,
) {
    val token = tokens[tokenIndex]
    val isLast = tokenIndex == tokens.lastIndex
    when (container) {
        is JSONObject -> {
            val key = token as? ProcessPathToken.Key
                ?: error("Object path expected key")
            if (isLast) {
                container.put(key.name, value)
                return
            }
            val nextToken = tokens[tokenIndex + 1]
            val nextValue = container.opt(key.name)
            val nextContainer = when {
                nextValue is JSONObject -> nextValue
                nextValue is JSONArray -> nextValue
                nextToken is ProcessPathToken.Index -> JSONArray().also {
                    container.put(key.name, it)
                }

                else -> JSONObject().also {
                    container.put(key.name, it)
                }
            }
            putPathValue(
                container = nextContainer,
                tokens = tokens,
                tokenIndex = tokenIndex + 1,
                value = value,
            )
        }

        is JSONArray -> {
            val index = token as? ProcessPathToken.Index
                ?: error("Array path expected index")
            container.ensureSize(index.index + 1)
            if (isLast) {
                container.put(index.index, value)
                return
            }
            val nextToken = tokens[tokenIndex + 1]
            val nextValue = container.opt(index.index)
            val nextContainer = when {
                nextValue is JSONObject -> nextValue
                nextValue is JSONArray -> nextValue
                nextToken is ProcessPathToken.Index -> JSONArray().also {
                    container.put(index.index, it)
                }

                else -> JSONObject().also {
                    container.put(index.index, it)
                }
            }
            putPathValue(
                container = nextContainer,
                tokens = tokens,
                tokenIndex = tokenIndex + 1,
                value = value,
            )
        }
    }
}

private fun JSONArray.ensureSize(size: Int) {
    while (length() < size) {
        put(JSONObject.NULL)
    }
}

private sealed interface ProcessPathToken {
    data class Key(val name: String) : ProcessPathToken
    data class Index(val index: Int) : ProcessPathToken
}

private fun String.toPathTokens(): List<ProcessPathToken> {
    val tokens = mutableListOf<ProcessPathToken>()
    split('.').forEach { segment ->
        val key = segment.substringBefore('[')
        if (key.isNotBlank()) {
            tokens.add(ProcessPathToken.Key(key))
        }
        Regex("""\[(\d+)]""").findAll(segment).forEach { match ->
            tokens.add(ProcessPathToken.Index(match.groupValues[1].toInt()))
        }
    }
    return tokens
}

private fun String.toProcessMethodGroup(): String {
    return when {
        startsWith("list.request.") ||
            startsWith("list.pageUrl.") ||
            startsWith("list.parse.") -> GROUP_LIST_PREVIEW

        startsWith("list.pagination.") -> GROUP_LIST_PAGINATION

        startsWith("detail.request.") ||
            startsWith("detail.parse.") -> GROUP_DETAIL_PAGE

        startsWith("detail.pagination.") -> GROUP_DETAIL_PAGINATION

        else -> GROUP_OTHER
    }
}

private fun String.isHiddenProcessMethod(): Boolean {
    return this == "parser" ||
        startsWith("parser.") ||
        this == "list.cache" ||
        startsWith("list.cache.") ||
        this == "detail.cache" ||
        startsWith("detail.cache.")
}

private fun String.toProcessMethodRemark(): String {
    val normalizedPath = replace(Regex("""\[\d+]"""), "[]")
    return when (normalizedPath) {
        "list.request.homeUrl" -> "列表首页请求地址"
        "list.pageUrl.firstPage" -> "列表第一页 URL 生成规则"
        "list.pageUrl.nextPage" -> "列表下一页 URL 生成规则"
        "list.parse.itemSelector" -> "列表中每个封面 item 的 CSS 选择器"
        "list.parse.detailLinkSelector" -> "从封面 item 中获取详情链接的 CSS 选择器"
        "list.parse.coverSelector" -> "从封面 item 中获取封面图片的 CSS 选择器"
        "list.parse.coverAttrOrder[]" -> "封面图片 URL 读取属性的优先级"
        "list.parse.titleSelector" -> "封面标题的 CSS 选择器"
        "list.parse.categorySelector" -> "一级分类 item 的 CSS 选择器"
        "list.parse.categoryLinkSelector" -> "一级分类链接的 CSS 选择器"
        "list.parse.childrenCategorySelector" -> "二级分类链接的 CSS 选择器"
        "list.parse.titleAttrOrder[]" -> "封面标题读取来源的优先级"
        "list.pagination.nextPageSelector" -> "判断列表是否存在下一页的 CSS 选择器"
        "list.pagination.nextPageValue" -> "列表下一页页码的计算方式"
        "list.pagination.currentPageSelector" -> "从页面读取当前页码的 CSS 选择器"
        "list.pagination.currentPagePattern" -> "从分页文本中提取页码的正则"

        "detail.request.detailUrl" -> "详情页请求地址来源"
        "detail.parse.titleSelector" -> "详情标题的 CSS 选择器"
        "detail.parse.dateSelector" -> "详情发布日期的 CSS 选择器"
        "detail.parse.tagSelector" -> "详情标签的 CSS 选择器"
        "detail.parse.contentSelector" -> "详情正文容器的 CSS 选择器"
        "detail.parse.imageSelector" -> "详情图片的 CSS 选择器"
        "detail.parse.imageAttrOrder[]" -> "详情图片 URL 读取属性的优先级"
        "detail.parse.imageLinkSelector" -> "详情大图链接的 CSS 选择器"
        "detail.parse.imageAttr" -> "详情大图 URL 的读取属性"
        "detail.parse.thumbSelector" -> "详情缩略图的 CSS 选择器"
        "detail.parse.thumbAttr" -> "详情缩略图 URL 的读取属性"
        "detail.pagination.containerSelector" -> "详情分页容器的 CSS 选择器"
        "detail.pagination.linkSelector" -> "详情分页链接的 CSS 选择器"
        "detail.pagination.nextPageValue" -> "详情下一页 URL 的计算方式"

        else -> "处理配置: $this"
    }
}

private fun MutableList<ProcessMethodDraft>.replaceWith(items: List<ProcessMethodDraft>) {
    clear()
    addAll(items)
}

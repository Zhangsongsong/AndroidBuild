package com.zasko.imageloads.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.components.HttpComponent
import com.zasko.imageloads.components.HttpHeaderConfigStore
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.common.SourceProcessMethodStore
import com.zasko.imageloads.ui.meizi5.Meizi5Repository
import com.zasko.imageloads.ui.taotu.TaoTuRepository
import com.zasko.imageloads.ui.trendszine.TrendszineRepository
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.MJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class LabActivity : BaseComposeActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, LabActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadsTheme {
                LabScreen(
                    onBack = ::finish,
                    onOpenHttpHeadersSettings = {
                        HttpHeadersSettingsActivity.start(context = this)
                    },
                    onOpenProcessMethodsSettings = {
                        ProcessMethodsSettingsActivity.start(context = this)
                    },
                    showToast = ::showToast,
                )
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

private data class LabSource(
    val type: Int,
    val key: String,
    val title: String,
    val defaultUrl: String,
)

private data class LabProcessMode(
    val key: String,
    val title: String,
)

private data class LabRequestResult(
    val code: Int,
    val message: String,
    val contentType: String,
    val bodyLength: Int,
    val bodyPreview: String,
    val headers: String,
    val connectionMode: String,
)

private const val LAB_MODE_REQUEST = "request"
private const val LAB_MODE_LIST = "list"
private const val LAB_MODE_DETAIL = "detail"
private const val LAB_MODE_METHODS = "methods"

private val labSources = listOf(
    LabSource(
        type = Constants.THEME_TYPE_TRENDSZINE,
        key = HttpHeaderConfigStore.TARGET_TRENDSZINE,
        title = "Trendszine",
        defaultUrl = "https://trendszine.com/",
    ),
    LabSource(
        type = Constants.THEME_TYPE_MEIZI5,
        key = HttpHeaderConfigStore.TARGET_MEIZI5,
        title = "Meizi5",
        defaultUrl = "https://meizi5.com/",
    ),
    LabSource(
        type = Constants.THEME_TYPE_TAOTU,
        key = HttpHeaderConfigStore.TARGET_TAOTU,
        title = "TaoTu",
        defaultUrl = "https://taotu.org/",
    ),
)

private val labProcessModes = listOf(
    LabProcessMode(key = LAB_MODE_REQUEST, title = "原始请求"),
    LabProcessMode(key = LAB_MODE_LIST, title = "列表解析"),
    LabProcessMode(key = LAB_MODE_DETAIL, title = "详情解析"),
    LabProcessMode(key = LAB_MODE_METHODS, title = "处理方法缓存"),
)

private val labHttpClient by lazy {
    HttpComponent.createHeaderAwareClient()
}

private val labCompatibleHttpClient by lazy {
    HttpComponent.createCompatibleHeaderAwareClient()
}

@Composable
private fun LabScreen(
    onBack: () -> Unit,
    onOpenHttpHeadersSettings: () -> Unit,
    onOpenProcessMethodsSettings: () -> Unit,
    showToast: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedSourceKey by rememberSaveable { mutableStateOf(labSources.first().key) }
    var selectedProcessModeKey by rememberSaveable { mutableStateOf(labProcessModes.first().key) }
    val selectedSource = labSources.firstOrNull { it.key == selectedSourceKey } ?: labSources.first()
    val selectedProcessMode = labProcessModes.firstOrNull { it.key == selectedProcessModeKey } ?: labProcessModes.first()
    var url by rememberSaveable { mutableStateOf(selectedSource.defaultUrl) }
    var resultText by remember { mutableStateOf("") }
    var isRequesting by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ImageLoadsTopBar(
                title = "实验室",
                onBack = onBack,
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
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                onClick = onOpenHttpHeadersSettings,
            ) {
                Text(text = "HTTP Headers 设置", maxLines = 1)
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                onClick = onOpenProcessMethodsSettings,
            ) {
                Text(text = "处理方法缓存设置", maxLines = 1)
            }
            Text(
                text = "请求会使用当前来源 Header；MJson 解析规则会写入来源缓存",
                color = Color(0xFF5F6368),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LabSourceMenu(
                    modifier = Modifier.weight(1f),
                    selectedSource = selectedSource,
                    onSourceSelected = { source ->
                        selectedSourceKey = source.key
                        url = source.defaultUrl
                        resultText = ""
                    },
                )
                LabProcessModeMenu(
                    modifier = Modifier.weight(1f),
                    selectedMode = selectedProcessMode,
                    onModeSelected = { mode ->
                        selectedProcessModeKey = mode.key
                        resultText = ""
                    },
                )
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isRequesting,
                contentPadding = PaddingValues(horizontal = 12.dp),
                onClick = {
                    val requestUrl = url.trim()
                    if (selectedProcessMode.key != LAB_MODE_METHODS && requestUrl.isBlank()) {
                        showToast("请先输入链接")
                        return@Button
                    }
                    isRequesting = true
                    resultText = ""
                    scope.launch {
                        try {
                            resultText = withContext(Dispatchers.IO) {
                                when (selectedProcessMode.key) {
                                    LAB_MODE_METHODS -> getCachedProcessMethodsText(source = selectedSource)
                                    LAB_MODE_LIST,
                                    LAB_MODE_DETAIL,
                                    -> processMJson(
                                        source = selectedSource,
                                        mode = selectedProcessMode,
                                        url = requestUrl,
                                    )

                                    else -> testRequest(url = requestUrl).toDisplayText()
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (throwable: Throwable) {
                            resultText = "处理失败\n${throwable.message ?: throwable.toString()}"
                        } finally {
                            isRequesting = false
                        }
                    }
                },
            ) {
                Text(text = if (isRequesting) "处理中" else selectedProcessMode.title, maxLines = 1)
            }
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                label = {
                    Text(text = "链接")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            if (isRequesting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Text(
                    text = resultText.ifBlank { "暂无请求结果" },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFD))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    color = Color(0xFF3C4043),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabProcessModeMenu(
    selectedMode: LabProcessMode,
    modifier: Modifier = Modifier,
    onModeSelected: (LabProcessMode) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedMode.title,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = {
                Text(text = "处理方式")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF1A73E8),
                unfocusedBorderColor = Color(0xFF8A94A6),
                focusedLabelColor = Color(0xFF1A73E8),
                unfocusedLabelColor = Color(0xFF5F6368),
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            labProcessModes.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mode.title,
                            fontWeight = if (mode.key == selectedMode.key) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onModeSelected(mode)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabSourceMenu(
    selectedSource: LabSource,
    modifier: Modifier = Modifier,
    onSourceSelected: (LabSource) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedSource.title,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = {
                Text(text = "来源")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF1A73E8),
                unfocusedBorderColor = Color(0xFF8A94A6),
                focusedLabelColor = Color(0xFF1A73E8),
                unfocusedLabelColor = Color(0xFF5F6368),
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            labSources.forEach { source ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = source.title,
                            fontWeight = if (source.key == selectedSource.key) {
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

private fun testRequest(url: String): LabRequestResult {
    val headers = HttpHeaderConfigStore.getHeadersForUrl(url = url)
    val request = Request.Builder()
        .url(url)
        .get()
        .build()
    val (response, connectionMode) = executeLabRequest(request = request)

    response.use { httpResponse ->
        val responseBody = httpResponse.body
        val contentType = responseBody?.contentType()?.toString().orEmpty()
        val bodyText = responseBody?.string().orEmpty()
        return LabRequestResult(
            code = httpResponse.code,
            message = httpResponse.message,
            contentType = contentType,
            bodyLength = bodyText.length,
            bodyPreview = bodyText,
            headers = headers.joinToString(separator = "\n") { "${it.name}: ${it.value}" },
            connectionMode = connectionMode,
        )
    }
}

private fun getCachedProcessMethodsText(source: LabSource): String {
    val methods = SourceProcessMethodStore.getOrCacheMethods(sourceType = source.type)
    return buildString {
        appendLine("${source.title} 处理方法已写入来源缓存")
        appendLine()
        append(methods.toString(2))
    }
}

private fun processMJson(
    source: LabSource,
    mode: LabProcessMode,
    url: String,
): String {
    val requestResult = testRequest(url = url)
    val html = requestResult.bodyPreview
    val methods = SourceProcessMethodStore.getOrCacheMethods(sourceType = source.type)
    val doc = MJson.parse(html)
    val parsedText = when (mode.key) {
        LAB_MODE_LIST -> parseListHtml(source = source, html = html)
        LAB_MODE_DETAIL -> parseDetailHtml(source = source, url = url, html = html)
        else -> ""
    }
    return buildString {
        appendLine("MJson: ${source.title} ${mode.title}")
        appendLine("Document title: ${doc.title().ifBlank { "-" }}")
        appendLine("article 数: ${doc.select("article").size}")
        appendLine("img 数: ${doc.select("img").size}")
        appendLine("a[href] 数: ${doc.select("a[href]").size}")
        appendLine()
        appendLine("请求信息:")
        appendLine("状态: ${requestResult.code} ${requestResult.message}")
        appendLine("连接模式: ${requestResult.connectionMode}")
        appendLine("Body 长度: ${requestResult.bodyLength}")
        appendLine()
        appendLine("解析结果:")
        appendLine(parsedText.ifBlank { "-" })
        appendLine()
        appendLine("处理方法缓存:")
        append(methods.toString(2))
    }
}

private fun parseListHtml(source: LabSource, html: String): String {
    return when (source.type) {
        Constants.THEME_TYPE_TRENDSZINE -> {
            val result = TrendszineRepository.transformHome(data = html, page = 1)
            buildString {
                appendLine("封面数: ${result.images.size}")
                appendLine("分类数: ${result.categories.size}")
                appendLine("下一页: ${result.nextPage ?: "-"}")
                appendImageSamples(result.images)
            }
        }

        Constants.THEME_TYPE_MEIZI5 -> {
            val images = Meizi5Repository.transformHome(data = html)
            buildString {
                appendLine("封面数: ${images.size}")
                appendLine("下一页: 手动 page + 1")
                appendImageSamples(images)
            }
        }

        Constants.THEME_TYPE_TAOTU -> {
            val result = TaoTuRepository.transformHome(data = html)
            buildString {
                appendLine("封面数: ${result.images.size}")
                appendLine("下一页: ${result.nextPage ?: "-"}")
                appendImageSamples(result.images)
            }
        }

        else -> "-"
    }
}

private fun parseDetailHtml(source: LabSource, url: String, html: String): String {
    return when (source.type) {
        Constants.THEME_TYPE_TRENDSZINE -> {
            val detail = TrendszineRepository.transformDetail(url = url, data = html)
            buildString {
                appendLine("标题: ${detail.title.ifBlank { "-" }}")
                appendLine("日期: ${detail.date.ifBlank { "-" }}")
                appendLine("标签: ${detail.tags.joinToString(" / ").ifBlank { "-" }}")
                appendLine("图片数: ${detail.pictures.size}")
                appendLine("下一页详情: ${detail.nextPageUrl.ifBlank { "-" }}")
                appendImageSamples(detail.pictures)
            }
        }

        Constants.THEME_TYPE_MEIZI5 -> {
            val detail = Meizi5Repository.transformDetail(url = url, data = html)
            buildString {
                appendLine("标题: ${detail.title.ifBlank { "-" }}")
                appendLine("日期: ${detail.date.ifBlank { "-" }}")
                appendLine("标签: ${detail.tags.joinToString(" / ").ifBlank { "-" }}")
                appendLine("图片数: ${detail.pictures.size}")
                appendImageSamples(detail.pictures)
            }
        }

        Constants.THEME_TYPE_TAOTU -> {
            val detail = TaoTuRepository.transformDetail(url = url, data = html)
            buildString {
                appendLine("标题: ${detail.title.ifBlank { "-" }}")
                appendLine("标签: ${detail.tags.joinToString(" / ").ifBlank { "-" }}")
                appendLine("图片数: ${detail.pictures.size}")
                appendImageSamples(detail.pictures)
            }
        }

        else -> "-"
    }
}

private fun StringBuilder.appendImageSamples(images: List<ImageLoadsInfo>) {
    if (images.isEmpty()) {
        return
    }
    appendLine()
    appendLine("前 ${minOf(images.size, 5)} 条:")
    images.take(5).forEachIndexed { index, info ->
        appendLine("${index + 1}. ${info.title.ifBlank { "(无标题)" }}")
        appendLine("   cover=${info.url}")
        appendLine("   detail=${info.href.ifBlank { "-" }}")
    }
}

private fun executeLabRequest(request: Request): Pair<Response, String> {
    return try {
        labHttpClient.newCall(request).execute() to "默认模式"
    } catch (exception: IOException) {
        if (!HttpComponent.isTlsConnectionReset(exception)) {
            throw exception
        }
        labCompatibleHttpClient.newCall(request).execute() to "兼容模式 HTTP/1.1"
    }
}

private fun LabRequestResult.toDisplayText(): String {
    return buildString {
        appendLine("状态: $code $message")
        appendLine("Content-Type: ${contentType.ifBlank { "-" }}")
        appendLine("Body 长度: $bodyLength")
        appendLine("连接模式: $connectionMode")
        appendLine()
        appendLine("使用的 Header:")
        appendLine(headers.ifBlank { "-" })
        appendLine()
        appendLine("Body:")
        append(bodyPreview.ifBlank { "-" })
    }
}

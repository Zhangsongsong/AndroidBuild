package com.zasko.imageloads.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.components.HttpComponent
import com.zasko.imageloads.components.HttpHeaderConfigStore
import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.common.DynamicSourceStore
import com.zasko.imageloads.ui.common.SourceProcessMethodStore
import com.zasko.imageloads.ui.generic.GenericSourceRepository
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
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Element
import java.io.IOException
import kotlin.math.max

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
                    onOpenHttpHeadersSettings = { targetId ->
                        HttpHeadersSettingsActivity.start(context = this, targetId = targetId)
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
    val isBuiltIn: Boolean = true,
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

private data class LabProcessMethodTestDisplay(
    val summary: String,
    val body: String,
)

private const val LAB_TEST_SAMPLE_LIMIT = 8

private val builtInLabSources = listOf(
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

private val labHttpClient by lazy {
    HttpComponent.createHeaderAwareClient()
}

private val labCompatibleHttpClient by lazy {
    HttpComponent.createCompatibleHeaderAwareClient()
}

@Composable
private fun LabScreen(
    onBack: () -> Unit,
    onOpenHttpHeadersSettings: (String) -> Unit,
    showToast: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableStateOf(0) }
    var sourceOptions by remember { mutableStateOf(loadLabSources()) }
    var selectedSourceKey by rememberSaveable { mutableStateOf(sourceOptions.first().key) }
    val selectedSource = sourceOptions.firstOrNull { it.key == selectedSourceKey } ?: sourceOptions.first()
    var processMethodOptions by remember { mutableStateOf(getLabProcessMethodOptions(source = selectedSource)) }
    var testingOption by remember { mutableStateOf<ProcessMethodDraft?>(null) }
    var useCommonHeaders by rememberSaveable {
        mutableStateOf(HttpHeaderConfigStore.isCommonHeadersEnabledForTarget(selectedSource.key))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTick += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(resumeTick) {
        val refreshedSources = loadLabSources()
        sourceOptions = refreshedSources
        if (refreshedSources.none { it.key == selectedSourceKey }) {
            selectedSourceKey = refreshedSources.first().key
        }
    }

    LaunchedEffect(selectedSource.key, resumeTick) {
        useCommonHeaders = HttpHeaderConfigStore.isCommonHeadersEnabledForTarget(selectedSource.key)
        processMethodOptions = getLabProcessMethodOptions(source = selectedSource)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ImageLoadsTopBar(
                title = "",
                onBack = onBack,
                titleContent = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = {
                                    onOpenHttpHeadersSettings(HttpHeaderConfigStore.TARGET_COMMON)
                                },
                            ) {
                                Text(text = "公共 Header", maxLines = 1)
                            }
                            Switch(
                                checked = useCommonHeaders,
                                onCheckedChange = { enabled ->
                                    useCommonHeaders = enabled
                                    HttpHeaderConfigStore.setCommonHeadersEnabledForTarget(
                                        targetId = selectedSource.key,
                                        enabled = enabled,
                                    )
                                },
                            )
                        }
                    }
                },
                actions = {
                    LabSourceMenu(
                        selectedSource = selectedSource,
                        sources = sourceOptions,
                        onSourceSelected = { source ->
                            selectedSourceKey = source.key
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
            LabSettingCard(
                title = "来源 Header",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onOpenHttpHeadersSettings(selectedSource.key)
                },
            )
            Text(
                text = "按分类查看处理方法，每一项都可以单独弹窗测试",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                val groupedOptions = processMethodOptions.groupBy { it.group }
                if (groupedOptions.isEmpty()) {
                    Text(
                        text = "当前来源没有处理方法配置",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    groupedOptions.forEach { (group, options) ->
                        LabProcessMethodGroupHeader(title = group)
                        options.forEach { option ->
                            LabProcessMethodItem(
                                option = option,
                                onTestClick = {
                                    testingOption = option
                                },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    testingOption?.let { option ->
        LabProcessMethodTestDialog(
            source = selectedSource,
            option = option,
            onDismiss = {
                testingOption = null
            },
            onOptionSaved = { savedOption ->
                processMethodOptions = getLabProcessMethodOptions(source = selectedSource)
                testingOption = savedOption
            },
            showToast = showToast,
        )
    }
}

@Composable
private fun LabSettingCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedCard(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                color = colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LabProcessMethodGroupHeader(title: String) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colorScheme.primary.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        color = colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun LabProcessMethodItem(
    option: ProcessMethodDraft,
    onTestClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = option.remark.ifBlank { option.path },
                color = colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option.path,
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option.value.ifBlank { "-" },
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onTestClick) {
            Text(text = "测试", maxLines = 1)
        }
    }
}

@Composable
private fun LabProcessMethodTestDialog(
    source: LabSource,
    option: ProcessMethodDraft,
    onDismiss: () -> Unit,
    onOptionSaved: (ProcessMethodDraft) -> Unit,
    showToast: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var url by remember(source.key, option.path) { mutableStateOf(source.defaultUrl) }
    var optionValue by remember(source.key, option.path, option.value) { mutableStateOf(option.value) }
    var resultText by remember(source.key, option.path) { mutableStateOf("") }
    var resultBodyText by remember(source.key, option.path) { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    val resultScrollState = rememberScrollState()
    val bodyScrollState = rememberScrollState()

    LaunchedEffect(resultText) {
        resultScrollState.scrollTo(0)
    }

    LaunchedEffect(resultBodyText) {
        bodyScrollState.scrollTo(0)
    }

    Dialog(
        onDismissRequest = {
            if (!isTesting) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = option.remark.ifBlank { option.path },
                        color = colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = option.path,
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    label = {
                        Text(text = "测试链接")
                    },
                )
                OutlinedTextField(
                    value = optionValue,
                    onValueChange = { optionValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 5,
                    label = {
                        Text(text = "处理值")
                    },
                )
                if (isTesting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LabScrollableResultPanel(
                        title = "测试结果",
                        text = resultText.ifBlank { "暂无测试结果" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        scrollState = resultScrollState,
                    )
                    LabScrollableResultPanel(
                        title = "Body",
                        text = resultBodyText.ifBlank { "暂无 Body 数据" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.25f),
                        scrollState = bodyScrollState,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        enabled = !isTesting,
                        onClick = onDismiss,
                    ) {
                        Text(text = "关闭")
                    }
                    TextButton(
                        enabled = !isTesting,
                        onClick = {
                            val saveResult = saveLabProcessMethodOption(
                                source = source,
                                option = option,
                                value = optionValue,
                            )
                            if (saveResult.isSuccess) {
                                val savedOption = option.copy(value = optionValue)
                                onOptionSaved(savedOption)
                                showToast("已保存")
                            } else {
                                showToast(saveResult.exceptionOrNull()?.message ?: "保存失败")
                            }
                        },
                    ) {
                        Text(text = "确认保存")
                    }
                    Button(
                        enabled = !isTesting,
                        onClick = {
                            val requestUrl = url.trim()
                            if (requestUrl.isBlank()) {
                                showToast("请先输入链接")
                                return@Button
                            }
                            isTesting = true
                            resultBodyText = ""
                            resultText = buildString {
                                appendLine("请求链接: $requestUrl")
                                appendLine()
                                append("正在请求...")
                            }
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        testProcessMethodOption(
                                            source = source,
                                            url = requestUrl,
                                            option = option.copy(value = optionValue),
                                        )
                                    }
                                    resultText = result.summary
                                    resultBodyText = result.body
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (throwable: Throwable) {
                                    resultBodyText = ""
                                    resultText = "处理失败\n${throwable.message ?: throwable.toString()}"
                                } finally {
                                    isTesting = false
                                }
                            }
                        },
                    ) {
                        Text(text = "开始测试")
                    }
                }
            }
        }
    }
}

@Composable
private fun LabScrollableResultPanel(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            color = colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surfaceVariant)
                    .labResultScrollbar(scrollState = scrollState),
            ) {
                Text(
                    text = text,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(start = 12.dp, top = 12.dp, end = 18.dp, bottom = 12.dp),
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun Modifier.labResultScrollbar(scrollState: ScrollState): Modifier {
    return drawWithContent {
        drawContent()
        val maxScroll = scrollState.maxValue
        if (maxScroll <= 0) {
            return@drawWithContent
        }
        val viewportHeight = size.height
        val contentHeight = viewportHeight + maxScroll
        val thumbWidth = 3.dp.toPx()
        val thumbHeight = max(24.dp.toPx(), viewportHeight * viewportHeight / contentHeight)
        val availableHeight = viewportHeight - thumbHeight
        val top = scrollState.value.toFloat() / maxScroll.toFloat() * availableHeight
        drawRoundRect(
            color = Color(0xFF9AA0A6).copy(alpha = 0.72f),
            topLeft = Offset(size.width - thumbWidth - 3.dp.toPx(), top),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f),
        )
    }
}

@Composable
private fun LabSourceMenu(
    selectedSource: LabSource,
    sources: List<LabSource>,
    onSourceSelected: (LabSource) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Row {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = "${selectedSource.title} v",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            sources.forEach { source ->
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

private data class LabElementContext(
    val label: String,
    val element: Element,
)

private fun loadLabSources(): List<LabSource> {
    return builtInLabSources + DynamicSourceStore.getDynamicThemes().map { theme ->
        LabSource(
            type = theme.theme,
            key = theme.sourceKey,
            title = theme.title,
            defaultUrl = theme.baseUrl,
            isBuiltIn = false,
        )
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

private fun getLabProcessMethodOptions(source: LabSource): List<ProcessMethodDraft> {
    return getLabProcessMethods(source = source).toProcessMethodDrafts()
}

private fun testProcessMethodOption(
    source: LabSource,
    url: String,
    option: ProcessMethodDraft,
): LabProcessMethodTestDisplay {
    val methods = runCatching {
        getLabProcessMethods(source = source)
            .copyForLabEdit()
            .putLabProcessMethodValue(option = option, value = option.value)
    }.getOrElse { throwable ->
        return LabProcessMethodTestDisplay(
            summary = "处理项配置错误\n${throwable.message ?: throwable.toString()}",
            body = "",
        )
    }
    val requestResult = testRequest(url = url)
    val html = requestResult.bodyPreview
    val doc = MJson.parse(html)
    val testResult = when {
        option.path.endsWith("Selector") -> testSelectorProcessMethod(
            doc = doc,
            methods = methods,
            option = option,
        )

        option.path.contains("AttrOrder") || option.path.endsWith("Attr") -> testAttrProcessMethod(
            doc = doc,
            methods = methods,
            option = option,
        )

        option.path.endsWith("Pattern") -> testRegexProcessMethod(
            doc = doc,
            methods = methods,
            option = option,
        )

        option.path.startsWith("list.pageUrl.") -> testPageUrlProcessMethod(
            source = source,
            inputUrl = url,
            option = option,
        )

        option.path.startsWith("list.pagination.") -> buildString {
            appendLine("该配置会影响完整列表解析。")
            appendLine()
            append(parseListHtml(source = source, html = html))
        }

        option.path.startsWith("detail.pagination.") ||
            option.path.startsWith("detail.request.") -> buildString {
            appendLine("该配置会影响完整详情解析。")
            appendLine()
            append(parseDetailHtml(source = source, url = url, html = html))
        }

        else -> "该处理项是配置值，不需要 selector 测试。\n当前值: ${option.value.ifBlank { "-" }}"
    }

    return LabProcessMethodTestDisplay(
        summary = buildString {
            appendLine("${source.title} 处理项测试")
            appendLine("分组: ${option.group}")
            appendLine("处理项: ${option.remark.ifBlank { option.path }}")
            appendLine("路径: ${option.path}")
            appendLine("配置值: ${option.value.ifBlank { "-" }}")
            appendLine()
            appendLine("请求信息:")
            appendLine("请求链接: $url")
            appendLine("状态: ${requestResult.code} ${requestResult.message}")
            appendLine("连接模式: ${requestResult.connectionMode}")
            appendLine("Body 长度: ${requestResult.bodyLength}")
            appendLine("Content-Type: ${requestResult.contentType.ifBlank { "-" }}")
            appendLine("使用的 Header:")
            appendLine(requestResult.headers.ifBlank { "-" })
            appendLine()
            appendLine("测试结果:")
            append(testResult.ifBlank { "-" })
        },
        body = requestResult.formattedBody(),
    )
}

private fun saveLabProcessMethodOption(
    source: LabSource,
    option: ProcessMethodDraft,
    value: String,
): Result<Unit> {
    return runCatching {
        val methods = getLabProcessMethods(source = source)
            .copyForLabEdit()
            .putLabProcessMethodValue(option = option, value = value)
        if (source.isBuiltIn) {
            SourceProcessMethodStore.saveMethods(sourceType = source.type, methods = methods)
        } else {
            SourceLocalDataStore.saveProcessMethods(targetId = source.key, methods = methods)
        }
    }
}

private fun JSONObject.copyForLabEdit(): JSONObject {
    return JSONObject(toString())
}

private fun JSONObject.putLabProcessMethodValue(
    option: ProcessMethodDraft,
    value: String,
): JSONObject {
    val parsedValue = option.parseLabValue(value = value)
    putLabPathValue(path = option.path, value = parsedValue)
    return this
}

private fun ProcessMethodDraft.parseLabValue(value: String): Any {
    return when (type) {
        ProcessMethodValueType.STRING -> value
        ProcessMethodValueType.NUMBER -> value.trim().toLongOrNull()
            ?: value.trim().toDoubleOrNull()
            ?: error("数字格式错误: $path")

        ProcessMethodValueType.BOOLEAN -> when (value.trim().lowercase()) {
            "true" -> true
            "false" -> false
            else -> error("布尔格式错误: $path，只能填写 true 或 false")
        }
    }
}

private fun JSONObject.putLabPathValue(path: String, value: Any) {
    val tokens = path.toLabPathTokens()
    if (tokens.isEmpty()) {
        return
    }
    putLabPathValue(container = this, tokens = tokens, tokenIndex = 0, value = value)
}

private fun putLabPathValue(
    container: Any,
    tokens: List<LabPathToken>,
    tokenIndex: Int,
    value: Any,
) {
    val token = tokens[tokenIndex]
    val isLast = tokenIndex == tokens.lastIndex
    when (container) {
        is JSONObject -> {
            val key = token as? LabPathToken.Key
                ?: error("路径格式错误")
            if (isLast) {
                container.put(key.name, value)
                return
            }
            val nextToken = tokens[tokenIndex + 1]
            val nextValue = container.opt(key.name)
            val nextContainer = when {
                nextValue is JSONObject -> nextValue
                nextValue is JSONArray -> nextValue
                nextToken is LabPathToken.Index -> JSONArray().also {
                    container.put(key.name, it)
                }

                else -> JSONObject().also {
                    container.put(key.name, it)
                }
            }
            putLabPathValue(
                container = nextContainer,
                tokens = tokens,
                tokenIndex = tokenIndex + 1,
                value = value,
            )
        }

        is JSONArray -> {
            val index = token as? LabPathToken.Index
                ?: error("路径格式错误")
            container.ensureLabSize(index = index.index)
            if (isLast) {
                container.put(index.index, value)
                return
            }
            val nextToken = tokens[tokenIndex + 1]
            val nextValue = container.opt(index.index)
            val nextContainer = when {
                nextValue is JSONObject -> nextValue
                nextValue is JSONArray -> nextValue
                nextToken is LabPathToken.Index -> JSONArray().also {
                    container.put(index.index, it)
                }

                else -> JSONObject().also {
                    container.put(index.index, it)
                }
            }
            putLabPathValue(
                container = nextContainer,
                tokens = tokens,
                tokenIndex = tokenIndex + 1,
                value = value,
            )
        }
    }
}

private fun JSONArray.ensureLabSize(index: Int) {
    while (length() <= index) {
        put(JSONObject.NULL)
    }
}

private sealed interface LabPathToken {
    data class Key(val name: String) : LabPathToken
    data class Index(val index: Int) : LabPathToken
}

private fun String.toLabPathTokens(): List<LabPathToken> {
    val tokens = mutableListOf<LabPathToken>()
    split('.').forEach { segment ->
        val key = segment.substringBefore('[')
        if (key.isNotBlank()) {
            tokens.add(LabPathToken.Key(key))
        }
        Regex("""\[(\d+)]""").findAll(segment).forEach { match ->
            tokens.add(LabPathToken.Index(match.groupValues[1].toInt()))
        }
    }
    return tokens
}

private fun testSelectorProcessMethod(
    doc: Element,
    methods: JSONObject,
    option: ProcessMethodDraft,
): String {
    val selector = option.value.trim()
    if (selector.isBlank()) {
        return "CSS selector 为空"
    }
    val contexts = selectorContextsForPath(doc = doc, methods = methods, path = option.path)
    var totalCount = 0
    val samples = mutableListOf<String>()
    contexts.forEach { context ->
        val result = context.element.selectSafelyForLab(selector)
        result.exceptionOrNull()?.let { throwable ->
            return "CSS selector 解析失败: ${throwable.message ?: throwable.toString()}"
        }
        val nodes = result.getOrNull().orEmpty()
        totalCount += nodes.size
        nodes.forEach { node ->
            if (samples.size < LAB_TEST_SAMPLE_LIMIT) {
                samples.add("[${context.label}] ${node.toLabSummary()}")
            }
        }
    }
    return buildString {
        appendLine("测试 selector: $selector")
        appendLine("上下文数: ${contexts.size}")
        appendLine("命中节点数: $totalCount")
        if (samples.isNotEmpty()) {
            appendLine()
            appendLine("样例:")
            samples.forEachIndexed { index, sample ->
                appendLine("${index + 1}. $sample")
            }
        }
    }
}

private fun testAttrProcessMethod(
    doc: Element,
    methods: JSONObject,
    option: ProcessMethodDraft,
): String {
    val attrSpec = option.value.trim()
    if (attrSpec.isBlank()) {
        return "属性配置为空"
    }
    val targets = attrTargetsForPath(doc = doc, methods = methods, path = option.path)
    val samples = targets.take(LAB_TEST_SAMPLE_LIMIT).map { context ->
        val value = context.element.attrByLabSpec(attrSpec)
        "[${context.label}] ${value.ifBlank { "(空)" }} | ${context.element.toLabSummary()}"
    }
    return buildString {
        appendLine("测试属性: $attrSpec")
        appendLine("候选节点数: ${targets.size}")
        appendLine("非空值数: ${targets.count { it.element.attrByLabSpec(attrSpec).isNotBlank() }}")
        if (samples.isNotEmpty()) {
            appendLine()
            appendLine("样例:")
            samples.forEachIndexed { index, sample ->
                appendLine("${index + 1}. $sample")
            }
        }
    }
}

private fun testRegexProcessMethod(
    doc: Element,
    methods: JSONObject,
    option: ProcessMethodDraft,
): String {
    val pattern = option.value.trim()
    if (pattern.isBlank()) {
        return "正则配置为空"
    }
    val regex = runCatching { Regex(pattern) }.getOrElse { throwable ->
        return "正则解析失败: ${throwable.message ?: throwable.toString()}"
    }
    val sourceText = when (option.path) {
        "list.pagination.currentPagePattern" -> {
            val selector = methods.optMethodString("list.pagination.currentPageSelector")
            doc.selectSafelyForLab(selector).getOrNull()?.firstOrNull()?.text()
        }

        else -> null
    }.orEmpty().ifBlank { doc.text() }
    val match = regex.find(sourceText)
    return buildString {
        appendLine("测试正则: $pattern")
        appendLine("文本: ${sourceText.toLabOneLine(limit = 240).ifBlank { "-" }}")
        appendLine("匹配: ${match?.value ?: "-"}")
        if (match != null && match.groupValues.size > 1) {
            appendLine("分组: ${match.groupValues.drop(1).joinToString(" / ")}")
        }
    }
}

private fun testPageUrlProcessMethod(
    source: LabSource,
    inputUrl: String,
    option: ProcessMethodDraft,
): String {
    val template = option.value.trim()
    if (template.isBlank()) {
        return "URL 规则为空"
    }
    return buildString {
        appendLine("URL 规则: $template")
        appendLine("page=1: ${template.resolveLabPageUrl(source = source, inputUrl = inputUrl, page = 1)}")
        appendLine("page=2: ${template.resolveLabPageUrl(source = source, inputUrl = inputUrl, page = 2)}")
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

        else -> {
            val config = DynamicSourceStore.getConfig(sourceKey = source.key) ?: return "-"
            val result = GenericSourceRepository.transformHome(config = config, data = html, page = 1)
            buildString {
                appendLine("封面数: ${result.images.size}")
                appendLine("下一页: ${result.nextPage ?: "-"}")
                appendImageSamples(result.images)
            }
        }
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

        else -> {
            val config = DynamicSourceStore.getConfig(sourceKey = source.key) ?: return "-"
            val detail = GenericSourceRepository.transformDetail(config = config, url = url, data = html)
            buildString {
                appendLine("标题: ${detail.title.ifBlank { "-" }}")
                appendLine("信息: ${detail.subtitles.joinToString(" / ").ifBlank { "-" }}")
                appendLine("图片数: ${detail.pictures.size}")
                appendLine("下一页详情: ${detail.nextPageUrl.ifBlank { "-" }}")
                appendImageSamples(detail.pictures)
            }
        }
    }
}

private fun getLabProcessMethods(source: LabSource): org.json.JSONObject {
    return if (source.isBuiltIn) {
        SourceProcessMethodStore.getOrCacheMethods(sourceType = source.type)
    } else {
        SourceLocalDataStore.getProcessMethods(targetId = source.key) ?: org.json.JSONObject()
    }
}

private fun selectorContextsForPath(
    doc: Element,
    methods: JSONObject,
    path: String,
): List<LabElementContext> {
    return when {
        path == "list.parse.itemSelector" ||
            path == "list.parse.categorySelector" ||
            path == "detail.parse.contentSelector" ||
            path == "detail.pagination.containerSelector" -> listOf(LabElementContext(label = "document", element = doc))

        path == "list.parse.categoryLinkSelector" ||
            path == "list.parse.childrenCategorySelector" -> contextsBySelector(
            root = doc,
            selector = methods.optMethodString("list.parse.categorySelector"),
            labelPrefix = "category",
        )

        path.startsWith("list.parse.") -> contextsBySelector(
            root = doc,
            selector = methods.optMethodString("list.parse.itemSelector"),
            labelPrefix = "item",
        )

        path == "detail.pagination.linkSelector" -> contextsBySelector(
            root = doc,
            selector = methods.optMethodString("detail.pagination.containerSelector"),
            labelPrefix = "pagination",
        )

        path.startsWith("detail.parse.") -> contextsBySelector(
            root = doc,
            selector = methods.optMethodString("detail.parse.contentSelector"),
            labelPrefix = "content",
        )

        else -> listOf(LabElementContext(label = "document", element = doc))
    }
}

private fun attrTargetsForPath(
    doc: Element,
    methods: JSONObject,
    path: String,
): List<LabElementContext> {
    val listItems by lazy {
        contextsBySelector(
            root = doc,
            selector = methods.optMethodString("list.parse.itemSelector"),
            labelPrefix = "item",
        )
    }
    val detailContents by lazy {
        contextsBySelector(
            root = doc,
            selector = methods.optMethodString("detail.parse.contentSelector"),
            labelPrefix = "content",
        )
    }
    return when {
        path.startsWith("list.parse.coverAttrOrder") -> childContextsBySelector(
            contexts = listItems,
            selector = methods.optMethodString("list.parse.coverSelector"),
            labelPrefix = "cover",
        )

        path.startsWith("list.parse.titleAttrOrder") -> listItems

        path.startsWith("detail.parse.imageAttrOrder") -> childContextsBySelector(
            contexts = detailContents,
            selector = methods.optMethodString("detail.parse.imageSelector"),
            labelPrefix = "image",
        )

        path == "detail.parse.imageAttr" -> childContextsBySelector(
            contexts = detailContents,
            selector = methods.optMethodString("detail.parse.imageLinkSelector"),
            labelPrefix = "imageLink",
        )

        path == "detail.parse.thumbAttr" -> childContextsBySelector(
            contexts = detailContents,
            selector = methods.optMethodString("detail.parse.thumbSelector"),
            labelPrefix = "thumb",
        )

        else -> selectorContextsForPath(doc = doc, methods = methods, path = path)
    }
}

private fun contextsBySelector(
    root: Element,
    selector: String,
    labelPrefix: String,
): List<LabElementContext> {
    val normalizedSelector = selector.trim()
    if (normalizedSelector.isBlank()) {
        return listOf(LabElementContext(label = "document", element = root))
    }
    return root.selectSafelyForLab(normalizedSelector)
        .getOrNull()
        .orEmpty()
        .take(LAB_TEST_SAMPLE_LIMIT)
        .mapIndexed { index, element ->
            LabElementContext(label = "$labelPrefix ${index + 1}", element = element)
        }
        .ifEmpty { listOf(LabElementContext(label = "document", element = root)) }
}

private fun childContextsBySelector(
    contexts: List<LabElementContext>,
    selector: String,
    labelPrefix: String,
): List<LabElementContext> {
    val normalizedSelector = selector.trim()
    if (normalizedSelector.isBlank()) {
        return contexts
    }
    return contexts.flatMap { context ->
        context.element.selectSafelyForLab(normalizedSelector)
            .getOrNull()
            .orEmpty()
            .mapIndexed { index, element ->
                LabElementContext(
                    label = "${context.label} / $labelPrefix ${index + 1}",
                    element = element,
                )
            }
    }.take(LAB_TEST_SAMPLE_LIMIT)
}

private fun Element.selectSafelyForLab(selector: String): Result<List<Element>> {
    val normalizedSelector = selector.trim()
    if (normalizedSelector.isBlank()) {
        return Result.success(emptyList())
    }
    return runCatching { select(normalizedSelector).toList() }
}

private fun Element.attrByLabSpec(attrSpec: String): String {
    val spec = attrSpec.trim()
    if (spec.isBlank()) {
        return ""
    }
    if ("." in spec && !spec.startsWith("data-")) {
        val selector = spec.substringBeforeLast('.').trim()
        val attrName = spec.substringAfterLast('.').trim()
        val target = if (selector.equals(tagName(), ignoreCase = true)) {
            this
        } else {
            selectSafelyForLab(selector).getOrNull()?.firstOrNull()
        } ?: return ""
        return target.readLabAttr(attrName)
    }
    return readLabAttr(spec)
}

private fun Element.readLabAttr(attrName: String): String {
    return when (attrName.trim()) {
        "text" -> text().trim()
        "html" -> html().trim()
        else -> attr(attrName).trim()
    }
}

private fun Element.toLabSummary(): String {
    val name = buildString {
        append("<")
        append(tagName())
        id().takeIf { it.isNotBlank() }?.let { append("#").append(it) }
        className().takeIf { it.isNotBlank() }?.let { classes ->
            append(".")
            append(classes.split(Regex("\\s+")).take(3).joinToString("."))
        }
        append(">")
    }
    val attrs = listOf("href", "src", "data-src", "data-lazy-src", "alt")
        .mapNotNull { attrName ->
            attr(attrName).trim()
                .takeIf { it.isNotBlank() }
                ?.let { "$attrName=${it.toLabOneLine(limit = 90)}" }
        }
        .joinToString(" ")
    val text = text().toLabOneLine(limit = 90)
    return listOf(name, attrs, "text=${text.ifBlank { "-" }}")
        .filter { it.isNotBlank() }
        .joinToString(" | ")
}

private fun JSONObject.optMethodString(path: String): String {
    var current: Any? = this
    path.split('.').forEach { segment ->
        val key = segment.substringBefore('[')
        val arrayIndex = Regex("""\[(\d+)]""").find(segment)?.groupValues?.getOrNull(1)?.toIntOrNull()
        current = when (val value = current) {
            is JSONObject -> value.opt(key)
            is org.json.JSONArray -> arrayIndex?.let(value::opt)
            else -> null
        }
        if (arrayIndex != null && current is org.json.JSONArray) {
            current = (current as org.json.JSONArray).opt(arrayIndex)
        }
    }
    return when (val value = current) {
        null,
        JSONObject.NULL,
        -> ""

        else -> value.toString()
    }
}

private fun String.resolveLabPageUrl(
    source: LabSource,
    inputUrl: String,
    page: Int,
): String {
    val baseUrl = source.defaultUrl.trimEnd('/')
    val categoryUrl = inputUrl.ifBlank { source.defaultUrl }.trimEnd('/')
    return replace("{page}", page.toString())
        .replace("{baseUrl}", baseUrl)
        .replace("{homeUrl}", source.defaultUrl)
        .replace("{categoryUrl}", categoryUrl)
}

private fun String.toLabOneLine(limit: Int): String {
    return trim()
        .replace(Regex("\\s+"), " ")
        .let { value ->
            if (value.length <= limit) {
                value
            } else {
                value.take(limit) + "..."
            }
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
        append(formattedBody())
    }
}

private fun LabRequestResult.formattedBody(): String {
    val body = bodyPreview.ifBlank { return "-" }
    return if (contentType.contains("html", ignoreCase = true) || body.looksLikeHtml()) {
        body.prettyFormatHtmlForLab()
    } else {
        body
    }
}

private fun String.looksLikeHtml(): Boolean {
    val value = trimStart().take(120).lowercase()
    return value.startsWith("<!doctype html") ||
        value.startsWith("<html") ||
        value.contains("<head") ||
        value.contains("<body")
}

private fun String.prettyFormatHtmlForLab(): String {
    return runCatching {
        MJson.parse(this).apply {
            outputSettings()
                .prettyPrint(true)
                .indentAmount(2)
                .maxPaddingWidth(120)
        }.outerHtml()
    }.getOrDefault(this)
}

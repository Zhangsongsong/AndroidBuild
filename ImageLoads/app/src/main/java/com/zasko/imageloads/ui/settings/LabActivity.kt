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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zasko.imageloads.components.HttpHeaderConfigStore
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

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
    val key: String,
    val title: String,
    val defaultUrl: String,
)

private data class LabRequestResult(
    val code: Int,
    val message: String,
    val contentType: String,
    val bodyLength: Int,
    val bodyPreview: String,
    val headers: String,
)

private val labSources = listOf(
    LabSource(
        key = HttpHeaderConfigStore.TARGET_TRENDSZINE,
        title = "Trendszine",
        defaultUrl = "https://trendszine.com/",
    ),
    LabSource(
        key = HttpHeaderConfigStore.TARGET_MEIZI5,
        title = "Meizi5",
        defaultUrl = "https://meizi5.com/",
    ),
    LabSource(
        key = HttpHeaderConfigStore.TARGET_TAOTU,
        title = "TaoTu",
        defaultUrl = "https://taotu.org/",
    ),
)

@Composable
private fun LabScreen(
    onBack: () -> Unit,
    onOpenHttpHeadersSettings: () -> Unit,
    showToast: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedSourceKey by rememberSaveable { mutableStateOf(labSources.first().key) }
    val selectedSource = labSources.firstOrNull { it.key == selectedSourceKey } ?: labSources.first()
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
            Text(
                text = "测试请求会使用当前来源和公共 Header 配置",
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
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !isRequesting,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        val requestUrl = url.trim()
                        if (requestUrl.isBlank()) {
                            showToast("请先输入链接")
                            return@Button
                        }
                        isRequesting = true
                        resultText = ""
                        scope.launch {
                            try {
                                resultText = withContext(Dispatchers.IO) {
                                    testRequest(url = requestUrl).toDisplayText()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (throwable: Throwable) {
                                resultText = "请求失败\n${throwable.message ?: throwable.toString()}"
                            } finally {
                                isRequesting = false
                            }
                        }
                    },
                ) {
                    Text(text = if (isRequesting) "请求中" else "测试访问", maxLines = 1)
                }
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

@Composable
private fun LabSourceMenu(
    selectedSource: LabSource,
    modifier: Modifier = Modifier,
    onSourceSelected: (LabSource) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            onClick = { expanded = true },
        ) {
            Text(text = selectedSource.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(
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
    val client = OkHttpClient.Builder()
        .addInterceptor(HttpHeaderConfigStore.createInterceptor())
        .build()
    val response = client.newCall(
        Request.Builder()
            .url(url)
            .get()
            .build(),
    ).execute()

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
        )
    }
}

private fun LabRequestResult.toDisplayText(): String {
    return buildString {
        appendLine("状态: $code $message")
        appendLine("Content-Type: ${contentType.ifBlank { "-" }}")
        appendLine("Body 长度: $bodyLength")
        appendLine()
        appendLine("使用的 Header:")
        appendLine(headers.ifBlank { "-" })
        appendLine()
        appendLine("Body:")
        append(bodyPreview.ifBlank { "-" })
    }
}

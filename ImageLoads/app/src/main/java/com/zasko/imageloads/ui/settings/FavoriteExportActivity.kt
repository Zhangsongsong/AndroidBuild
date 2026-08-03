package com.zasko.imageloads.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.ui.common.FavoriteBackupManager
import com.zasko.imageloads.ui.common.FavoriteBackupSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoriteExportActivity : BaseComposeActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, FavoriteExportActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadsTheme {
                FavoriteExportScreen(
                    onBack = ::finish,
                    copyToClipboard = ::copyToClipboard,
                    showToast = ::showToast,
                )
            }
        }
    }

    private fun copyToClipboard(source: FavoriteBackupSource, json: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                FavoriteBackupManager.createExportFileName(source = source),
                json,
            ),
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun FavoriteExportScreen(
    onBack: () -> Unit,
    copyToClipboard: (FavoriteBackupSource, String) -> Unit,
    showToast: (String) -> Unit,
) {
    var selectedSourceKey by rememberSaveable {
        mutableStateOf(FavoriteBackupManager.sourceOptions.first().key)
    }
    var jsonText by rememberSaveable { mutableStateOf("") }
    val selectedSource = FavoriteBackupManager.sourceOptions.firstOrNull { it.key == selectedSourceKey }
        ?: FavoriteBackupManager.sourceOptions.first()
    val context = LocalContext.current
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            exportJsonToFile(
                context = context,
                uri = uri,
                json = jsonText,
                showToast = showToast,
            )
        }
    }

    LaunchedEffect(selectedSourceKey) {
        jsonText = withContext(Dispatchers.IO) {
            FavoriteBackupManager.createBackupJson(source = selectedSource)
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ImageLoadsTopBar(
                title = "导出来源数据",
                onBack = onBack,
                actions = {
                    FavoriteSourceMenu(
                        selectedSource = selectedSource,
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
            Text(
                text = "选择来源后导出 JSON，包含 Headers、列表设置和收藏",
                color = colorResource(id = R.color.color_h2),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedTextField(
                value = jsonText,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                readOnly = true,
                label = {
                    Text(text = "${selectedSource.title} 来源 JSON")
                },
                minLines = 12,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        jsonText = FavoriteBackupManager.createBackupJson(source = selectedSource)
                    },
                ) {
                    Text(text = "刷新", maxLines = 1)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        if (jsonText.isBlank()) {
                            showToast("暂无可导出内容")
                        } else {
                            exportFileLauncher.launch(
                                FavoriteBackupManager.createExportFileName(source = selectedSource),
                            )
                        }
                    },
                ) {
                    Text(text = "导出文件", maxLines = 1)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        if (jsonText.isBlank()) {
                            showToast("暂无可复制内容")
                        } else {
                            copyToClipboard(selectedSource, jsonText)
                            showToast("已复制 ${selectedSource.title} 来源 JSON")
                        }
                    },
                ) {
                    Text(text = "复制", maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun exportJsonToFile(
    context: Context,
    uri: Uri,
    json: String,
    showToast: (String) -> Unit,
) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(json.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("open output stream failed")
    }.onSuccess {
        showToast("已导出 JSON 文件")
    }.onFailure {
        showToast("导出失败")
    }
}

@Composable
private fun FavoriteSourceMenu(
    selectedSource: FavoriteBackupSource,
    onSourceSelected: (FavoriteBackupSource) -> Unit,
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
            FavoriteBackupManager.sourceOptions.forEach { source ->
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

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.ui.common.FavoriteBackupManager
import com.zasko.imageloads.ui.common.FavoriteBackupSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoriteExportActivity : BaseComposeActivity() {

    companion object {
        private const val EXTRA_HOME_THEMES = "extra_home_themes"

        fun start(context: Context, themes: List<MainThemeSelectInfo> = emptyList()) {
            context.startActivity(
                Intent(context, FavoriteExportActivity::class.java).apply {
                    putExtra(EXTRA_HOME_THEMES, ArrayList(themes))
                },
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceOptions = FavoriteBackupManager.sourceOptionsFromHomeThemes(
            themes = readHomeThemes(),
            includeAll = false,
        )
        setContent {
            ImageLoadsTheme {
                FavoriteExportScreen(
                    sourceOptions = sourceOptions,
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

    @Suppress("DEPRECATION")
    private fun readHomeThemes(): List<MainThemeSelectInfo> {
        val rawThemes = intent.getSerializableExtra(EXTRA_HOME_THEMES) as? ArrayList<*>
        return rawThemes?.filterIsInstance<MainThemeSelectInfo>().orEmpty()
    }
}

@Composable
private fun FavoriteExportScreen(
    sourceOptions: List<FavoriteBackupSource>,
    onBack: () -> Unit,
    copyToClipboard: (FavoriteBackupSource, String) -> Unit,
    showToast: (String) -> Unit,
) {
    var selectedSourceKey by rememberSaveable {
        mutableStateOf(sourceOptions.firstOrNull()?.key.orEmpty())
    }
    var includeFavorites by rememberSaveable {
        mutableStateOf(false)
    }
    var jsonText by rememberSaveable { mutableStateOf("") }
    val selectedSource = sourceOptions.firstOrNull { it.key == selectedSourceKey }
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

    LaunchedEffect(selectedSourceKey, sourceOptions.size, includeFavorites) {
        jsonText = if (selectedSource == null) {
            ""
        } else {
            withContext(Dispatchers.IO) {
                FavoriteBackupManager.createBackupJson(
                    source = selectedSource,
                    sourceOptions = sourceOptions,
                    includeFavorites = includeFavorites,
                )
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ImageLoadsTopBar(
                title = "导出来源数据",
                onBack = onBack,
                actions = {
                    selectedSource?.let { source ->
                        FavoriteSourceMenu(
                            sourceOptions = sourceOptions,
                            selectedSource = source,
                            onSourceSelected = { selected ->
                                selectedSourceKey = selected.key
                            },
                        )
                    }
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
                text = if (selectedSource == null) {
                    "暂无可导出来源，先导入一个来源 JSON"
                } else {
                    "选择来源后导出 JSON，默认只包含来源定义和列表设置"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "一起导出收藏",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "关闭时不会写入 favorites 字段",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(
                    checked = includeFavorites,
                    enabled = selectedSource != null,
                    onCheckedChange = { includeFavorites = it },
                )
            }
            OutlinedTextField(
                value = jsonText,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                readOnly = true,
                label = {
                    Text(text = "${selectedSource?.title ?: "未选择"} 来源 JSON")
                },
                minLines = 12,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = selectedSource != null,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        selectedSource?.let { source ->
                            jsonText = FavoriteBackupManager.createBackupJson(
                                source = source,
                                sourceOptions = sourceOptions,
                                includeFavorites = includeFavorites,
                            )
                        }
                    },
                ) {
                    Text(text = "刷新", maxLines = 1)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = selectedSource != null,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        if (jsonText.isBlank()) {
                            showToast("暂无可导出内容")
                        } else if (selectedSource != null) {
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
                    enabled = selectedSource != null,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        if (jsonText.isBlank()) {
                            showToast("暂无可复制内容")
                        } else if (selectedSource != null) {
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
    sourceOptions: List<FavoriteBackupSource>,
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
            sourceOptions.forEach { source ->
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

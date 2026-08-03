package com.zasko.imageloads.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.ui.common.FavoriteBackupManager

class FavoriteImportActivity : BaseComposeActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, FavoriteImportActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadsTheme {
                FavoriteImportScreen(
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
private fun FavoriteImportScreen(
    onBack: () -> Unit,
    showToast: (String) -> Unit,
) {
    var jsonText by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            importJsonFromFile(
                context = context,
                uri = uri,
                onJsonLoaded = { jsonText = it },
                showToast = showToast,
            )
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ImageLoadsTopBar(
                title = "导入来源数据",
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
            Text(
                text = "粘贴单个来源 JSON，未知来源会添加到首页",
                color = colorResource(id = R.color.color_h2),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = {
                    Text(text = "来源 JSON")
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
                        jsonText = ""
                    },
                ) {
                    Text(text = "清空", maxLines = 1)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        importFileLauncher.launch(arrayOf("application/json", "text/*"))
                    },
                ) {
                    Text(text = "选文件", maxLines = 1)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    onClick = {
                        importFavorites(
                            rawData = jsonText,
                            showToast = showToast,
                        )
                    },
                ) {
                    Text(text = "导入", maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun importJsonFromFile(
    context: Context,
    uri: Uri,
    onJsonLoaded: (String) -> Unit,
    showToast: (String) -> Unit,
) {
    runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { input ->
            input.readText()
        } ?: throw IllegalStateException("open input stream failed")
    }.onSuccess { json ->
        onJsonLoaded(json)
        importFavorites(
            rawData = json,
            showToast = showToast,
        )
    }.onFailure {
        showToast("读取文件失败")
    }
}

private fun importFavorites(
    rawData: String,
    showToast: (String) -> Unit,
) {
    val json = rawData.trim()
    if (json.isBlank()) {
        showToast("请先填写来源 JSON")
        return
    }
    runCatching {
        FavoriteBackupManager.importBackupJson(rawData = json)
    }.onSuccess { result ->
        if (result.restoredSourceCount == 0) {
            showToast("JSON 中没有可导入来源数据")
        } else {
            showToast(
                "已导入 ${result.restoredSourceCount} 个来源，" +
                    "${result.restoredHeaderGroupCount} 组 Header，" +
                    "${result.restoredSettingsCount} 项设置，" +
                    "${result.restoredItemCount} 条收藏",
            )
        }
    }.onFailure {
        showToast(it.message?.takeIf { message -> message.isNotBlank() } ?: "导入失败")
    }
}

package com.zasko.imageloads.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class FavoriteBulkDownloadDialogState(
    val totalCount: Int = 0,
    val alreadyDownloadedCount: Int = 0,
    val pendingCount: Int = 0,
    val finishedCount: Int = 0,
    val failedCount: Int = 0,
    val currentTitle: String = "",
    val currentImageProgress: String = "",
    val isDownloading: Boolean = false,
)

@Composable
fun FavoriteBulkDownloadDialog(
    state: FavoriteBulkDownloadDialogState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!state.isDownloading) {
                onDismiss()
            }
        },
        title = {
            Text(text = "下载全部")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FavoriteBulkStatLine(label = "收藏总数", value = state.totalCount)
                FavoriteBulkStatLine(label = "已下载", value = state.alreadyDownloadedCount)
                FavoriteBulkStatLine(label = "待下载", value = state.pendingCount)
                if (state.isDownloading || state.finishedCount > 0 || state.failedCount > 0) {
                    DownloadProgressBar(
                        finishedCount = state.finishedCount,
                        totalCount = state.pendingCount,
                    )
                    Text(
                        text = "进度 ${state.finishedCount}/${state.pendingCount}",
                        color = Color(0xFF3C4043),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (state.failedCount > 0) {
                        Text(
                            text = "失败 ${state.failedCount} 个",
                            color = Color(0xFFB3261E),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (state.currentTitle.isNotBlank()) {
                        Text(
                            text = state.currentTitle,
                            color = Color(0xFF5F6368),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (state.currentImageProgress.isNotBlank()) {
                        Text(
                            text = state.currentImageProgress,
                            color = Color(0xFF5F6368),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else if (state.pendingCount == 0) {
                    Text(
                        text = "没有需要下载的收藏",
                        color = Color(0xFF5F6368),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            if (state.isDownloading) {
                TextButton(
                    enabled = false,
                    onClick = {},
                ) {
                    Text(text = "下载中")
                }
            } else if (state.pendingCount > 0) {
                TextButton(onClick = onConfirm) {
                    Text(text = "确认下载")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(text = "确定")
                }
            }
        },
        dismissButton = {
            if (!state.isDownloading && state.pendingCount > 0) {
                TextButton(onClick = onDismiss) {
                    Text(text = "取消")
                }
            }
        },
    )
}

@Composable
private fun FavoriteBulkStatLine(label: String, value: Int) {
    Text(
        text = "$label: $value",
        color = Color(0xFF3C4043),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun DownloadProgressBar(
    finishedCount: Int,
    totalCount: Int,
) {
    val progress = if (totalCount > 0) {
        (finishedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFE8EAED)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(Color(0xFF137333)),
        )
    }
}

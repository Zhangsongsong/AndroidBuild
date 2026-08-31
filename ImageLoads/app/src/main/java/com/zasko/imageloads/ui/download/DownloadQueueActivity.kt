package com.zasko.imageloads.ui.download

import android.content.Context
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.EmptyContent
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.manager.DownloadQueueManager
import com.zasko.imageloads.manager.DownloadTaskUiState

class DownloadQueueActivity : BaseComposeActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, DownloadQueueActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        DownloadQueueManager.bootstrap()
        setContent {
            ImageLoadsTheme {
                DownloadQueueRoute(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun DownloadQueueRoute(onBack: () -> Unit) {
    val tasks by DownloadQueueManager.activeDownloads.collectAsState()
    Scaffold(
        topBar = {
            ImageLoadsTopBar(
                title = "正在下载",
                onBack = onBack,
            )
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            EmptyContent(
                text = "当前没有正在下载的任务",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            itemsIndexed(
                items = tasks,
                key = { _, task -> task.id },
            ) { index, task ->
                DownloadQueueItem(task = task)
                if (index < tasks.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloadQueueItem(task: DownloadTaskUiState) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = task.sourceLabel.ifBlank { "下载" },
                color = colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = task.progressText,
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.End,
            )
        }
        Text(
            text = task.detailTitle.ifBlank { "详情" },
            color = colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        DownloadQueueProgressBar(progress = task.progressFraction)
        Text(
            text = when {
                task.totalCount > 0 -> "${task.finishedCount}/${task.totalCount}"
                else -> task.progressText
            },
            color = colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DownloadQueueProgressBar(progress: Float?) {
    if (progress == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        return
    }
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(6.dp)
                .background(Color(0xFF137333)),
        )
    }
}

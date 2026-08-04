package com.zasko.imageloads.compose

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.data.HasDownloadInfo

@Composable
fun HasDownloadScreen(
    items: List<HasDownloadInfo>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onItemClick: (HasDownloadInfo) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            ImageLoadsTopBar(
                title = "已下载",
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = colorScheme.primary,
                    )
                }

                items.isEmpty() -> EmptyContent(text = "暂无已下载内容")

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items) { info ->
                            HasDownloadItem(
                                info = info,
                                onClick = { onItemClick(info) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HasDownloadItem(
    info: HasDownloadInfo,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = info.name,
                color = colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val previews = info.images.orEmpty().take(3)
                previews.forEach { path ->
                    GlideImage(
                        model = path,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colorScheme.surfaceVariant),
                        scaleType = ImageView.ScaleType.CENTER_CROP,
                    )
                }
                repeat(3 - previews.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(2f / 3f),
                    )
                }
            }
        }
    }
}

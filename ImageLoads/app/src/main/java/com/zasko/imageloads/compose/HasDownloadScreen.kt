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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.data.HasDownloadInfo

@Composable
fun HasDownloadScreen(
    items: List<HasDownloadInfo>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onItemClick: (HasDownloadInfo) -> Unit = {},
    onItemDelete: (HasDownloadInfo) -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedItemKeys: Set<String> = emptySet(),
    itemKeyProvider: (HasDownloadInfo) -> String = { it.path },
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onItemSelectToggle: (HasDownloadInfo) -> Unit = {},
    onDeleteSelected: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val isAllSelected = items.isNotEmpty() && items.all { selectedItemKeys.contains(itemKeyProvider(it)) }
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            ImageLoadsTopBar(
                title = if (isSelectionMode) "已选择 ${selectedItemKeys.size} 项" else "已下载",
                onBack = {
                    if (isSelectionMode) {
                        onClearSelection()
                    } else {
                        onBack()
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(
                            onClick = if (isSelectionMode && isAllSelected) {
                                onClearSelection
                            } else {
                                onSelectAll
                            },
                        ) {
                            Text(text = if (isSelectionMode && isAllSelected) "取消全选" else "全选")
                        }
                    }
                    if (isSelectionMode) {
                        TextButton(
                            enabled = selectedItemKeys.isNotEmpty(),
                            onClick = onDeleteSelected,
                        ) {
                            Text(text = "删除")
                        }
                    }
                },
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
                            val itemKey = itemKeyProvider(info)
                            HasDownloadItem(
                                info = info,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedItemKeys.contains(itemKey),
                                onClick = {
                                    if (isSelectionMode) {
                                        onItemSelectToggle(info)
                                    } else {
                                        onItemClick(info)
                                    }
                                },
                                onSelectToggle = { onItemSelectToggle(info) },
                                onDelete = { onItemDelete(info) },
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
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelectToggle: () -> Unit,
    onDelete: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelectionMode) {
                    DownloadSelectionIndicator(
                        isSelected = isSelected,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable(onClick = onSelectToggle),
                    )
                }
                Text(
                    text = info.name,
                    modifier = Modifier.weight(1f),
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!isSelectionMode) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_delete_24),
                            contentDescription = "删除",
                            tint = colorScheme.error,
                        )
                    }
                }
            }
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

@Composable
private fun DownloadSelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Text(
                text = "✓",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

package com.zasko.imageloads.compose

import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.data.ImageLoadsInfo
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageListScreen(
    title: String,
    images: List<ImageLoadsInfo>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onBack: () -> Unit,
    onOpenWeb: () -> Unit,
    onLoadMore: () -> Unit,
    onImageClick: (ImageLoadsInfo) -> Unit,
    titleContent: (@Composable () -> Unit)? = null,
    topContent: (@Composable () -> Unit)? = null,
    pageLabelProvider: (Int, ImageLoadsInfo) -> String? = { _, _ -> null },
    showWebAction: Boolean = true,
    imageModelProvider: (ImageLoadsInfo) -> Any? = { it.url },
    imageRatioProvider: (ImageLoadsInfo) -> Float = { it.defaultDisplayRatio() },
    imageScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_INSIDE,
    showActionMenu: Boolean = false,
    showDownloadMenuAction: Boolean = true,
    showDownloadAllAction: Boolean = false,
    isDownloadAllActionEnabled: Boolean = true,
    showPageJumpMenuAction: Boolean = false,
    pageJumpInitialPage: Int = 1,
    showFavoriteMenuAction: Boolean = false,
    favoriteMenuText: String = "收藏",
    isSelectionMode: Boolean = false,
    selectedImageKeys: Set<String> = emptySet(),
    isDownloadActionEnabled: Boolean = true,
    selectionDownloadText: String? = null,
    showFavoriteAction: Boolean = false,
    favoriteImageKeys: Set<String> = emptySet(),
    showItemDownloadAction: Boolean = false,
    downloadingImageKeys: Set<String> = emptySet(),
    downloadedImageKeys: Set<String> = emptySet(),
    imageKeyProvider: (ImageLoadsInfo) -> String = { it.url },
    itemDownloadProgressProvider: (ImageLoadsInfo) -> String? = { null },
    onImageDownloadModeClick: () -> Unit = {},
    onDownloadAllClick: () -> Unit = {},
    onPageJump: (Int) -> Unit = {},
    onFavoriteMenuClick: () -> Unit = {},
    onFavoriteClick: (ImageLoadsInfo) -> Unit = {},
    onItemDownloadClick: (ImageLoadsInfo) -> Unit = {},
    onCancelSelection: () -> Unit = {},
    onDownloadSelected: () -> Unit = {},
) {
    val gridState = rememberLazyStaggeredGridState()
    var showPageJumpDialog by remember { mutableStateOf(false) }
    val shouldLoadMore by remember(gridState, images.size) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
            images.size > 5 && lastVisible >= max(0, images.size - 3)
        }
    }

    LaunchedEffect(shouldLoadMore, images.size) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ImageLoadsTopBar(
                title = if (isSelectionMode) "已选择 ${selectedImageKeys.size} 张" else title,
                titleContent = if (isSelectionMode) null else titleContent,
                onBack = {
                    if (isSelectionMode) {
                        onCancelSelection()
                    } else {
                        onBack()
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        val enabled = selectedImageKeys.isNotEmpty() && isDownloadActionEnabled
                        if (selectionDownloadText == null) {
                            IconButton(
                                enabled = enabled,
                                onClick = onDownloadSelected,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_cloud_download_24),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            TextButton(
                                enabled = enabled,
                                onClick = onDownloadSelected,
                            ) {
                                Text(text = selectionDownloadText)
                            }
                        }
                    } else if (showActionMenu) {
                        ImageListActionMenu(
                            onOpenWeb = onOpenWeb,
                            onImageDownloadModeClick = onImageDownloadModeClick,
                            showDownloadMenuAction = showDownloadMenuAction,
                            showPageJumpMenuAction = showPageJumpMenuAction,
                            showFavoriteMenuAction = showFavoriteMenuAction,
                            favoriteMenuText = favoriteMenuText,
                            onPageJumpMenuClick = { showPageJumpDialog = true },
                            onFavoriteMenuClick = onFavoriteMenuClick,
                        )
                    } else if (showDownloadAllAction) {
                        IconButton(
                            enabled = isDownloadAllActionEnabled,
                            onClick = onDownloadAllClick,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_cloud_download_24),
                                contentDescription = null,
                            )
                        }
                    } else if (showWebAction) {
                        IconButton(onClick = onOpenWeb) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_public_24),
                                contentDescription = null,
                            )
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
                .background(Color.White),
        ) {
            if (images.isEmpty() && !isRefreshing && topContent == null) {
                EmptyContent(text = "暂无图片")
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = PaddingValues(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalItemSpacing = 4.dp,
                ) {
                    topContent?.let { content ->
                        item(span = StaggeredGridItemSpan.FullLine) {
                            content()
                        }
                    }
                    if (images.isEmpty() && !isRefreshing) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            EmptyContent(
                                text = "暂无图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                            )
                        }
                    }
                    images.forEachIndexed { index, imageInfo ->
                        pageLabelProvider(index, imageInfo)?.let { pageLabel ->
                            item(span = StaggeredGridItemSpan.FullLine) {
                                ImageListPageLabel(text = pageLabel)
                            }
                        }
                        item {
                            val imageKey = imageKeyProvider(imageInfo)
                            val imageTitle = imageInfo.displayTitle()
                            ImageLoadTile(
                                info = imageInfo,
                                model = imageModelProvider(imageInfo),
                                title = imageTitle,
                                ratio = imageRatioProvider(imageInfo),
                                scaleType = imageScaleType,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedImageKeys.contains(imageKey),
                                showFavoriteAction = showFavoriteAction && !isSelectionMode,
                                isFavorite = favoriteImageKeys.contains(imageKey),
                                showDownloadAction = showItemDownloadAction && !isSelectionMode,
                                isItemDownloading = downloadingImageKeys.contains(imageKey),
                                isItemDownloaded = downloadedImageKeys.contains(imageKey),
                                downloadProgressText = itemDownloadProgressProvider(imageInfo),
                                onClick = onImageClick,
                                onFavoriteClick = onFavoriteClick,
                                onDownloadClick = onItemDownloadClick,
                            )
                        }
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        if (isLoadingMore) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            if (isRefreshing && images.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorResource(id = R.color.teal_700),
                )
            }
        }
    }
    if (showPageJumpDialog) {
        PageJumpDialog(
            initialPage = pageJumpInitialPage,
            onConfirm = { page ->
                showPageJumpDialog = false
                onPageJump(page)
            },
            onDismiss = {
                showPageJumpDialog = false
            },
        )
    }
}

@Composable
private fun ImageListPageLabel(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F3F4))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            color = Color(0xFF5F6368),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ImageListActionMenu(
    onOpenWeb: () -> Unit,
    onImageDownloadModeClick: () -> Unit,
    showDownloadMenuAction: Boolean,
    showPageJumpMenuAction: Boolean,
    showFavoriteMenuAction: Boolean,
    favoriteMenuText: String,
    onPageJumpMenuClick: () -> Unit,
    onFavoriteMenuClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_vert_24),
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = "打开网页") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_public_24),
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onOpenWeb()
                },
            )
            if (showPageJumpMenuAction) {
                DropdownMenuItem(
                    text = { Text(text = "跳转页码") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_refresh_24),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        expanded = false
                        onPageJumpMenuClick()
                    },
                )
            }
            if (showDownloadMenuAction) {
                DropdownMenuItem(
                    text = { Text(text = "图片下载") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_cloud_download_24),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        expanded = false
                        onImageDownloadModeClick()
                    },
                )
            }
            if (showFavoriteMenuAction) {
                DropdownMenuItem(
                    text = { Text(text = favoriteMenuText) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_favorite_24),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        expanded = false
                        onFavoriteMenuClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun PageJumpDialog(
    initialPage: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember(initialPage) {
        mutableStateOf(initialPage.coerceAtLeast(1).toString())
    }
    val page = input.trim().toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "跳转页码")
        },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { value ->
                    input = value.filter { it.isDigit() }.take(6)
                },
                singleLine = true,
                label = {
                    Text(text = "页码")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = input.isNotBlank() && page == null,
            )
        },
        confirmButton = {
            TextButton(
                enabled = page != null && page > 0,
                onClick = {
                    page?.takeIf { it > 0 }?.let(onConfirm)
                },
            ) {
                Text(text = "确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun ImageLoadTile(
    info: ImageLoadsInfo,
    model: Any?,
    title: String,
    ratio: Float,
    scaleType: ImageView.ScaleType,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    showFavoriteAction: Boolean,
    isFavorite: Boolean,
    showDownloadAction: Boolean,
    isItemDownloading: Boolean,
    isItemDownloaded: Boolean,
    downloadProgressText: String?,
    onClick: (ImageLoadsInfo) -> Unit,
    onFavoriteClick: (ImageLoadsInfo) -> Unit,
    onDownloadClick: (ImageLoadsInfo) -> Unit,
) {
    val displayRatio = remember(ratio) { ratio.coerceIn(0.2f, 5f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(displayRatio)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF1F1F1))
            .clickable(enabled = !isItemDownloading) { onClick(info) },
    ) {
        GlideImage(
            model = model,
            modifier = Modifier.fillMaxSize(),
            scaleType = scaleType,
        )
        if (title.isNotBlank()) {
            CoverTitleOverlay(
                title = title,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        if (isSelectionMode) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33000000)),
                )
            }
            SelectionIndicator(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
        if (isItemDownloading) {
            DownloadProgressOverlay(text = downloadProgressText.orEmpty().ifBlank { "下载中" })
        }
        if (showDownloadAction && !isItemDownloading) {
            DownloadIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                isDownloaded = isItemDownloaded,
                onClick = { onDownloadClick(info) },
            )
        }
        if (showFavoriteAction) {
            FavoriteIndicator(
                isFavorite = isFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                onClick = { onFavoriteClick(info) },
            )
        }
    }
}

@Composable
private fun CoverTitleOverlay(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x3D000000))
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DownloadIndicator(
    modifier: Modifier = Modifier,
    isDownloaded: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isDownloaded) Color(0xDDE6F4EA) else Color(0xB3FFFFFF)),
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_cloud_download_24),
            contentDescription = null,
            tint = if (isDownloaded) Color(0xFF137333) else Color(0xFF5F6368),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun DownloadProgressOverlay(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC202124))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun FavoriteIndicator(
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xB3FFFFFF)),
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(
                id = if (isFavorite) {
                    R.drawable.baseline_favorite_24
                } else {
                    R.drawable.baseline_favorite_border_24
                },
            ),
            contentDescription = null,
            tint = if (isFavorite) Color(0xFFE91E63) else Color(0xFF5F6368),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFF1967D2) else Color(0xB3FFFFFF)),
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

private fun ImageLoadsInfo.defaultDisplayRatio(): Float {
    return if (width > 0 && height > 0) {
        (width.toFloat() / height.toFloat()).coerceIn(0.45f, 1.8f)
    } else {
        2f / 3f
    }
}

private fun ImageLoadsInfo.displayTitle(): String {
    return title.trim()
        .ifBlank {
            href.trim()
                .trimEnd('/')
                .substringAfterLast('/')
                .substringBefore('?')
                .replace(Regex("[-_]+"), " ")
                .trim()
        }
}

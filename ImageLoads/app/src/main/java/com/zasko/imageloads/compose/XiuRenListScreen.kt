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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.data.ImageLoadsInfo
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XiuRenListScreen(
    title: String,
    images: List<ImageLoadsInfo>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onBack: () -> Unit,
    onOpenWeb: () -> Unit,
    onLoadMore: () -> Unit,
    onImageClick: (ImageLoadsInfo) -> Unit,
    showWebAction: Boolean = true,
    imageModelProvider: (ImageLoadsInfo) -> Any? = { it.url },
    imageRatioProvider: (ImageLoadsInfo) -> Float = { it.defaultDisplayRatio() },
    imageScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_INSIDE,
    showActionMenu: Boolean = false,
    showDownloadMenuAction: Boolean = true,
    showFavoriteMenuAction: Boolean = false,
    favoriteMenuText: String = "收藏",
    isSelectionMode: Boolean = false,
    selectedImageKeys: Set<String> = emptySet(),
    isDownloadActionEnabled: Boolean = true,
    selectionDownloadText: String? = null,
    showFavoriteAction: Boolean = false,
    favoriteImageKeys: Set<String> = emptySet(),
    imageKeyProvider: (ImageLoadsInfo) -> String = { it.url },
    onImageDownloadModeClick: () -> Unit = {},
    onFavoriteMenuClick: () -> Unit = {},
    onFavoriteClick: (ImageLoadsInfo) -> Unit = {},
    onCancelSelection: () -> Unit = {},
    onDownloadSelected: () -> Unit = {},
) {
    val gridState = rememberLazyStaggeredGridState()
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
                            showFavoriteMenuAction = showFavoriteMenuAction,
                            favoriteMenuText = favoriteMenuText,
                            onFavoriteMenuClick = onFavoriteMenuClick,
                        )
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
            if (images.isEmpty() && !isRefreshing) {
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
                    itemsIndexed(images) { _, imageInfo ->
                        val imageKey = imageKeyProvider(imageInfo)
                        ImageLoadTile(
                            info = imageInfo,
                            model = imageModelProvider(imageInfo),
                            ratio = imageRatioProvider(imageInfo),
                            scaleType = imageScaleType,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedImageKeys.contains(imageKey),
                            showFavoriteAction = showFavoriteAction && !isSelectionMode,
                            isFavorite = favoriteImageKeys.contains(imageKey),
                            onClick = onImageClick,
                            onFavoriteClick = onFavoriteClick,
                        )
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
}

@Composable
private fun ImageListActionMenu(
    onOpenWeb: () -> Unit,
    onImageDownloadModeClick: () -> Unit,
    showDownloadMenuAction: Boolean,
    showFavoriteMenuAction: Boolean,
    favoriteMenuText: String,
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
private fun ImageLoadTile(
    info: ImageLoadsInfo,
    model: Any?,
    ratio: Float,
    scaleType: ImageView.ScaleType,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    showFavoriteAction: Boolean,
    isFavorite: Boolean,
    onClick: (ImageLoadsInfo) -> Unit,
    onFavoriteClick: (ImageLoadsInfo) -> Unit,
) {
    val displayRatio = remember(ratio) { ratio.coerceIn(0.2f, 5f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(displayRatio)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF1F1F1))
            .clickable { onClick(info) },
    ) {
        GlideImage(
            model = model,
            modifier = Modifier.fillMaxSize(),
            scaleType = scaleType,
        )
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
        if (showFavoriteAction) {
            FavoriteIndicator(
                isFavorite = isFavorite,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                onClick = { onFavoriteClick(info) },
            )
        }
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

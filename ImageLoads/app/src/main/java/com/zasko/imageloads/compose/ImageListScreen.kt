package com.zasko.imageloads.compose

import android.widget.ImageView
import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zasko.imageloads.R
import com.zasko.imageloads.data.ImageLoadsInfo
import kotlin.math.roundToInt

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
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var showPageJumpDialog by remember { mutableStateOf(false) }
    val currentImageModelProvider by rememberUpdatedState(imageModelProvider)
    val currentImageRatioProvider by rememberUpdatedState(imageRatioProvider)
    val currentPageLabelProvider by rememberUpdatedState(pageLabelProvider)
    val currentImageKeyProvider by rememberUpdatedState(imageKeyProvider)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val currentOnImageClick by rememberUpdatedState(onImageClick)
    val currentOnFavoriteClick by rememberUpdatedState(onFavoriteClick)
    val currentOnDownloadClick by rememberUpdatedState(onItemDownloadClick)
    val recyclerAdapter = remember {
        ImageListRecyclerAdapter(
            imageModelProvider = { currentImageModelProvider(it) },
            imageRatioProvider = { currentImageRatioProvider(it) },
            imageScaleType = imageScaleType,
            pageLabelProvider = { index, image -> currentPageLabelProvider(index, image) },
            imageKeyProvider = { currentImageKeyProvider(it) },
            onLoadMore = { currentOnLoadMore() },
            onImageClick = { currentOnImageClick(it) },
            onFavoriteClick = { currentOnFavoriteClick(it) },
            onDownloadClick = { currentOnDownloadClick(it) },
        )
    }

    Scaffold(
        containerColor = colorScheme.background,
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
                .background(colorScheme.background),
        ) {
            if (images.isEmpty() && !isRefreshing && topContent == null) {
                EmptyContent(text = "暂无图片")
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        RecyclerView(context).apply {
                            itemAnimator = null
                            clipToPadding = false
                            setPadding(
                                4.dp(context.resources.displayMetrics.density),
                                4.dp(context.resources.displayMetrics.density),
                                4.dp(context.resources.displayMetrics.density),
                                12.dp(context.resources.displayMetrics.density),
                            )
                            layoutManager = StaggeredGridLayoutManager(
                                2,
                                StaggeredGridLayoutManager.VERTICAL,
                            )
                            addItemDecoration(ImageListGridSpacingDecoration(recyclerAdapter, 2.dp(context.resources.displayMetrics.density)))
                            adapter = recyclerAdapter
                        }
                    },
                    update = {
                        recyclerAdapter.updateCallbacks(
                            imageModelProvider = { currentImageModelProvider(it) },
                            imageRatioProvider = { currentImageRatioProvider(it) },
                            imageScaleType = imageScaleType,
                            pageLabelProvider = { index, image -> currentPageLabelProvider(index, image) },
                            imageKeyProvider = { currentImageKeyProvider(it) },
                            onLoadMore = { currentOnLoadMore() },
                            onImageClick = { currentOnImageClick(it) },
                            onFavoriteClick = { currentOnFavoriteClick(it) },
                            onDownloadClick = { currentOnDownloadClick(it) },
                        )
                        recyclerAdapter.submit(
                            images = images,
                            isRefreshing = isRefreshing,
                            isLoadingMore = isLoadingMore,
                            topContent = topContent,
                            selectedImageKeys = selectedImageKeys,
                            isSelectionMode = isSelectionMode,
                            showFavoriteAction = showFavoriteAction,
                            favoriteImageKeys = favoriteImageKeys,
                            showItemDownloadAction = showItemDownloadAction,
                            downloadingImageKeys = downloadingImageKeys,
                            downloadedImageKeys = downloadedImageKeys,
                            itemDownloadProgressProvider = itemDownloadProgressProvider,
                        )
                    },
                )
            }
            if (isRefreshing && images.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorScheme.primary,
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

private fun ImageLoadsInfo.defaultDisplayRatio(): Float {
    return if (width > 0 && height > 0) {
        (width.toFloat() / height.toFloat()).coerceIn(0.45f, 1.8f)
    } else {
        2f / 3f
    }
}

private class ImageListGridSpacingDecoration(
    private val adapter: ImageListRecyclerAdapter,
    private val spacing: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION || adapter.isFullSpan(position)) {
            return
        }
        val layoutParams = view.layoutParams as? StaggeredGridLayoutManager.LayoutParams
        val spanIndex = layoutParams?.spanIndex ?: 0
        outRect.left = if (spanIndex == 0) 0 else spacing / 2
        outRect.right = if (spanIndex == 0) spacing / 2 else 0
        outRect.bottom = spacing
    }
}

private fun Int.dp(density: Float): Int = (this * density).roundToInt()

package com.zasko.imageloads.compose

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    showFavoriteToolsMenu: Boolean = false,
    isFavoriteExportEnabled: Boolean = true,
    isFavoriteImportEnabled: Boolean = true,
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
    onExportFavoritesClick: () -> Unit = {},
    onImportFavoritesClick: () -> Unit = {},
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
                    } else if (showFavoriteToolsMenu) {
                        ImageListFavoriteToolsMenu(
                            showDownloadAllAction = showDownloadAllAction,
                            isDownloadAllActionEnabled = isDownloadAllActionEnabled,
                            isFavoriteExportEnabled = isFavoriteExportEnabled,
                            isFavoriteImportEnabled = isFavoriteImportEnabled,
                            onDownloadAllClick = onDownloadAllClick,
                            onExportFavoritesClick = onExportFavoritesClick,
                            onImportFavoritesClick = onImportFavoritesClick,
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
                            val displayMetrics = context.resources.displayMetrics
                            val spanCount = 2
                            val itemSpacing = 2.dp(displayMetrics.density)
                            itemAnimator = null
                            setHasFixedSize(true)
                            clipToPadding = false
                            setPadding(
                                4.dp(displayMetrics.density),
                                4.dp(displayMetrics.density),
                                4.dp(displayMetrics.density),
                                12.dp(displayMetrics.density),
                            )
                            layoutManager = GridLayoutManager(context, spanCount).apply {
                                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                                    override fun getSpanSize(position: Int): Int {
                                        return if (recyclerAdapter.isFullSpan(position)) spanCount else 1
                                    }
                                }.apply {
                                    setSpanIndexCacheEnabled(true)
                                }
                            }
                            addItemDecoration(ImageListGridSpacingDecoration(recyclerAdapter, itemSpacing))
                            addItemDecoration(
                                StickyPageHeaderDecoration(
                                    adapter = recyclerAdapter,
                                    headerHeight = 38.dp(displayMetrics.density),
                                    chipHorizontalPadding = 12.dp(displayMetrics.density),
                                    chipVerticalInset = 6.dp(displayMetrics.density),
                                    cornerRadius = 8.dp(displayMetrics.density).toFloat(),
                                    textSizePx = 13f * displayMetrics.density * context.resources.configuration.fontScale,
                                    chipColor = colorScheme.secondaryContainer.toArgb(),
                                    textColor = colorScheme.onSecondaryContainer.toArgb(),
                                ),
                            )
                            recyclerAdapter.configureRecyclerView(this)
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
                        (it.layoutManager as? GridLayoutManager)
                            ?.spanSizeLookup
                            ?.invalidateSpanIndexCache()
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
private fun ImageListFavoriteToolsMenu(
    showDownloadAllAction: Boolean,
    isDownloadAllActionEnabled: Boolean,
    isFavoriteExportEnabled: Boolean,
    isFavoriteImportEnabled: Boolean,
    onDownloadAllClick: () -> Unit,
    onExportFavoritesClick: () -> Unit,
    onImportFavoritesClick: () -> Unit,
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
            if (showDownloadAllAction) {
                DropdownMenuItem(
                    text = { Text(text = "批量下载") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_cloud_download_24),
                            contentDescription = null,
                        )
                    },
                    enabled = isDownloadAllActionEnabled,
                    onClick = {
                        expanded = false
                        onDownloadAllClick()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(text = "导出收藏") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_file_download_24),
                        contentDescription = null,
                    )
                },
                enabled = isFavoriteExportEnabled,
                onClick = {
                    expanded = false
                    onExportFavoritesClick()
                },
            )
            DropdownMenuItem(
                text = { Text(text = "导入收藏") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_file_upload_24),
                        contentDescription = null,
                    )
                },
                enabled = isFavoriteImportEnabled,
                onClick = {
                    expanded = false
                    onImportFavoritesClick()
                },
            )
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

private class StickyPageHeaderDecoration(
    private val adapter: ImageListRecyclerAdapter,
    private val headerHeight: Int,
    private val chipHorizontalPadding: Int,
    private val chipVerticalInset: Int,
    private val cornerRadius: Float,
    textSizePx: Float,
    chipColor: Int,
    textColor: Int,
) : RecyclerView.ItemDecoration() {

    private val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = chipColor
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = textSizePx
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val chipRect = RectF()

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val label = adapter.stickyPageLabelForPosition(firstVisiblePosition) ?: return
        val top = parent.paddingTop
        val nextHeaderTop = findNextHeaderTop(parent, firstVisiblePosition)
        val headerTop = if (nextHeaderTop != null && nextHeaderTop < top + headerHeight) {
            nextHeaderTop - headerHeight
        } else {
            top
        }
        drawHeader(canvas, parent, label, headerTop)
    }

    private fun findNextHeaderTop(parent: RecyclerView, afterPosition: Int): Int? {
        var nextTop: Int? = null
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            if (position <= afterPosition || !adapter.isPageLabel(position)) {
                continue
            }
            val childTop = (parent.layoutManager?.getDecoratedTop(child) ?: child.top) +
                child.translationY.roundToInt()
            val currentNextTop = nextTop
            if (currentNextTop == null || childTop < currentNextTop) {
                nextTop = childTop
            }
        }
        return nextTop
    }

    private fun drawHeader(canvas: Canvas, parent: RecyclerView, label: String, top: Int) {
        val bottom = top + headerHeight
        val textWidth = textPaint.measureText(label)
        val chipWidth = textWidth + chipHorizontalPadding * 2
        val chipLeft = (parent.width - chipWidth) / 2f
        val chipTop = top + chipVerticalInset.toFloat()
        val chipBottom = bottom - chipVerticalInset.toFloat()
        chipRect.set(chipLeft, chipTop, chipLeft + chipWidth, chipBottom)
        canvas.drawRoundRect(chipRect, cornerRadius, cornerRadius, chipPaint)

        val textBaseline = chipRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, parent.width / 2f, textBaseline, textPaint)
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
        val layoutParams = view.layoutParams as? GridLayoutManager.LayoutParams
        val spanIndex = layoutParams?.spanIndex ?: 0
        outRect.left = if (spanIndex == 0) 0 else spacing / 2
        outRect.right = if (spanIndex == 0) spacing / 2 else 0
        outRect.bottom = spacing
    }
}

private fun Int.dp(density: Float): Int = (this * density).roundToInt()

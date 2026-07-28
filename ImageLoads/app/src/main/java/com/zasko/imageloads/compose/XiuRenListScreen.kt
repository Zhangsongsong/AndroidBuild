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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
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
                title = title,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onOpenWeb,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_public_24),
                            contentDescription = null,
                        )
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
                        ImageLoadTile(
                            info = imageInfo,
                            onClick = onImageClick,
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
private fun ImageLoadTile(
    info: ImageLoadsInfo,
    onClick: (ImageLoadsInfo) -> Unit,
) {
    val ratio = remember(info.width, info.height) {
        if (info.width > 0 && info.height > 0) {
            (info.width.toFloat() / info.height.toFloat()).coerceIn(0.45f, 1.8f)
        } else {
            2f / 3f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF1F1F1))
            .clickable { onClick(info) },
    ) {
        GlideImage(
            model = info.url,
            modifier = Modifier.fillMaxSize(),
            scaleType = ImageView.ScaleType.CENTER_INSIDE,
        )
    }
}

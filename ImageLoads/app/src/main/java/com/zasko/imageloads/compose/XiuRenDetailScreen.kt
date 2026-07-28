package com.zasko.imageloads.compose

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import kotlin.math.max

@Composable
fun XiuRenDetailScreen(
    coverUrl: String,
    detailInfo: ImageDetailInfo,
    pictures: List<ImageInfo>,
    isInitialLoading: Boolean,
    isLoadingMore: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember(gridState, pictures.size) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
            pictures.size > 2 && lastVisible >= max(0, pictures.size - 1)
        }
    }

    LaunchedEffect(shouldLoadMore, pictures.size) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Scaffold(containerColor = Color.White) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DetailHeader(
                        coverUrl = coverUrl,
                        detailInfo = detailInfo,
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        onBack = onBack,
                        onDownload = onDownload,
                    )
                }
                items(pictures) { imageInfo ->
                    DetailImageTile(info = imageInfo)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (isLoadingMore) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            if (isInitialLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorResource(id = R.color.teal_700),
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(
    coverUrl: String,
    detailInfo: ImageDetailInfo,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onBack: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 10.dp),
    ) {
        IconButton(
            modifier = Modifier
                .width(40.dp)
                .height(40.dp),
            onClick = onBack,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                contentDescription = null,
                tint = colorResource(id = R.color.color_222125),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GlideImage(
                    model = coverUrl,
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1F1F1)),
                    scaleType = ImageView.ScaleType.CENTER_INSIDE,
                )
                IconButton(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDownloaded) colorResource(id = R.color.teal_700) else Color.Transparent)
                        .alpha(if (isDownloading) 0.5f else 1f),
                    enabled = !isDownloading,
                    onClick = onDownload,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_cloud_download_24),
                        contentDescription = null,
                        tint = if (isDownloaded) Color.White else colorResource(id = R.color.color_222125),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detailInfo.name,
                    color = colorResource(id = R.color.color_h1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = detailInfo.time,
                    modifier = Modifier.padding(top = 3.dp),
                    color = colorResource(id = R.color.color_h2),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = detailInfo.desc,
                    modifier = Modifier.padding(top = 3.dp),
                    color = colorResource(id = R.color.color_h3),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DetailImageTile(info: ImageInfo) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(24f / 36f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF1F1F1)),
    ) {
        GlideImage(
            model = info.url,
            modifier = Modifier.fillMaxSize(),
            scaleType = ImageView.ScaleType.CENTER_INSIDE,
            placeholderRes = R.mipmap.icon_pic,
        )
    }
}

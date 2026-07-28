package com.zasko.imageloads.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.zasko.imageloads.data.HasDownloadInfo
import com.zasko.imageloads.data.ImageDetailInfo
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo

private val previewImages = listOf(
    ImageLoadsInfo(url = "preview://image-1", width = 1024, height = 1536),
    ImageLoadsInfo(url = "preview://image-2", width = 900, height = 1280),
    ImageLoadsInfo(url = "preview://image-3", width = 1200, height = 1600),
    ImageLoadsInfo(url = "preview://image-4", width = 960, height = 1440),
    ImageLoadsInfo(url = "preview://image-5", width = 1024, height = 1400),
    ImageLoadsInfo(url = "preview://image-6", width = 960, height = 1300),
)

private val previewDetail = ImageDetailInfo(
    name = "[XiuRen秀人网] 第10261期 写真合集",
    desc = "白色丝袜主题写真，包含多组室内与棚拍样张。这里使用预览数据展示详情头部、下载按钮和图片网格排版。",
    time = "2025.05.09",
    pictures = previewImages.mapIndexed { index, _ ->
        ImageInfo(url = "preview://detail-$index", width = 1024, height = 1536)
    },
)

private val previewDownloads = listOf(
    HasDownloadInfo(
        name = "[XiuRen秀人网] 第10261期 写真合集",
        path = "/preview/xiuren/10261",
        images = listOf("preview://download-1", "preview://download-2", "preview://download-3"),
    ),
    HasDownloadInfo(
        name = "[XiuRen秀人网] 第10260期",
        path = "/preview/xiuren/10260",
        images = listOf("preview://download-4", "preview://download-5"),
    ),
)

@Preview(name = "XiuRen List", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun XiuRenListScreenPreview() {
    ImageLoadsTheme {
        XiuRenListScreen(
            title = "秀人网",
            images = previewImages,
            isRefreshing = false,
            isLoadingMore = true,
            onBack = {},
            onOpenWeb = {},
            onLoadMore = {},
            onImageClick = {},
        )
    }
}

@Preview(name = "XiuRen Detail", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun XiuRenDetailScreenPreview() {
    ImageLoadsTheme {
        XiuRenDetailScreen(
            coverUrl = "preview://cover",
            detailInfo = previewDetail,
            pictures = previewDetail.pictures.orEmpty(),
            isInitialLoading = false,
            isLoadingMore = true,
            isDownloaded = true,
            isDownloading = false,
            onBack = {},
            onDownload = {},
            onLoadMore = {},
        )
    }
}

@Preview(name = "Downloads", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HasDownloadScreenPreview() {
    ImageLoadsTheme {
        HasDownloadScreen(
            items = previewDownloads,
            isLoading = false,
            onBack = {},
        )
    }
}

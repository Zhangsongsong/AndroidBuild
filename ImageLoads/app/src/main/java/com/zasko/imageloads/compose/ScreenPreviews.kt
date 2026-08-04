package com.zasko.imageloads.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.zasko.imageloads.data.HasDownloadInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.common.CommonImageDetailInfo
import com.zasko.imageloads.ui.common.CommonImageDetailScreen

private val previewImages = listOf(
    ImageLoadsInfo(url = "preview://image-1", width = 1024, height = 1536),
    ImageLoadsInfo(url = "preview://image-2", width = 900, height = 1280),
    ImageLoadsInfo(url = "preview://image-3", width = 1200, height = 1600),
    ImageLoadsInfo(url = "preview://image-4", width = 960, height = 1440),
    ImageLoadsInfo(url = "preview://image-5", width = 1024, height = 1400),
    ImageLoadsInfo(url = "preview://image-6", width = 960, height = 1300),
)

private val previewDetail = CommonImageDetailInfo(
    title = "[XiuRen秀人网] 第10261期 写真合集",
    subtitles = listOf(
        "2025.05.09",
        "白色丝袜主题写真，包含多组室内与棚拍样张。",
    ),
    pictures = previewImages,
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
private fun ImageListScreenPreview() {
    ImageLoadsTheme {
        ImageListScreen(
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

@Preview(name = "Common Detail", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun CommonDetailScreenPreview() {
    ImageLoadsTheme {
        CommonImageDetailScreen(
            detailInfo = previewDetail,
            defaultTitle = "详情",
            isLoading = false,
            isLoadingMore = true,
            isDownloading = false,
            isLoadMoreEnabled = true,
            downloadText = "已下载",
            showOverwriteDialog = false,
            errorMessage = "",
            imageModelProvider = { it.url },
            onBack = {},
            onDownload = {},
            onConfirmOverwrite = {},
            onDismissOverwrite = {},
            onLoadMore = {},
            onImageClick = { _, _ -> },
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

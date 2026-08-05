package com.zasko.imageloads.ui.common

import android.content.Context
import android.content.Intent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.meizi5.Meizi5DetailInfo
import com.zasko.imageloads.ui.meizi5.Meizi5FavoriteStore
import com.zasko.imageloads.ui.meizi5.Meizi5Repository
import com.zasko.imageloads.ui.meizi5.toMeizi5ImageModel
import com.zasko.imageloads.ui.taotu.TaoTuDetailInfo
import com.zasko.imageloads.ui.taotu.TaoTuFavoriteStore
import com.zasko.imageloads.ui.taotu.TaoTuRepository
import com.zasko.imageloads.ui.taotu.toTaoTuImageModel
import com.zasko.imageloads.ui.trendszine.TrendszineDetailInfo
import com.zasko.imageloads.ui.trendszine.TrendszineFavoriteStore
import com.zasko.imageloads.ui.trendszine.TrendszineRepository
import com.zasko.imageloads.ui.trendszine.toTrendszineImageModel
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.FileUtil
import kotlinx.coroutines.CancellationException
import java.io.File

object SourceImageDetailDelegate {

    private const val MAX_DETAIL_PAGE_COUNT = 50

    fun logTag(sourceType: Int): String {
        return when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> "Meizi5Detail"
            Constants.THEME_TYPE_TAOTU -> "TaoTuDetail"
            Constants.THEME_TYPE_TRENDSZINE -> "TrendszineDetail"
            else -> "SourceImageDetailActivity"
        }
    }

    fun defaultTitle(sourceType: Int): String {
        return when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> "Meizi5详情"
            Constants.THEME_TYPE_TAOTU -> "TaoTu详情"
            Constants.THEME_TYPE_TRENDSZINE -> "Trendszine详情"
            else -> "详情"
        }
    }

    fun referer(sourceType: Int): String {
        return when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> "https://meizi5.com/"
            Constants.THEME_TYPE_TAOTU -> "https://taotu.org/"
            Constants.THEME_TYPE_TRENDSZINE -> "https://trendszine.com/"
            else -> ""
        }
    }

    suspend fun requestDetail(sourceType: Int, dataUseFrom: Int?, url: String): CommonImageDetailInfo {
        return when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> {
                Meizi5Repository.getDetail(dataUseFrom = dataUseFrom, url = url).toCommonDetailInfo()
            }

            Constants.THEME_TYPE_TAOTU -> {
                TaoTuRepository.getDetail(dataUseFrom = dataUseFrom, url = url).toCommonDetailInfo()
            }

            Constants.THEME_TYPE_TRENDSZINE -> {
                TrendszineRepository.getDetail(dataUseFrom = dataUseFrom, url = url).toCommonDetailInfo()
            }

            else -> CommonImageDetailInfo(url = url)
        }
    }

    suspend fun requestRemainingDetailPages(
        sourceType: Int,
        dataUseFrom: Int?,
        detailInfo: CommonImageDetailInfo,
    ): CommonImageDetailInfo {
        var currentDetail = detailInfo
        val visitedUrls = mutableSetOf<String>().apply {
            currentDetail.url.trim().takeIf { it.isNotBlank() }?.let(::add)
        }
        var nextUrl = currentDetail.nextPageUrl.trim()
        var loadCount = 0
        while (nextUrl.isNotBlank() && loadCount < MAX_DETAIL_PAGE_COUNT && visitedUrls.add(nextUrl)) {
            val nextDetail = try {
                requestDetail(sourceType = sourceType, dataUseFrom = dataUseFrom, url = nextUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                LogComponent.printE(
                    tag = logTag(sourceType = sourceType),
                    message = "request remaining detail page failed:$nextUrl $throwable",
                )
                return currentDetail.copy(nextPageUrl = "")
            }
            currentDetail = currentDetail.mergePage(nextDetail)
            nextUrl = currentDetail.nextPageUrl.trim()
            loadCount += 1
        }
        return currentDetail
    }

    fun imageModel(sourceType: Int, imageInfo: ImageLoadsInfo): Any? {
        return when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> imageInfo.url.toMeizi5ImageModel()
            Constants.THEME_TYPE_TAOTU -> imageInfo.url.toTaoTuImageModel()
            Constants.THEME_TYPE_TRENDSZINE -> imageInfo.url.toTrendszineImageModel()
            else -> imageInfo.url
        }
    }

    fun getDownloadParentDir(sourceType: Int): File {
        return when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> File(
                "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_MEIZI5}/${FileUtil.PICTURE_MEIZI5_DETAIL}",
            )

            Constants.THEME_TYPE_TAOTU -> File(
                "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_TAOTU}/${FileUtil.PICTURE_TAOTU_DETAIL}",
            )

            Constants.THEME_TYPE_TRENDSZINE -> File(
                "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_TRENDSZINE}/${FileUtil.PICTURE_TRENDSZINE_DETAIL}",
            )

            else -> File(FileUtil.getDownloadPath())
        }
    }

    fun isFavoriteEnabled(sourceType: Int): Boolean {
        return sourceType == Constants.THEME_TYPE_MEIZI5 ||
            sourceType == Constants.THEME_TYPE_TAOTU ||
            sourceType == Constants.THEME_TYPE_TRENDSZINE
    }

    fun isImageFavorite(sourceType: Int, imageInfo: ImageLoadsInfo): Boolean {
        val url = imageInfo.url.trim()
        if (url.isBlank()) {
            return false
        }
        return when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> Meizi5FavoriteStore.getFavorites().any { it.url == url }
            Constants.THEME_TYPE_TAOTU -> TaoTuFavoriteStore.getFavorites().any { it.url == url }
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineFavoriteStore.getFavorites().any { it.url == url }
            else -> false
        }
    }

    fun toggleFavorite(sourceType: Int, imageInfo: ImageLoadsInfo): Boolean {
        return when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> Meizi5FavoriteStore.toggleFavorite(imageInfo)
            Constants.THEME_TYPE_TAOTU -> TaoTuFavoriteStore.toggleFavorite(imageInfo)
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineFavoriteStore.toggleFavorite(imageInfo)
            else -> false
        }
    }
}

class SourceImageDetailActivity : CommonImageDetailActivity() {

    companion object {
        fun start(context: Context, sourceType: Int, info: ImageLoadsInfo, dataUseFrom: Int?) {
            context.startActivity(Intent(context, SourceImageDetailActivity::class.java).apply {
                putExtra(CommonImageDetailExtras.KEY_SOURCE_TYPE, sourceType)
                putExtra(CommonImageDetailExtras.KEY_INFO, info)
                if (dataUseFrom != null) {
                    putExtra(CommonImageDetailExtras.KEY_DATA_USE_FROM, dataUseFrom)
                }
            })
        }
    }

    private val sourceType: Int by lazy {
        intent.getIntExtra(CommonImageDetailExtras.KEY_SOURCE_TYPE, -1)
    }

    override val logTag: String
        get() = SourceImageDetailDelegate.logTag(sourceType = sourceType)

    override val defaultTitle: String
        get() = SourceImageDetailDelegate.defaultTitle(sourceType = sourceType)

    override val referer: String
        get() = SourceImageDetailDelegate.referer(sourceType = sourceType)

    override suspend fun requestDetail(dataUseFrom: Int?, url: String): CommonImageDetailInfo {
        return SourceImageDetailDelegate.requestDetail(sourceType = sourceType, dataUseFrom = dataUseFrom, url = url)
    }

    override fun imageModel(imageInfo: ImageLoadsInfo): Any? {
        return SourceImageDetailDelegate.imageModel(sourceType = sourceType, imageInfo = imageInfo)
    }

    override fun getDownloadParentDir(): File {
        return SourceImageDetailDelegate.getDownloadParentDir(sourceType = sourceType)
    }

    override val isFavoriteEnabled: Boolean
        get() = SourceImageDetailDelegate.isFavoriteEnabled(sourceType = sourceType)

    override fun isImageFavorite(imageInfo: ImageLoadsInfo): Boolean {
        return SourceImageDetailDelegate.isImageFavorite(sourceType = sourceType, imageInfo = imageInfo)
    }

    override fun toggleFavorite(imageInfo: ImageLoadsInfo): Boolean {
        return SourceImageDetailDelegate.toggleFavorite(sourceType = sourceType, imageInfo = imageInfo)
    }
}

private fun Meizi5DetailInfo.toCommonDetailInfo(): CommonImageDetailInfo {
    val subtitles = mutableListOf<String>()
    if (date.isNotBlank()) {
        subtitles.add(date)
    }
    if (tags.isNotEmpty()) {
        subtitles.add("标签: ${tags.joinToString(" / ")}")
    }
    return CommonImageDetailInfo(
        url = url,
        title = title,
        subtitles = subtitles,
        pictures = pictures,
    )
}

private fun TaoTuDetailInfo.toCommonDetailInfo(): CommonImageDetailInfo {
    val subtitles = if (tags.isEmpty()) {
        emptyList()
    } else {
        listOf("标签: ${tags.joinToString(" / ")}")
    }
    return CommonImageDetailInfo(
        url = url,
        title = title,
        subtitles = subtitles,
        pictures = pictures,
    )
}

private fun TrendszineDetailInfo.toCommonDetailInfo(): CommonImageDetailInfo {
    val subtitles = mutableListOf<String>()
    if (date.isNotBlank()) {
        subtitles.add(date)
    }
    if (tags.isNotEmpty()) {
        subtitles.add("标签: ${tags.joinToString(" / ")}")
    }
    return CommonImageDetailInfo(
        url = url,
        title = title,
        subtitles = subtitles,
        pictures = pictures,
        nextPageUrl = nextPageUrl,
    )
}

private fun CommonImageDetailInfo.mergePage(pageInfo: CommonImageDetailInfo): CommonImageDetailInfo {
    return copy(
        title = title.ifBlank { pageInfo.title },
        subtitles = if (subtitles.isEmpty()) pageInfo.subtitles else subtitles,
        pictures = (pictures + pageInfo.pictures).distinctBy { it.url },
        nextPageUrl = pageInfo.nextPageUrl,
    )
}

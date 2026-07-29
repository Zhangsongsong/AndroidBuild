package com.zasko.imageloads.ui.common

import android.content.Context
import android.content.Intent
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.meizi5.Meizi5DetailInfo
import com.zasko.imageloads.ui.meizi5.Meizi5Repository
import com.zasko.imageloads.ui.meizi5.toMeizi5ImageModel
import com.zasko.imageloads.ui.taotu.TaoTuDetailInfo
import com.zasko.imageloads.ui.taotu.TaoTuRepository
import com.zasko.imageloads.ui.taotu.toTaoTuImageModel
import com.zasko.imageloads.ui.trendszine.TrendszineDetailInfo
import com.zasko.imageloads.ui.trendszine.TrendszineRepository
import com.zasko.imageloads.ui.trendszine.toTrendszineImageModel
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.FileUtil
import java.io.File

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
        get() = when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> "Meizi5Detail"
            Constants.THEME_TYPE_TAOTU -> "TaoTuDetail"
            Constants.THEME_TYPE_TRENDSZINE -> "TrendszineDetail"
            else -> "SourceImageDetailActivity"
        }

    override val defaultTitle: String
        get() = when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> "Meizi5详情"
            Constants.THEME_TYPE_TAOTU -> "TaoTu详情"
            Constants.THEME_TYPE_TRENDSZINE -> "Trendszine详情"
            else -> "详情"
        }

    override val referer: String
        get() = when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> "https://meizi5.com/"
            Constants.THEME_TYPE_TAOTU -> "https://taotu.org/"
            Constants.THEME_TYPE_TRENDSZINE -> "https://trendszine.com/"
            else -> ""
        }

    override suspend fun requestDetail(dataUseFrom: Int?, url: String): CommonImageDetailInfo {
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

    override fun imageModel(imageInfo: ImageLoadsInfo): Any? {
        return when (sourceType) {
            Constants.THEME_TYPE_MEIZI5 -> imageInfo.url.toMeizi5ImageModel()
            Constants.THEME_TYPE_TAOTU -> imageInfo.url.toTaoTuImageModel()
            Constants.THEME_TYPE_TRENDSZINE -> imageInfo.url.toTrendszineImageModel()
            else -> imageInfo.url
        }
    }

    override fun getDownloadParentDir(): File {
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
    )
}

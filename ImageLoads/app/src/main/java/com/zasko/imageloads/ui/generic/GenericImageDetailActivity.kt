package com.zasko.imageloads.ui.generic

import android.content.Context
import android.content.Intent
import android.os.Build
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.ui.common.CommonImageDetailActivity
import com.zasko.imageloads.ui.common.CommonImageDetailExtras
import com.zasko.imageloads.ui.common.CommonImageDetailInfo
import com.zasko.imageloads.ui.common.DynamicSourceConfig
import com.zasko.imageloads.ui.common.DynamicSourceStore
import com.zasko.imageloads.utils.FileUtil
import java.io.File

class GenericImageDetailActivity : CommonImageDetailActivity() {

    companion object {
        private const val KEY_DATA = "generic_key_data"

        fun start(context: Context, data: MainThemeSelectInfo, info: ImageLoadsInfo) {
            context.startActivity(Intent(context, GenericImageDetailActivity::class.java).apply {
                putExtra(KEY_DATA, data)
                putExtra(CommonImageDetailExtras.KEY_INFO, info)
                putExtra(CommonImageDetailExtras.KEY_DATA_USE_FROM, data.dataUseFrom)
            })
        }
    }

    private val dataInfo: MainThemeSelectInfo by lazy { readThemeInfo() }
    private val sourceConfig: DynamicSourceConfig? by lazy {
        DynamicSourceStore.getConfig(sourceKey = dataInfo.sourceKey)
    }

    override val logTag: String
        get() = "GenericDetail:${dataInfo.sourceKey}"

    override val defaultTitle: String
        get() = "${dataInfo.title}详情"

    override val referer: String
        get() = dataInfo.baseUrl

    override val detailImageColumnCount: Int
        get() = 2

    override suspend fun requestDetail(dataUseFrom: Int?, url: String): CommonImageDetailInfo {
        val config = sourceConfig ?: return CommonImageDetailInfo(url = url)
        return GenericSourceRepository.getDetail(config = config, dataUseFrom = dataUseFrom, url = url)
    }

    override fun imageModel(imageInfo: ImageLoadsInfo): Any? {
        return imageInfo.url.toGenericImageModel()
    }

    override fun getDownloadParentDir(): File {
        return File("${FileUtil.getDownloadPath()}/${dataInfo.sourceKey}/detail")
    }

    override val isFavoriteEnabled: Boolean
        get() = sourceConfig != null

    override fun isImageFavorite(imageInfo: ImageLoadsInfo): Boolean {
        val config = sourceConfig ?: return false
        val url = imageInfo.url.trim()
        return url.isNotBlank() && GenericFavoriteStore.getFavorites(config = config).any { it.url == url }
    }

    override fun toggleFavorite(imageInfo: ImageLoadsInfo): Boolean {
        val config = sourceConfig ?: return false
        return GenericFavoriteStore.toggleFavorite(config = config, imageInfo = imageInfo)
    }

    private fun readThemeInfo(): MainThemeSelectInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(KEY_DATA, MainThemeSelectInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(KEY_DATA) as? MainThemeSelectInfo
        } ?: MainThemeSelectInfo()
    }
}

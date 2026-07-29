package com.zasko.imageloads

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.zasko.imageloads.activity.PersonListActivity
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.compose.HomeScreen
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.ui.TestActivity
import com.zasko.imageloads.ui.common.CommonDownloadedActivity
import com.zasko.imageloads.ui.meizi5.Meizi5Activity
import com.zasko.imageloads.ui.taotu.TaoTuActivity
import com.zasko.imageloads.ui.xiuren.activity.XiuRenActivity
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.FileUtil

class MainActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageLoadsTheme {
                MainRoute()
            }
        }
    }

    @Composable
    private fun MainRoute() {
        var useXiuRenLocalData by rememberSaveable { mutableStateOf(true) }
        var useMeizi5LocalData by rememberSaveable { mutableStateOf(true) }
        var useTaoTuLocalData by rememberSaveable { mutableStateOf(false) }
        val xiuRenTheme = MainThemeSelectInfo(
            cover = XIUREN_COVER,
            title = stringResource(id = R.string.xiuren),
            dataUseFrom = if (useXiuRenLocalData) {
                DataUseFrom.PRIVATE_FILE.value
            } else {
                DataUseFrom.NETWORK.value
            },
            theme = Constants.THEME_TYPE_XIUREN,
        )
        val meizi5Theme = MainThemeSelectInfo(
            cover = MEIZI5_COVER,
            title = "Meizi5",
            dataUseFrom = if (useMeizi5LocalData) {
                DataUseFrom.PRIVATE_FILE.value
            } else {
                DataUseFrom.NETWORK.value
            },
            theme = Constants.THEME_TYPE_MEIZI5,
        )
        val taoTuTheme = MainThemeSelectInfo(
            cover = TAOTU_COVER,
            title = "TaoTu",
            dataUseFrom = if (useTaoTuLocalData) {
                DataUseFrom.PRIVATE_FILE.value
            } else {
                DataUseFrom.NETWORK.value
            },
            theme = Constants.THEME_TYPE_TAOTU,
        )

        HomeScreen(
            themes = listOf(meizi5Theme, taoTuTheme, xiuRenTheme),
            onOpenTheme = { info ->
                openTheme(info = info)
            },
            onOpenRandom = { info ->
                openRandomTheme(info = info)
            },
            onOpenDownloads = { info ->
                openDownloads(info = info)
            },
            onUseLocalChanged = { info, checked ->
                when (info.theme) {
                    Constants.THEME_TYPE_XIUREN -> useXiuRenLocalData = checked
                    Constants.THEME_TYPE_MEIZI5 -> useMeizi5LocalData = checked
                    Constants.THEME_TYPE_TAOTU -> useTaoTuLocalData = checked
                }
            },
        )
    }

    private fun openRandomTheme(info: MainThemeSelectInfo) {
        when (info.theme) {
            Constants.THEME_TYPE_XIUREN -> XiuRenActivity.startRandom(context = this, data = info)
            Constants.THEME_TYPE_MEIZI5 -> Unit
            Constants.THEME_TYPE_TAOTU -> Unit
            else -> PersonListActivity.start(context = this, data = info)
        }
    }

    private fun openTheme(info: MainThemeSelectInfo) {
        when (info.theme) {
            Constants.THEME_TYPE_XIUREN -> XiuRenActivity.start(context = this, data = info)
            Constants.THEME_TYPE_MEIZI5 -> Meizi5Activity.start(context = this, data = info)
            Constants.THEME_TYPE_TAOTU -> TaoTuActivity.start(context = this, data = info)
            else -> PersonListActivity.start(context = this, data = info)
        }
    }

    private fun openDownloads(info: MainThemeSelectInfo) {
        val parentPath = when (info.theme) {
            Constants.THEME_TYPE_XIUREN -> "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_XIUREN}"
            Constants.THEME_TYPE_MEIZI5 -> {
                "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_MEIZI5}/${FileUtil.PICTURE_MEIZI5_DETAIL}"
            }

            Constants.THEME_TYPE_TAOTU -> {
                "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_TAOTU}/${FileUtil.PICTURE_TAOTU_DETAIL}"
            }

            else -> ""
        }
        if (parentPath.isNotBlank()) {
            CommonDownloadedActivity.start(context = this, parentPath = parentPath)
        }
    }

    private companion object {
        const val XIUREN_COVER = "https://i.xiutaku.com/photo/uploadfile/202505/22/9810543470.jpg"
        const val MEIZI5_COVER = "https://meizi5.com/wp-content/uploads/2026/04/VOL_350_face.jpg"
        const val TAOTU_COVER = "https://res.taotu.org/hot-girls/%e5%b0%8f%e8%94%a1%e5%a4%b4%e5%96%b5%e5%96%b5%e5%96%b5/00069-%e9%bb%91%e4%b8%9d%e8%be%85%e5%af%bc%e5%91%98-29p/thumbnail/0020.jpg"
    }
}

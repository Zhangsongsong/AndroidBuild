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
import com.zasko.imageloads.ui.xiuren.XiuRenHasDownloadActivity
import com.zasko.imageloads.ui.xiuren.activity.XiuRenActivity
import com.zasko.imageloads.utils.Constants

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
        var useLocalData by rememberSaveable { mutableStateOf(true) }
        val themeInfo = MainThemeSelectInfo(
            cover = XIUREN_COVER,
            title = stringResource(id = R.string.xiuren),
            dataUseFrom = if (useLocalData) {
                DataUseFrom.PRIVATE_FILE.value
            } else {
                DataUseFrom.NETWORK.value
            },
            theme = Constants.THEME_TYPE_XIUREN,
        )

        HomeScreen(
            themes = listOf(themeInfo),
            onOpenTheme = { info ->
                openTheme(info = info)
            },
            onOpenDownloads = {
                XiuRenHasDownloadActivity.start(context = this@MainActivity)
            },
            onUseLocalChanged = { _, checked ->
                useLocalData = checked
            },
        )
    }

    private fun openTheme(info: MainThemeSelectInfo) {
        when (info.theme) {
            Constants.THEME_TYPE_XIUREN -> XiuRenActivity.start(context = this, data = info)
            else -> PersonListActivity.start(context = this, data = info)
        }
    }

    private companion object {
        const val XIUREN_COVER = "https://i.xiutaku.com/photo/uploadfile/202505/22/9810543470.jpg"
    }
}

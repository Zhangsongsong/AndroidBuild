package com.zasko.imageloads

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.activity.PersonListActivity
import com.zasko.imageloads.base.BaseComposeActivity
import com.zasko.imageloads.components.HttpHeaderConfigStore
import com.zasko.imageloads.components.SourceLocalDataStore
import com.zasko.imageloads.compose.HomeScreen
import com.zasko.imageloads.compose.AppThemeStyleOption
import com.zasko.imageloads.compose.AppThemeStyleStore
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.ui.common.CommonDownloadedActivity
import com.zasko.imageloads.ui.common.DynamicSourceStore
import com.zasko.imageloads.ui.common.SourceListSettingsStore
import com.zasko.imageloads.ui.generic.GenericSourceActivity
import com.zasko.imageloads.ui.meizi5.Meizi5Activity
import com.zasko.imageloads.ui.settings.FavoriteExportActivity
import com.zasko.imageloads.ui.settings.FavoriteImportActivity
import com.zasko.imageloads.ui.settings.FavoritePythonExportActivity
import com.zasko.imageloads.ui.settings.AboutActivity
import com.zasko.imageloads.ui.settings.LabActivity
import com.zasko.imageloads.ui.settings.ManualSourceImportActivity
import com.zasko.imageloads.ui.taotu.TaoTuActivity
import com.zasko.imageloads.ui.trendszine.TrendszineActivity
import com.zasko.imageloads.ui.xiuren.activity.XiuRenActivity
import com.zasko.imageloads.utils.Constants
import com.zasko.imageloads.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : BaseComposeActivity() {

    private var homeRefreshVersion by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveTaskToBack(true)
                }
            },
        )
        setContent {
            ImageLoadsTheme {
                MainRoute()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeRefreshVersion += 1
    }

    @Composable
    private fun MainRoute() {
        var useXiuRenLocalData by rememberSaveable {
            mutableStateOf(SourceListSettingsStore.isLocalDataEnabled(Constants.THEME_TYPE_XIUREN))
        }
        var useMeizi5LocalData by rememberSaveable {
            mutableStateOf(SourceListSettingsStore.isLocalDataEnabled(Constants.THEME_TYPE_MEIZI5))
        }
        var useTaoTuLocalData by rememberSaveable {
            mutableStateOf(SourceListSettingsStore.isLocalDataEnabled(Constants.THEME_TYPE_TAOTU))
        }
        var useTrendszineLocalData by rememberSaveable {
            mutableStateOf(SourceListSettingsStore.isLocalDataEnabled(Constants.THEME_TYPE_TRENDSZINE))
        }
        var useXiuRenCommonHeaders by rememberSaveable {
            mutableStateOf(HttpHeaderConfigStore.isCommonHeadersEnabled(Constants.THEME_TYPE_XIUREN))
        }
        var useMeizi5CommonHeaders by rememberSaveable {
            mutableStateOf(HttpHeaderConfigStore.isCommonHeadersEnabled(Constants.THEME_TYPE_MEIZI5))
        }
        var useTaoTuCommonHeaders by rememberSaveable {
            mutableStateOf(HttpHeaderConfigStore.isCommonHeadersEnabled(Constants.THEME_TYPE_TAOTU))
        }
        var useTrendszineCommonHeaders by rememberSaveable {
            mutableStateOf(HttpHeaderConfigStore.isCommonHeadersEnabled(Constants.THEME_TYPE_TRENDSZINE))
        }
        var showThemeStyleDialog by rememberSaveable {
            mutableStateOf(false)
        }
        var pendingDeleteTheme by remember {
            mutableStateOf<MainThemeSelectInfo?>(null)
        }
        var isDeletingSource by rememberSaveable {
            mutableStateOf(false)
        }
        var deleteProgressText by rememberSaveable {
            mutableStateOf("")
        }
        val xiuRenTheme = MainThemeSelectInfo(
            cover = getHomeCover(sourceType = Constants.THEME_TYPE_XIUREN, fallback = XIUREN_COVER),
            title = stringResource(id = R.string.xiuren),
            dataUseFrom = if (useXiuRenLocalData) {
                DataUseFrom.PRIVATE_FILE.value
            } else {
                DataUseFrom.NETWORK.value
            },
            theme = Constants.THEME_TYPE_XIUREN,
        )
        val meizi5Theme = MainThemeSelectInfo(
            cover = getHomeCover(sourceType = Constants.THEME_TYPE_MEIZI5, fallback = MEIZI5_COVER),
            title = "Meizi5",
            dataUseFrom = if (useMeizi5LocalData) {
                DataUseFrom.PRIVATE_FILE.value
            } else {
                DataUseFrom.NETWORK.value
            },
            theme = Constants.THEME_TYPE_MEIZI5,
        )
        val taoTuTheme = MainThemeSelectInfo(
            cover = getHomeCover(sourceType = Constants.THEME_TYPE_TAOTU, fallback = TAOTU_COVER),
            title = "TaoTu",
            dataUseFrom = if (useTaoTuLocalData) {
                DataUseFrom.PRIVATE_FILE.value
            } else {
                DataUseFrom.NETWORK.value
            },
            theme = Constants.THEME_TYPE_TAOTU,
        )
        val trendszineTheme = MainThemeSelectInfo(
            cover = getHomeCover(sourceType = Constants.THEME_TYPE_TRENDSZINE, fallback = TRENDSZINE_COVER),
            title = "Trendszine",
            dataUseFrom = if (useTrendszineLocalData) {
                DataUseFrom.PRIVATE_FILE.value
            } else {
                DataUseFrom.NETWORK.value
            },
            theme = Constants.THEME_TYPE_TRENDSZINE,
        )
        val refreshVersion = homeRefreshVersion
        val dynamicThemes = remember(refreshVersion) {
            DynamicSourceStore.getDynamicThemes()
        }
        val themes = listOf(trendszineTheme, meizi5Theme, taoTuTheme, xiuRenTheme) + dynamicThemes
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                MainDrawerContent(
                    themes = themes,
                    onHomeClick = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    onThemeClick = { info ->
                        coroutineScope.launch { drawerState.close() }
                        openTheme(info = info)
                    },
                    onLabClick = {
                        LabActivity.start(context = this@MainActivity)
                    },
                    onAboutClick = {
                        AboutActivity.start(context = this@MainActivity)
                    },
                    onThemeStyleClick = {
                        showThemeStyleDialog = true
                    },
                    onExportSourceDataClick = {
                        FavoriteExportActivity.start(context = this@MainActivity)
                    },
                    onExportFavoritePythonClick = {
                        FavoritePythonExportActivity.start(context = this@MainActivity)
                    },
                    onImportSourceDataClick = {
                        FavoriteImportActivity.start(context = this@MainActivity)
                    },
                    onManualImportSourceClick = {
                        ManualSourceImportActivity.start(context = this@MainActivity)
                    },
                )
            },
        ) {
            HomeScreen(
                themes = themes,
                commonHeadersEnabledProvider = { info ->
                    when (info.theme) {
                        Constants.THEME_TYPE_XIUREN -> useXiuRenCommonHeaders
                        Constants.THEME_TYPE_MEIZI5 -> useMeizi5CommonHeaders
                        Constants.THEME_TYPE_TAOTU -> useTaoTuCommonHeaders
                        Constants.THEME_TYPE_TRENDSZINE -> useTrendszineCommonHeaders
                        else -> HttpHeaderConfigStore.isCommonHeadersEnabledForTarget(info.sourceKey)
                    }
                },
                onOpenDrawer = {
                    coroutineScope.launch { drawerState.open() }
                },
                onOpenTheme = { info ->
                    openTheme(info = info)
                },
                onOpenFavorites = { info ->
                    openFavoriteTheme(info = info)
                },
                onOpenDownloads = { info ->
                    openDownloads(info = info)
                },
                onDeleteTheme = { info ->
                    if (!isDeletingSource) {
                        deleteProgressText = ""
                        pendingDeleteTheme = info
                    }
                },
                onUseLocalChanged = { info, checked ->
                    when (info.theme) {
                        Constants.THEME_TYPE_XIUREN -> useXiuRenLocalData = checked
                        Constants.THEME_TYPE_MEIZI5 -> useMeizi5LocalData = checked
                        Constants.THEME_TYPE_TAOTU -> useTaoTuLocalData = checked
                        Constants.THEME_TYPE_TRENDSZINE -> useTrendszineLocalData = checked
                    }
                    if (info.sourceKey.isBlank()) {
                        SourceListSettingsStore.setLocalDataEnabled(
                            sourceType = info.theme,
                            enabled = checked,
                        )
                    } else {
                        SourceListSettingsStore.setLocalDataEnabled(sourceKey = info.sourceKey, enabled = checked)
                        homeRefreshVersion += 1
                    }
                },
                onUseCommonHeadersChanged = { info, checked ->
                    when (info.theme) {
                        Constants.THEME_TYPE_XIUREN -> useXiuRenCommonHeaders = checked
                        Constants.THEME_TYPE_MEIZI5 -> useMeizi5CommonHeaders = checked
                        Constants.THEME_TYPE_TAOTU -> useTaoTuCommonHeaders = checked
                        Constants.THEME_TYPE_TRENDSZINE -> useTrendszineCommonHeaders = checked
                    }
                    if (info.sourceKey.isBlank()) {
                        HttpHeaderConfigStore.setCommonHeadersEnabled(
                            sourceType = info.theme,
                            enabled = checked,
                        )
                    } else {
                        HttpHeaderConfigStore.setCommonHeadersEnabledForTarget(targetId = info.sourceKey, enabled = checked)
                        homeRefreshVersion += 1
                    }
                },
            )
            pendingDeleteTheme?.let { info ->
                DeleteSourceDialog(
                    info = info,
                    isDeleting = isDeletingSource,
                    progressText = deleteProgressText,
                    onConfirm = {
                        if (!isDeletingSource) {
                            isDeletingSource = true
                            deleteProgressText = "准备删除"
                            coroutineScope.launch {
                                val result = runCatching {
                                    deleteSourceItem(
                                        info = info,
                                        onProgress = { message ->
                                            deleteProgressText = message
                                        },
                                    )
                                }
                                isDeletingSource = false
                                pendingDeleteTheme = null
                                deleteProgressText = ""
                                result.onSuccess {
                                    homeRefreshVersion += 1
                                    showToast(message = "已删除 ${info.title}")
                                }.onFailure { throwable ->
                                    showToast(
                                        message = throwable.message
                                            ?.takeIf { it.isNotBlank() }
                                            ?: "删除失败",
                                    )
                                }
                            }
                        }
                    },
                    onDismiss = {
                        if (!isDeletingSource) {
                            pendingDeleteTheme = null
                        }
                    },
                )
            }
            if (showThemeStyleDialog) {
                ThemeStyleDialog(
                    onDismiss = {
                        showThemeStyleDialog = false
                    },
                    onSelect = { option ->
                        AppThemeStyleStore.select(option)
                        showThemeStyleDialog = false
                    },
                )
            }
        }
    }

    @Composable
    private fun MainDrawerContent(
        themes: List<MainThemeSelectInfo>,
        onHomeClick: () -> Unit,
        onThemeClick: (MainThemeSelectInfo) -> Unit,
        onLabClick: () -> Unit,
        onAboutClick: () -> Unit,
        onThemeStyleClick: () -> Unit,
        onExportSourceDataClick: () -> Unit,
        onExportFavoritePythonClick: () -> Unit,
        onImportSourceDataClick: () -> Unit,
        onManualImportSourceClick: () -> Unit,
    ) {
        val drawerWidth = (LocalConfiguration.current.screenWidthDp * 2 / 3).dp
        ModalDrawerSheet(
            modifier = Modifier.width(drawerWidth),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                DrawerSectionTitle(text = "导出数据")
                NavigationDrawerItem(
                    label = { Text(text = "来源数据") },
                    selected = false,
                    onClick = onExportSourceDataClick,
                )
                NavigationDrawerItem(
                    label = { Text(text = "收藏 Python") },
                    selected = false,
                    onClick = onExportFavoritePythonClick,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerSectionTitle(text = "导入数据")
                NavigationDrawerItem(
                    label = { Text(text = "JSON数据") },
                    selected = false,
                    onClick = onImportSourceDataClick,
                )
                NavigationDrawerItem(
                    label = { Text(text = "手动添加") },
                    selected = false,
                    onClick = onManualImportSourceClick,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerSectionTitle(text = "其他")
                NavigationDrawerItem(
                    label = { Text(text = "实验室") },
                    selected = false,
                    onClick = onLabClick,
                )
                NavigationDrawerItem(
                    label = { Text(text = "主题风格") },
                    selected = false,
                    onClick = onThemeStyleClick,
                )
                NavigationDrawerItem(
                    label = { Text(text = "关于") },
                    selected = false,
                    onClick = onAboutClick,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    @Composable
    private fun DrawerSectionTitle(text: String) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }

    @Composable
    private fun DeleteSourceDialog(
        info: MainThemeSelectInfo,
        isDeleting: Boolean,
        progressText: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    onDismiss()
                }
            },
            title = {
                Text(text = "删除来源")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "确认删除「${info.title}」？会同时删除该来源的数据、收藏记录、本地 HTML 缓存和下载目录。")
                    if (isDeleting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = progressText.ifBlank { "正在删除" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = onConfirm,
                ) {
                    Text(
                        text = if (isDeleting) "删除中" else "删除",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = onDismiss,
                ) {
                    Text(text = "取消")
                }
            },
        )
    }

    @Composable
    private fun ThemeStyleDialog(
        onDismiss: () -> Unit,
        onSelect: (AppThemeStyleOption) -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "主题风格")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppThemeStyleStore.options.forEach { option ->
                        ThemeStyleOptionRow(
                            option = option,
                            selected = AppThemeStyleStore.isSelected(option),
                            onClick = {
                                onSelect(option)
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = "关闭")
                }
            },
        )
    }

    @Composable
    private fun ThemeStyleOptionRow(
        option: AppThemeStyleOption,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            color = if (selected) {
                option.primary.copy(alpha = 0.08f)
            } else {
                Color.Transparent
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (selected) option.primary else MaterialTheme.colorScheme.outline,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(option.primary),
                )
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(option.secondary),
                )
                Text(
                    text = option.title,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                )
            }
        }
    }

    private fun openFavoriteTheme(info: MainThemeSelectInfo) {
        when (info.theme) {
            Constants.THEME_TYPE_XIUREN -> XiuRenActivity.startFavorite(context = this, data = info)
            Constants.THEME_TYPE_MEIZI5 -> Meizi5Activity.startFavorite(context = this, data = info)
            Constants.THEME_TYPE_TAOTU -> TaoTuActivity.startFavorite(context = this, data = info)
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineActivity.startFavorite(context = this, data = info)
            else -> GenericSourceActivity.startFavorite(context = this, data = info)
        }
    }

    private fun openTheme(info: MainThemeSelectInfo) {
        when (info.theme) {
            Constants.THEME_TYPE_XIUREN -> XiuRenActivity.start(context = this, data = info)
            Constants.THEME_TYPE_MEIZI5 -> Meizi5Activity.start(context = this, data = info)
            Constants.THEME_TYPE_TAOTU -> TaoTuActivity.start(context = this, data = info)
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineActivity.start(context = this, data = info)
            else -> {
                if (info.sourceKey.isNotBlank()) {
                    GenericSourceActivity.start(context = this, data = info)
                } else {
                    PersonListActivity.start(context = this, data = info)
                }
            }
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

            Constants.THEME_TYPE_TRENDSZINE -> {
                "${FileUtil.getDownloadPath()}/${FileUtil.PICTURE_TRENDSZINE}/${FileUtil.PICTURE_TRENDSZINE_DETAIL}"
            }

            else -> if (info.sourceKey.isNotBlank()) {
                "${FileUtil.getDownloadPath()}/${info.sourceKey}/detail"
            } else {
                ""
            }
        }
        if (parentPath.isNotBlank()) {
            CommonDownloadedActivity.start(context = this, parentPath = parentPath)
        }
    }

    private suspend fun deleteSourceItem(
        info: MainThemeSelectInfo,
        onProgress: (String) -> Unit,
    ) {
        val targetId = info.sourceTargetId()
        if (targetId.isBlank()) {
            return
        }

        onProgress("删除来源数据")
        withContext(Dispatchers.IO) {
            SourceLocalDataStore.removeSourceJson(targetId = targetId)
            HttpHeaderConfigStore.removeTargetConfig(targetId = targetId)
        }
        info.localHtmlDirName().takeIf { it.isNotBlank() }?.let { dirName ->
            onProgress("删除本地 HTML 缓存")
            withContext(Dispatchers.IO) {
                File(FileUtil.getPrivateHtmlDir(), dirName).deleteRecursively()
            }
        }
        info.downloadDirName().takeIf { it.isNotBlank() }?.let { dirName ->
            onProgress("删除下载目录")
            withContext(Dispatchers.IO) {
                File(FileUtil.getDownloadPath(), dirName).deleteRecursively()
            }
        }
        onProgress("刷新首页")
    }

    private fun MainThemeSelectInfo.sourceTargetId(): String {
        return sourceKey.trim().ifBlank {
            HttpHeaderConfigStore.getHeaderTargetId(sourceType = theme)
        }
    }

    private fun MainThemeSelectInfo.localHtmlDirName(): String {
        return sourceKey.trim().ifBlank {
            when (theme) {
                Constants.THEME_TYPE_XIUREN -> FileUtil.NAME_XIUREN
                Constants.THEME_TYPE_MEIZI5 -> FileUtil.NAME_MEIZI5
                Constants.THEME_TYPE_TAOTU -> FileUtil.NAME_TAOTU
                Constants.THEME_TYPE_TRENDSZINE -> FileUtil.NAME_TRENDSZINE
                else -> ""
            }
        }
    }

    private fun MainThemeSelectInfo.downloadDirName(): String {
        return sourceKey.trim().ifBlank {
            when (theme) {
                Constants.THEME_TYPE_XIUREN -> FileUtil.PICTURE_XIUREN
                Constants.THEME_TYPE_MEIZI5 -> FileUtil.PICTURE_MEIZI5
                Constants.THEME_TYPE_TAOTU -> FileUtil.PICTURE_TAOTU
                Constants.THEME_TYPE_TRENDSZINE -> FileUtil.PICTURE_TRENDSZINE
                else -> ""
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getHomeCover(sourceType: Int, fallback: String): String {
        return SourceLocalDataStore.getCover(sourceType = sourceType) ?: fallback
    }

    private companion object {
        const val XIUREN_COVER = "https://i.xiutaku.com/photo/uploadfile/202505/22/9810543470.jpg"
        const val MEIZI5_COVER = "https://meizi5.com/wp-content/uploads/2026/04/VOL_350_face.jpg"
        const val TAOTU_COVER =
            "https://res.taotu.org/hot-girls/%e5%b0%8f%e8%94%a1%e5%a4%b4%e5%96%b5%e5%96%b5%e5%96%b5/00069-%e9%bb%91%e4%b8%9d%e8%be%85%e5%af%bc%e5%91%98-29p/thumbnail/0020.jpg"
        const val TRENDSZINE_COVER = "https://trendszine.com/wp-content/uploads/2026/07/33603291310201.webp.webp"
    }
}

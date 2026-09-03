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
import com.zasko.imageloads.ui.download.DownloadQueueActivity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class MainActivity : BaseComposeActivity() {

    private data class HomeUiState(
        val themes: List<MainThemeSelectInfo> = emptyList(),
        val commonHeadersEnabledByTarget: Map<String, Boolean> = emptyMap(),
        val isLoading: Boolean = true,
    )

    private var homeUiState by mutableStateOf(HomeUiState())
    private var homeLoadJob: Job? = null
    private var homeLoadRequestId = 0
    private var skipNextResumeReload = true

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
        loadHomeData()
    }

    override fun onResume() {
        super.onResume()
        if (skipNextResumeReload) {
            skipNextResumeReload = false
            return
        }
        loadHomeData()
    }

    @Composable
    private fun MainRoute() {
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
        val homeState = homeUiState
        val themes = homeState.themes
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
                    onDownloadQueueClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            DownloadQueueActivity.start(context = this@MainActivity)
                        }
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
                        FavoriteExportActivity.start(context = this@MainActivity, themes = themes)
                    },
                    onExportFavoritePythonClick = {
                        FavoritePythonExportActivity.start(context = this@MainActivity, themes = themes)
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
                isLoading = homeState.isLoading,
                commonHeadersEnabledProvider = { info ->
                    homeState.commonHeadersEnabledByTarget[info.sourceTargetId()] ?: true
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
                    updateHomeLocalData(info = info, enabled = checked)
                },
                onUseCommonHeadersChanged = { info, checked ->
                    updateHomeCommonHeaders(info = info, enabled = checked)
                },
                onThemeOrderChanged = { orderedThemes ->
                    saveHomeThemeOrder(orderedThemes)
                    homeUiState = homeUiState.copy(themes = orderedThemes)
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
                                    loadHomeData()
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
        onDownloadQueueClick: () -> Unit,
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
                DrawerSectionTitle(text = "任务")
                NavigationDrawerItem(
                    label = { Text(text = "正在下载") },
                    selected = false,
                    onClick = onDownloadQueueClick,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
                    Text(text = "确认删除「${info.title}」？会同时删除该来源的数据、收藏记录和本地 HTML 缓存，已下载的图片会保留。")
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
        if (info.sourceKey.isNotBlank()) {
            GenericSourceActivity.startFavorite(context = this, data = info)
            return
        }
        when (info.theme) {
            Constants.THEME_TYPE_XIUREN -> XiuRenActivity.startFavorite(context = this, data = info)
            Constants.THEME_TYPE_MEIZI5 -> Meizi5Activity.startFavorite(context = this, data = info)
            Constants.THEME_TYPE_TAOTU -> TaoTuActivity.startFavorite(context = this, data = info)
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineActivity.startFavorite(context = this, data = info)
            else -> GenericSourceActivity.startFavorite(context = this, data = info)
        }
    }

    private fun openTheme(info: MainThemeSelectInfo) {
        if (info.sourceKey.isNotBlank()) {
            GenericSourceActivity.start(context = this, data = info)
            return
        }
        when (info.theme) {
            Constants.THEME_TYPE_XIUREN -> XiuRenActivity.start(context = this, data = info)
            Constants.THEME_TYPE_MEIZI5 -> Meizi5Activity.start(context = this, data = info)
            Constants.THEME_TYPE_TAOTU -> TaoTuActivity.start(context = this, data = info)
            Constants.THEME_TYPE_TRENDSZINE -> TrendszineActivity.start(context = this, data = info)
            else -> PersonListActivity.start(context = this, data = info)
        }
    }

    private fun openDownloads(info: MainThemeSelectInfo) {
        val parentPath = if (info.sourceKey.isNotBlank()) {
            "${FileUtil.getDownloadPath()}/${info.sourceKey}/detail"
        } else {
            when (info.theme) {
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

                else -> ""
            }
        }
        if (parentPath.isNotBlank()) {
            CommonDownloadedActivity.start(context = this, parentPath = parentPath)
        }
    }

    private fun loadHomeData() {
        homeLoadJob?.cancel()
        val requestId = ++homeLoadRequestId
        homeUiState = homeUiState.copy(isLoading = true)
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    buildHomeUiState()
                }
            }
            if (requestId != homeLoadRequestId) {
                return@launch
            }
            result.onSuccess { state ->
                homeUiState = state.copy(isLoading = false)
            }.onFailure { throwable ->
                homeUiState = homeUiState.copy(isLoading = false)
                showToast(
                    message = throwable.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "首页加载失败",
                )
            }
        }
        homeLoadJob = job
        addJobBindLife(job)
    }

    private fun buildHomeUiState(): HomeUiState {
        val themes = applyHomeThemeOrder(DynamicSourceStore.getDynamicThemes())
        return HomeUiState(
            themes = themes,
            commonHeadersEnabledByTarget = themes.associate { info ->
                info.sourceTargetId() to readCommonHeadersEnabled(info = info)
            },
            isLoading = false,
        )
    }

    private fun updateHomeLocalData(info: MainThemeSelectInfo, enabled: Boolean) {
        val orderKey = info.homeThemeOrderKey()
        val dataUseFrom = if (enabled) {
            DataUseFrom.PRIVATE_FILE.value
        } else {
            DataUseFrom.NETWORK.value
        }
        homeUiState = homeUiState.copy(
            themes = homeUiState.themes.map { theme ->
                if (theme.homeThemeOrderKey() == orderKey) {
                    theme.copy(dataUseFrom = dataUseFrom)
                } else {
                    theme
                }
            },
        )
        val job = CoroutineScope(Dispatchers.IO).launch {
            if (info.sourceKey.isBlank()) {
                SourceListSettingsStore.setLocalDataEnabled(
                    sourceType = info.theme,
                    enabled = enabled,
                )
            } else {
                SourceListSettingsStore.setLocalDataEnabled(sourceKey = info.sourceKey, enabled = enabled)
            }
        }
        addJobBindLife(job)
    }

    private fun updateHomeCommonHeaders(info: MainThemeSelectInfo, enabled: Boolean) {
        val targetId = info.sourceTargetId()
        homeUiState = homeUiState.copy(
            commonHeadersEnabledByTarget = homeUiState.commonHeadersEnabledByTarget + (targetId to enabled),
        )
        val job = CoroutineScope(Dispatchers.IO).launch {
            if (info.sourceKey.isBlank()) {
                HttpHeaderConfigStore.setCommonHeadersEnabled(
                    sourceType = info.theme,
                    enabled = enabled,
                )
            } else {
                HttpHeaderConfigStore.setCommonHeadersEnabledForTarget(targetId = info.sourceKey, enabled = enabled)
            }
        }
        addJobBindLife(job)
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
        onProgress("刷新首页")
    }

    private fun MainThemeSelectInfo.sourceTargetId(): String {
        return sourceKey.trim().ifBlank {
            HttpHeaderConfigStore.getHeaderTargetId(sourceType = theme)
        }
    }

    private fun readCommonHeadersEnabled(info: MainThemeSelectInfo): Boolean {
        return if (info.sourceKey.isBlank()) {
            HttpHeaderConfigStore.isCommonHeadersEnabled(sourceType = info.theme)
        } else {
            HttpHeaderConfigStore.isCommonHeadersEnabledForTarget(targetId = info.sourceKey)
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun applyHomeThemeOrder(themes: List<MainThemeSelectInfo>): List<MainThemeSelectInfo> {
        val order = readHomeThemeOrder()
        if (order.isEmpty()) {
            return themes
        }
        val orderIndex = order.withIndex().associate { it.value to it.index }
        val originalIndex = themes.withIndex().associate { it.value.homeThemeOrderKey() to it.index }
        return themes.sortedWith(
            compareBy<MainThemeSelectInfo> { orderIndex[it.homeThemeOrderKey()] ?: Int.MAX_VALUE }
                .thenBy { originalIndex[it.homeThemeOrderKey()] ?: Int.MAX_VALUE },
        )
    }

    private fun saveHomeThemeOrder(themes: List<MainThemeSelectInfo>) {
        val order = JSONArray()
        themes.map { it.homeThemeOrderKey() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach(order::put)
        getPreferences()
            .edit()
            .putString(KEY_HOME_THEME_ORDER, order.toString())
            .apply()
    }

    private fun readHomeThemeOrder(): List<String> {
        val rawData = getPreferences().getString(KEY_HOME_THEME_ORDER, null).orEmpty()
        if (rawData.isBlank()) {
            return emptyList()
        }
        return runCatching {
            val order = JSONArray(rawData)
            buildList {
                for (index in 0 until order.length()) {
                    order.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }.distinct()
        }.getOrDefault(emptyList())
    }

    private fun MainThemeSelectInfo.homeThemeOrderKey(): String {
        return sourceKey.trim().ifBlank { theme.toString() }
    }

    private fun getPreferences() = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

    private companion object {
        const val PREF_NAME = "main_activity"
        const val KEY_HOME_THEME_ORDER = "home_theme_order"
    }
}

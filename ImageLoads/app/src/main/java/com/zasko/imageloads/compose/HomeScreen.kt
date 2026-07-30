package com.zasko.imageloads.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zasko.imageloads.R
import com.zasko.imageloads.data.DataUseFrom
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.ui.meizi5.toMeizi5ImageModel
import com.zasko.imageloads.ui.taotu.toTaoTuImageModel
import com.zasko.imageloads.ui.trendszine.toTrendszineImageModel
import com.zasko.imageloads.utils.Constants
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage as OfficialGlideImage
import com.bumptech.glide.integration.compose.placeholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    themes: List<MainThemeSelectInfo>,
    commonHeadersEnabledProvider: (MainThemeSelectInfo) -> Boolean = { true },
    onOpenDrawer: () -> Unit = {},
    onOpenTheme: (MainThemeSelectInfo) -> Unit,
    onOpenFavorites: (MainThemeSelectInfo) -> Unit,
    onOpenDownloads: (MainThemeSelectInfo) -> Unit,
    onUseLocalChanged: (MainThemeSelectInfo, Boolean) -> Unit,
    onUseCommonHeadersChanged: (MainThemeSelectInfo, Boolean) -> Unit,
) {
    Scaffold(
        containerColor = Color(0xFFF8FAFD),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_menu_24),
                            contentDescription = "菜单",
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        color = colorResource(id = R.color.color_h1),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF8FAFD),
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(themes) { theme ->
                ThemeSelectCard(
                    info = theme,
                    useCommonHeaders = commonHeadersEnabledProvider(theme),
                    onOpenTheme = onOpenTheme,
                    onOpenFavorites = onOpenFavorites,
                    onOpenDownloads = onOpenDownloads,
                    onUseLocalChanged = onUseLocalChanged,
                    onUseCommonHeadersChanged = onUseCommonHeadersChanged,
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ThemeSelectCard(
    info: MainThemeSelectInfo,
    useCommonHeaders: Boolean,
    onOpenTheme: (MainThemeSelectInfo) -> Unit,
    onOpenFavorites: (MainThemeSelectInfo) -> Unit,
    onOpenDownloads: (MainThemeSelectInfo) -> Unit,
    onUseLocalChanged: (MainThemeSelectInfo, Boolean) -> Unit,
    onUseCommonHeadersChanged: (MainThemeSelectInfo, Boolean) -> Unit,
) {
    val useLocalData = info.dataUseFrom == DataUseFrom.PRIVATE_FILE.value
    val isAvailable = info.isAvailable()
    val outlineColor = Color(0xFFE0E3EB)

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = Color.White,
        ),
        border = BorderStroke(1.dp, outlineColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(104.dp)
                    .height(136.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE8EAED))
                    .clickable { onOpenTheme(info) },
            ) {
                OfficialGlideImage(
                    model = info.coverModel(),
                    contentDescription = info.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = placeholder(R.mipmap.icon_pic),
                    failure = placeholder(R.mipmap.icon_pic),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTheme(info) },
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = info.title,
                            modifier = Modifier.weight(1f),
                            color = colorResource(id = R.color.color_h1),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        ThemeStatusBadge(isAvailable = isAvailable)
                    }
                    Text(
                        text = if (useLocalData) "本地缓存" else "网络内容",
                        color = colorResource(id = R.color.color_h2),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }

                DataSourceSelector(
                    useLocalData = useLocalData,
                    onUseLocalChanged = { checked ->
                        onUseLocalChanged(info, checked)
                    },
                )

                CommonHeaderSelector(
                    useCommonHeaders = useCommonHeaders,
                    onUseCommonHeadersChanged = { checked ->
                        onUseCommonHeadersChanged(info, checked)
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenTheme(info) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFE8F0FE),
                            contentColor = Color(0xFF1967D2),
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Text(text = "打开", maxLines = 1)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_chevron_right_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    OutlinedIconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = { onOpenFavorites(info) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, outlineColor),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_favorite_24),
                            contentDescription = "收藏",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF5F6368),
                        )
                    }
                    OutlinedIconButton(
                        modifier = Modifier.size(40.dp),
                        onClick = { onOpenDownloads(info) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, outlineColor),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_cloud_download_24),
                            contentDescription = stringResource(id = R.string.has_download),
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF5F6368),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeStatusBadge(isAvailable: Boolean) {
    val backgroundColor = if (isAvailable) Color(0xFF188038) else Color(0xFF9AA0A6)
    val text = if (isAvailable) "可用" else "不可用"

    Surface(
        shape = RoundedCornerShape(3.dp),
        color = backgroundColor,
        contentColor = Color.White,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun DataSourceSelector(
    useLocalData: Boolean,
    onUseLocalChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF1F3F4))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        DataSourceOption(
            text = "本地",
            selected = useLocalData,
            modifier = Modifier.weight(1f),
            onClick = { onUseLocalChanged(true) },
        )
        DataSourceOption(
            text = "网络",
            selected = !useLocalData,
            modifier = Modifier.weight(1f),
            onClick = { onUseLocalChanged(false) },
        )
    }
}

@Composable
private fun CommonHeaderSelector(
    useCommonHeaders: Boolean,
    onUseCommonHeadersChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = "公共 Header",
                color = colorResource(id = R.color.color_h1),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (useCommonHeaders) "已启用" else "已关闭",
                color = colorResource(id = R.color.color_h2),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
        Switch(
            checked = useCommonHeaders,
            onCheckedChange = onUseCommonHeadersChanged,
        )
    }
}

@Composable
private fun DataSourceOption(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (selected) Color(0xFF1967D2) else Color(0xFF5F6368),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}

private fun MainThemeSelectInfo.isAvailable(): Boolean {
    return when (theme) {
        Constants.THEME_TYPE_XIUREN -> false
        else -> title.isNotBlank() && cover.isNotBlank()
    }
}

private fun MainThemeSelectInfo.coverModel(): Any {
    return when (theme) {
        Constants.THEME_TYPE_MEIZI5 -> cover.toMeizi5ImageModel()
        Constants.THEME_TYPE_TAOTU -> cover.toTaoTuImageModel()
        Constants.THEME_TYPE_TRENDSZINE -> cover.toTrendszineImageModel()
        else -> cover
    }
}

@Preview(name = "Home", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeScreenPreview() {
    ImageLoadsTheme {
        HomeScreen(
            themes = listOf(
                MainThemeSelectInfo(
                    cover = "preview://cover",
                    title = "秀人网",
                    dataUseFrom = DataUseFrom.PRIVATE_FILE.value,
                    theme = Constants.THEME_TYPE_XIUREN,
                ),
            ),
            commonHeadersEnabledProvider = { true },
            onOpenTheme = {},
            onOpenFavorites = {},
            onOpenDownloads = {},
            onUseLocalChanged = { _, _ -> },
            onUseCommonHeadersChanged = { _, _ -> },
        )
    }
}

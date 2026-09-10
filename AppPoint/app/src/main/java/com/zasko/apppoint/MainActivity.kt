package com.zasko.apppoint

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zasko.apppoint.automation.ActionOutcome
import com.zasko.apppoint.automation.AppProfile
import com.zasko.apppoint.automation.AppProfileStore
import com.zasko.apppoint.automation.AutomationAccessibilityService
import com.zasko.apppoint.automation.AutomationDefaults
import com.zasko.apppoint.automation.AutomationServiceState
import com.zasko.apppoint.automation.InstalledAppInfo
import com.zasko.apppoint.automation.InstalledAppRepository
import com.zasko.apppoint.overlay.OverlayController
import com.zasko.apppoint.ui.theme.AppPointTheme

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppPointTheme {
                AppPointScreen(resumeTick = resumeTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPointScreen(resumeTick: Int) {
    val context = LocalContext.current
    val serviceState = AutomationAccessibilityService.state.value
    var selectedApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var profileRefreshTick by remember { mutableIntStateOf(0) }
    var selectedPackage by remember(resumeTick) {
        mutableStateOf(AppProfileStore.selectedPackage(context))
    }
    val profiles by remember(context, resumeTick, profileRefreshTick) {
        mutableStateOf(AppProfileStore.loadProfiles(context))
    }
    val availableApps by remember(context, resumeTick) {
        mutableStateOf(InstalledAppRepository.loadLaunchableApps(context))
    }
    val permissions = remember(resumeTick) {
        PermissionSnapshot(
            accessibilityEnabled = isAccessibilityEnabled(context),
            overlayEnabled = Settings.canDrawOverlays(context)
        )
    }
    val currentApp = serviceState.currentPackage
        ?.takeUnless { it == context.packageName }
        ?.let { packageName ->
            availableApps.firstOrNull { it.packageName == packageName }
                ?: InstalledAppInfo(
                    appName = serviceState.currentAppName ?: packageName,
                    packageName = packageName
                )
        }

    val app = selectedApp
    if (app == null) {
        HomeScreen(
            profiles = profiles,
            availableApps = availableApps,
            selectedPackage = selectedPackage,
            currentApp = currentApp,
            permissions = permissions,
            serviceState = serviceState,
            onSelectApp = { target ->
                AppProfileStore.select(context, target)
                selectedPackage = target.packageName
                selectedApp = target
            },
            onOpenApp = { target ->
                if (!InstalledAppRepository.launch(context, target)) {
                    Toast.makeText(
                        context,
                        "无法打开 ${target.appName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onConfigureCurrentApp = { target ->
                AppProfileStore.select(context, target)
                selectedPackage = target.packageName
                selectedApp = target
            },
            onOpenAccessibilitySettings = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onOpenOverlaySettings = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            }
        )
    } else {
        ProfileScreen(
            app = app,
            permissions = permissions,
            serviceState = serviceState,
            onBack = {
                selectedApp = null
                profileRefreshTick++
            },
            onOpenApp = {
                if (!InstalledAppRepository.launch(context, app)) {
                    Toast.makeText(
                        context,
                        "无法打开 ${app.appName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    profiles: List<AppProfile>,
    availableApps: List<InstalledAppInfo>,
    selectedPackage: String?,
    currentApp: InstalledAppInfo?,
    permissions: PermissionSnapshot,
    serviceState: AutomationServiceState,
    onSelectApp: (InstalledAppInfo) -> Unit,
    onOpenApp: (InstalledAppInfo) -> Unit,
    onConfigureCurrentApp: (InstalledAppInfo) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    var showAppPicker by remember { mutableStateOf(false) }
    val configuredPackages = remember(profiles) {
        profiles.mapTo(mutableSetOf()) { it.packageName }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("AppPoint") },
                actions = {
                    Text(
                        text = "${profiles.size} 个目标",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                accessibilityEnabled = permissions.accessibilityEnabled,
                overlayEnabled = permissions.overlayEnabled,
                serviceConnected = serviceState.connected,
                targetIsForeground = selectedPackage != null &&
                    serviceState.currentPackage == selectedPackage,
                currentPackage = serviceState.currentPackage,
                currentAppName = serviceState.currentAppName,
                lastActionMessage = serviceState.lastActionMessage
            )

            PermissionCard(
                permissions = permissions,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenOverlaySettings = onOpenOverlaySettings
            )

            if (currentApp != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "当前无障碍应用",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = currentApp.appName,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = currentApp.packageName,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onConfigureCurrentApp(currentApp) }
                        ) {
                            Text("配置当前应用")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "已配置目标应用",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "每个目标应用的配置独立保存到应用名称目录。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = { showAppPicker = !showAppPicker }) {
                    Text(if (showAppPicker) "收起应用" else "添加应用")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (profiles.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "还没有目标应用配置。可以配置当前无障碍应用，或点击“添加应用”。",
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = profiles,
                        key = { "profile_${it.packageName}" }
                    ) { profile ->
                        ProfileListItem(
                            profile = profile,
                            selected = profile.packageName == selectedPackage,
                            installed = availableApps.any {
                                it.packageName == profile.packageName
                            },
                            onSelect = {
                                onSelectApp(
                                    InstalledAppInfo(profile.appName, profile.packageName)
                                )
                            },
                            onOpen = {
                                onOpenApp(
                                    InstalledAppInfo(profile.appName, profile.packageName)
                                )
                            }
                        )
                    }
                }

                if (showAppPicker) {
                    item {
                        Text(
                            text = "选择其他可启动应用",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (availableApps.isEmpty()) {
                        item {
                            Text(
                                text = "没有找到可启动应用",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        items(
                            items = availableApps,
                            key = { "available_${it.packageName}" }
                        ) { app ->
                            AppListItem(
                                app = app,
                                selected = app.packageName == selectedPackage,
                                configured = app.packageName in configuredPackages,
                                onSelect = { onSelectApp(app) },
                                onOpen = { onOpenApp(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileListItem(
    profile: AppProfile,
    selected: Boolean,
    installed: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.appName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = profile.packageName,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (selected) "当前选择" else "已配置",
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "目录：app_profiles/${
                        AppProfileStore.profileDirectoryName(
                            context,
                            InstalledAppInfo(profile.appName, profile.packageName)
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            TextButton(
                enabled = installed,
                onClick = onOpen
            ) {
                Text("打开")
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedButton(onClick = onSelect) {
                Text("配置")
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: InstalledAppInfo,
    selected: Boolean,
    configured: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = when {
                        selected -> "当前选择"
                        configured -> "已有配置"
                        else -> "未配置"
                    },
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }
            TextButton(onClick = onOpen) {
                Text("打开")
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedButton(onClick = onSelect) {
                Text("配置")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(
    app: InstalledAppInfo,
    permissions: PermissionSnapshot,
    serviceState: AutomationServiceState,
    onBack: () -> Unit,
    onOpenApp: () -> Unit
) {
    val context = LocalContext.current
    val initialProfile = remember(app.packageName) {
        AppProfileStore.load(context, app)
    }
    var tapX by remember(app.packageName) { mutableStateOf(initialProfile.tapX.toString()) }
    var tapY by remember(app.packageName) { mutableStateOf(initialProfile.tapY.toString()) }
    var swipeStartX by remember(app.packageName) {
        mutableStateOf(initialProfile.swipeStartX.toString())
    }
    var swipeStartY by remember(app.packageName) {
        mutableStateOf(initialProfile.swipeStartY.toString())
    }
    var swipeEndX by remember(app.packageName) {
        mutableStateOf(initialProfile.swipeEndX.toString())
    }
    var swipeEndY by remember(app.packageName) {
        mutableStateOf(initialProfile.swipeEndY.toString())
    }
    var swipeDuration by remember(app.packageName) {
        mutableStateOf(initialProfile.swipeDurationMs.toString())
    }
    val targetIsForeground = serviceState.currentPackage == app.packageName

    fun readProfile(): AppProfile? {
        val profile = AppProfile(
            appName = app.appName,
            packageName = app.packageName,
            tapX = tapX.toIntOrNull() ?: -1,
            tapY = tapY.toIntOrNull() ?: -1,
            swipeStartX = swipeStartX.toIntOrNull() ?: -1,
            swipeStartY = swipeStartY.toIntOrNull() ?: -1,
            swipeEndX = swipeEndX.toIntOrNull() ?: -1,
            swipeEndY = swipeEndY.toIntOrNull() ?: -1,
            swipeDurationMs = swipeDuration.toLongOrNull() ?: -1L
        )
        val invalid = profile.tapX < 0 ||
            profile.tapY < 0 ||
            profile.swipeStartX < 0 ||
            profile.swipeStartY < 0 ||
            profile.swipeEndX < 0 ||
            profile.swipeEndY < 0 ||
            profile.swipeDurationMs !in 50L..5_000L
        if (invalid) {
            Toast.makeText(
                context,
                "请检查坐标和滑动时长（50~5000毫秒）",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }
        return profile
    }

    fun saveProfile(): AppProfile? {
        val profile = readProfile() ?: return null
        AppProfileStore.save(context, profile)
        AppProfileStore.select(context, app)
        Toast.makeText(context, "已保存 ${app.appName} 的配置", Toast.LENGTH_SHORT).show()
        return profile
    }

    fun showOutcome(outcome: ActionOutcome) {
        Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(app.appName) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenApp) {
                        Text("打开")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                accessibilityEnabled = permissions.accessibilityEnabled,
                overlayEnabled = permissions.overlayEnabled,
                serviceConnected = serviceState.connected,
                targetIsForeground = targetIsForeground,
                currentPackage = serviceState.currentPackage,
                currentAppName = serviceState.currentAppName,
                lastActionMessage = serviceState.lastActionMessage
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("应用信息", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "配置目录：app_profiles/${
                            AppProfileStore.profileDirectoryName(context, app)
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("点击坐标", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(
                            value = tapX,
                            label = "X",
                            modifier = Modifier.weight(1f),
                            onValueChange = { tapX = it }
                        )
                        NumberField(
                            value = tapY,
                            label = "Y",
                            modifier = Modifier.weight(1f),
                            onValueChange = { tapY = it }
                        )
                    }
                    Text(
                        text = "坐标使用手机屏幕像素，默认点击 (${AutomationDefaults.tapX}, ${AutomationDefaults.tapY})。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("滑动坐标", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(
                            value = swipeStartX,
                            label = "起点 X",
                            modifier = Modifier.weight(1f),
                            onValueChange = { swipeStartX = it }
                        )
                        NumberField(
                            value = swipeStartY,
                            label = "起点 Y",
                            modifier = Modifier.weight(1f),
                            onValueChange = { swipeStartY = it }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(
                            value = swipeEndX,
                            label = "终点 X",
                            modifier = Modifier.weight(1f),
                            onValueChange = { swipeEndX = it }
                        )
                        NumberField(
                            value = swipeEndY,
                            label = "终点 Y",
                            modifier = Modifier.weight(1f),
                            onValueChange = { swipeEndY = it }
                        )
                    }
                    NumberField(
                        value = swipeDuration,
                        label = "时长（毫秒）",
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { swipeDuration = it }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { saveProfile() }
                ) {
                    Text("保存配置")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = permissions.overlayEnabled,
                    onClick = { OverlayController.show(context) }
                ) {
                    Text("显示悬浮窗")
                }
            }

            HorizontalDivider()
            Text("手动测试", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = serviceState.connected,
                    onClick = {
                        val profile = saveProfile()
                        if (profile != null) {
                            showOutcome(
                                AutomationAccessibilityService.dispatchTap(
                                    profile.packageName,
                                    profile.tapX,
                                    profile.tapY
                                )
                            )
                        }
                    }
                ) {
                    Text("执行点击")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = serviceState.connected,
                    onClick = {
                        val profile = saveProfile()
                        if (profile != null) {
                            showOutcome(
                                AutomationAccessibilityService.dispatchSwipe(
                                    profile.packageName,
                                    profile.swipeStartX,
                                    profile.swipeStartY,
                                    profile.swipeEndX,
                                    profile.swipeEndY,
                                    profile.swipeDurationMs
                                )
                            )
                        }
                    }
                ) {
                    Text("执行滑动")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    permissions: PermissionSnapshot,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("权限与悬浮窗", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "首次使用需要在系统设置中开启无障碍服务和悬浮窗权限。",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAccessibilitySettings
                ) {
                    Text(if (permissions.accessibilityEnabled) "无障碍已开启" else "开启无障碍")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenOverlaySettings
                ) {
                    Text(if (permissions.overlayEnabled) "悬浮窗已开启" else "开启悬浮窗")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    accessibilityEnabled: Boolean,
    overlayEnabled: Boolean,
    serviceConnected: Boolean,
    targetIsForeground: Boolean,
    currentPackage: String?,
    currentAppName: String?,
    lastActionMessage: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("运行状态", style = MaterialTheme.typography.titleMedium)
            StatusLine("无障碍权限", accessibilityEnabled)
            StatusLine("服务连接", serviceConnected)
            StatusLine("悬浮窗权限", overlayEnabled)
            StatusLine("当前应用前台", targetIsForeground)
            Text(
                text = "当前应用：${currentAppName ?: "未知"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "当前包名：${currentPackage ?: "未知"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "最近操作：$lastActionMessage",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StatusLine(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Text(
            text = if (enabled) "已就绪" else "未就绪",
            color = if (enabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    label: String,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            if (text.all(Char::isDigit)) onValueChange(text)
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

private data class PermissionSnapshot(
    val accessibilityEnabled: Boolean,
    val overlayEnabled: Boolean
)

private fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(
        context,
        AutomationAccessibilityService::class.java
    ).flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
}

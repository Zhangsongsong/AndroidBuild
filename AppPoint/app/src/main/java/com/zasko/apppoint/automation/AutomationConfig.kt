package com.zasko.apppoint.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.io.File
import java.util.Properties

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

data class AppProfile(
    val appName: String,
    val packageName: String,
    val tapX: Int = AutomationDefaults.tapX,
    val tapY: Int = AutomationDefaults.tapY,
    val swipeStartX: Int = AutomationDefaults.swipeStartX,
    val swipeStartY: Int = AutomationDefaults.swipeStartY,
    val swipeEndX: Int = AutomationDefaults.swipeEndX,
    val swipeEndY: Int = AutomationDefaults.swipeEndY,
    val swipeDurationMs: Long = AutomationDefaults.swipeDurationMs
)

object AutomationDefaults {
    const val tapX = 540
    const val tapY = 960
    const val swipeStartX = 540
    const val swipeStartY = 1400
    const val swipeEndX = 540
    const val swipeEndY = 600
    const val swipeDurationMs = 600L
}

object InstalledAppRepository {
    fun loadLaunchableApps(context: Context): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.MATCH_ALL
        )
            .mapNotNull { resolveInfo ->
                val applicationInfo =
                    resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
                val packageName = applicationInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                InstalledAppInfo(
                    appName = applicationInfo.loadLabel(packageManager)
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                        .ifBlank { packageName },
                    packageName = packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedWith { left, right ->
                val nameComparison = left.appName.compareTo(
                    other = right.appName,
                    ignoreCase = true
                )
                if (nameComparison != 0) {
                    nameComparison
                } else {
                    left.packageName.compareTo(right.packageName)
                }
            }
    }

    fun launch(context: Context, app: InstalledAppInfo): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            ?: return false
        context.startActivity(launchIntent)
        return true
    }
}

object AppProfileStore {
    private const val rootDirectoryName = "app_profiles"
    private const val configFileName = "config.properties"
    private const val sessionPreferencesName = "app_point_session"
    private const val selectedPackageKey = "selected_package"

    private const val appNameKey = "app_name"
    private const val packageNameKey = "package_name"
    private const val tapXKey = "tap_x"
    private const val tapYKey = "tap_y"
    private const val swipeStartXKey = "swipe_start_x"
    private const val swipeStartYKey = "swipe_start_y"
    private const val swipeEndXKey = "swipe_end_x"
    private const val swipeEndYKey = "swipe_end_y"
    private const val swipeDurationKey = "swipe_duration"

    fun load(context: Context, app: InstalledAppInfo): AppProfile {
        val directory = profileDirectory(context, app)
        val stored = read(File(directory, configFileName))
        return AppProfile(
            appName = app.appName,
            packageName = app.packageName,
            tapX = stored?.getInt(tapXKey) ?: AutomationDefaults.tapX,
            tapY = stored?.getInt(tapYKey) ?: AutomationDefaults.tapY,
            swipeStartX = stored?.getInt(swipeStartXKey) ?: AutomationDefaults.swipeStartX,
            swipeStartY = stored?.getInt(swipeStartYKey) ?: AutomationDefaults.swipeStartY,
            swipeEndX = stored?.getInt(swipeEndXKey) ?: AutomationDefaults.swipeEndX,
            swipeEndY = stored?.getInt(swipeEndYKey) ?: AutomationDefaults.swipeEndY,
            swipeDurationMs = stored?.getLong(swipeDurationKey)
                ?: AutomationDefaults.swipeDurationMs
        )
    }

    fun loadByPackage(context: Context, packageName: String): AppProfile? {
        val directories = profilesRoot(context)
            .listFiles { file -> file.isDirectory }
            ?: return null
        return directories.asSequence()
            .mapNotNull { directory -> read(File(directory, configFileName)) }
            .firstOrNull { it.getString(packageNameKey) == packageName }
            ?.toProfile()
    }

    fun loadProfiles(context: Context): List<AppProfile> {
        return profilesRoot(context)
            .listFiles { file -> file.isDirectory }
            ?.asSequence()
            ?.mapNotNull { directory ->
                read(File(directory, configFileName))?.toProfile()
            }
            ?.sortedWith { left, right ->
                val nameComparison = left.appName.compareTo(
                    other = right.appName,
                    ignoreCase = true
                )
                if (nameComparison != 0) {
                    nameComparison
                } else {
                    left.packageName.compareTo(right.packageName)
                }
            }
            ?.toList()
            ?: emptyList()
    }

    fun save(context: Context, profile: AppProfile) {
        val directory = profileDirectory(
            context,
            InstalledAppInfo(profile.appName, profile.packageName)
        )
        check(directory.mkdirs() || directory.isDirectory) {
            "Unable to create profile directory: ${directory.absolutePath}"
        }
        val properties = Properties().apply {
            setProperty(appNameKey, profile.appName)
            setProperty(packageNameKey, profile.packageName)
            setProperty(tapXKey, profile.tapX.toString())
            setProperty(tapYKey, profile.tapY.toString())
            setProperty(swipeStartXKey, profile.swipeStartX.toString())
            setProperty(swipeStartYKey, profile.swipeStartY.toString())
            setProperty(swipeEndXKey, profile.swipeEndX.toString())
            setProperty(swipeEndYKey, profile.swipeEndY.toString())
            setProperty(swipeDurationKey, profile.swipeDurationMs.toString())
        }
        File(directory, configFileName).outputStream().use { output ->
            properties.store(output, "AppPoint profile")
        }
    }

    fun hasProfile(context: Context, app: InstalledAppInfo): Boolean {
        return File(profileDirectory(context, app), configFileName).isFile
    }

    fun select(context: Context, app: InstalledAppInfo) {
        context.getSharedPreferences(
            sessionPreferencesName,
            Context.MODE_PRIVATE
        ).edit().putString(selectedPackageKey, app.packageName).apply()
    }

    fun selectedPackage(context: Context): String? {
        return context.getSharedPreferences(
            sessionPreferencesName,
            Context.MODE_PRIVATE
        ).getString(selectedPackageKey, null)
    }

    fun loadSelected(context: Context): AppProfile? {
        val packageName = selectedPackage(context) ?: return null
        val app = InstalledAppRepository.loadLaunchableApps(context)
            .firstOrNull { it.packageName == packageName }
            ?: return loadByPackage(context, packageName)
        return load(context, app)
    }

    fun profileDirectoryName(context: Context, app: InstalledAppInfo): String {
        return profileDirectory(context, app).name
    }

    private fun profilesRoot(context: Context): File {
        return File(context.filesDir, rootDirectoryName).also { it.mkdirs() }
    }

    private fun profileDirectory(context: Context, app: InstalledAppInfo): File {
        val root = profilesRoot(context)
        val baseName = sanitizeDirectoryName(app.appName)
        val candidate = File(root, baseName)
        val existingPackage = read(File(candidate, configFileName))
            ?.getString(packageNameKey)
        if (existingPackage == null || existingPackage == app.packageName) {
            return candidate
        }
        return File(root, "$baseName--${sanitizeDirectoryName(app.packageName)}")
    }

    private fun sanitizeDirectoryName(value: String): String {
        val sanitized = value
            .trim()
            .replace(Regex("[^a-z0-9\\u4e00-\\u9fff._-]+"), "_")
            .trim('_', '.', '-')
        return sanitized.ifBlank { "unnamed_app" }
    }

    private fun read(file: File): ProfileProperties? {
        if (!file.isFile) return null
        return runCatching {
            Properties().also { properties ->
                file.inputStream().use { input ->
                    properties.load(input)
                }
            }
        }.getOrNull()?.let(::ProfileProperties)
    }

    private class ProfileProperties(
        private val properties: Properties
    ) {
        fun getString(key: String): String? {
            return properties.getProperty(key)?.trim()?.ifBlank { null }
        }

        fun getInt(key: String): Int? {
            return getString(key)?.toIntOrNull()
        }

        fun getLong(key: String): Long? {
            return getString(key)?.toLongOrNull()
        }

        fun toProfile(): AppProfile? {
            val appName = getString(appNameKey) ?: return null
            val packageName = getString(packageNameKey) ?: return null
            return AppProfile(
                appName = appName,
                packageName = packageName,
                tapX = getInt(tapXKey) ?: AutomationDefaults.tapX,
                tapY = getInt(tapYKey) ?: AutomationDefaults.tapY,
                swipeStartX = getInt(swipeStartXKey) ?: AutomationDefaults.swipeStartX,
                swipeStartY = getInt(swipeStartYKey) ?: AutomationDefaults.swipeStartY,
                swipeEndX = getInt(swipeEndXKey) ?: AutomationDefaults.swipeEndX,
                swipeEndY = getInt(swipeEndYKey) ?: AutomationDefaults.swipeEndY,
                swipeDurationMs = getLong(swipeDurationKey)
                    ?: AutomationDefaults.swipeDurationMs
            )
        }
    }
}

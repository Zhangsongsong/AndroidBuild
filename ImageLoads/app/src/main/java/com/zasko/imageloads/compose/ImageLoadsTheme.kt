package com.zasko.imageloads.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import com.zasko.imageloads.MApplication

data class AppThemeStyleOption(
    val id: String,
    val title: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val isDark: Boolean,
)

object AppThemeStyleStore {

    private const val PREF_NAME = "app_theme_style_store"
    private const val LEGACY_PREF_NAME = "app_theme_color_store"
    private const val KEY_THEME_STYLE_ID = "theme_style_id"
    private const val LEGACY_KEY_THEME_COLOR_ID = "theme_color_id"
    private const val DEFAULT_ID = "day"

    val options = listOf(
        AppThemeStyleOption(
            id = "day",
            title = "浅色",
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            background = Color(0xFFFFFBFE),
            surface = Color(0xFFFFFBFE),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurface = Color(0xFF1C1B1F),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E),
            isDark = false,
        ),
        AppThemeStyleOption(
            id = "night",
            title = "深色",
            primary = Color(0xFFD0BCFF),
            secondary = Color(0xFFCCC2DC),
            background = Color(0xFF1C1B1F),
            surface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFF49454F),
            onSurface = Color(0xFFE6E1E5),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99),
            isDark = true,
        ),
    )

    private var selectedId by mutableStateOf(loadSelectedId())

    val currentOption: AppThemeStyleOption
        get() = options.firstOrNull { it.id == selectedId } ?: options.first()

    fun isSelected(option: AppThemeStyleOption): Boolean {
        return option.id == currentOption.id
    }

    fun select(option: AppThemeStyleOption) {
        selectedId = option.id
        getPreferences()
            .edit()
            .putString(KEY_THEME_STYLE_ID, option.id)
            .apply()
    }

    private fun loadSelectedId(): String {
        val selectedStyleId = getPreferences().getString(KEY_THEME_STYLE_ID, null)
        if (selectedStyleId != null) {
            return selectedStyleId.takeIf { id -> options.any { it.id == id } } ?: DEFAULT_ID
        }
        val legacyColorId = getLegacyPreferences().getString(LEGACY_KEY_THEME_COLOR_ID, null)
        val migratedId = if (legacyColorId == null || legacyColorId == "teal") {
            DEFAULT_ID
        } else {
            "day"
        }
        getPreferences()
            .edit()
            .putString(KEY_THEME_STYLE_ID, migratedId)
            .apply()
        return migratedId
            .takeIf { id -> options.any { it.id == id } }
            ?: DEFAULT_ID
    }

    private fun getPreferences() = MApplication.application.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE,
    )

    private fun getLegacyPreferences() = MApplication.application.getSharedPreferences(
        LEGACY_PREF_NAME,
        Context.MODE_PRIVATE,
    )
}

@Composable
fun ImageLoadsTheme(
    applySystemBars: Boolean = true,
    content: @Composable () -> Unit,
) {
    val themeStyle = AppThemeStyleStore.currentOption
    val colorScheme = if (themeStyle.isDark) {
        darkColorScheme(
            primary = themeStyle.primary,
            onPrimary = Color(0xFF381E72),
            primaryContainer = Color(0xFF4F378B),
            onPrimaryContainer = Color(0xFFEADDFF),
            inversePrimary = Color(0xFF6750A4),
            secondary = themeStyle.secondary,
            onSecondary = Color(0xFF332D41),
            secondaryContainer = Color(0xFF4A4458),
            onSecondaryContainer = Color(0xFFE8DEF8),
            tertiary = Color(0xFFEFB8C8),
            onTertiary = Color(0xFF492532),
            tertiaryContainer = Color(0xFF633B48),
            onTertiaryContainer = Color(0xFFFFD8E4),
            background = themeStyle.background,
            onBackground = themeStyle.onSurface,
            surface = themeStyle.surface,
            onSurface = themeStyle.onSurface,
            surfaceVariant = themeStyle.surfaceVariant,
            onSurfaceVariant = themeStyle.onSurfaceVariant,
            outline = themeStyle.outline,
            outlineVariant = Color(0xFF49454F),
            inverseSurface = Color(0xFFE6E1E5),
            inverseOnSurface = Color(0xFF313033),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            scrim = Color(0xFF000000),
        )
    } else {
        lightColorScheme(
            primary = themeStyle.primary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            inversePrimary = Color(0xFFD0BCFF),
            secondary = themeStyle.secondary,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            tertiary = Color(0xFF7D5260),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF31111D),
            background = themeStyle.background,
            onBackground = themeStyle.onSurface,
            surface = themeStyle.surface,
            onSurface = themeStyle.onSurface,
            surfaceVariant = themeStyle.surfaceVariant,
            onSurfaceVariant = themeStyle.onSurfaceVariant,
            outline = themeStyle.outline,
            outlineVariant = Color(0xFFCAC4D0),
            inverseSurface = Color(0xFF313033),
            inverseOnSurface = Color(0xFFF4EFF4),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            scrim = Color(0xFF000000),
        )
    }
    if (applySystemBars) {
        val view = LocalView.current
        SideEffect {
            view.context.findActivity()?.window?.applyAppSystemBars(themeStyle)
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Suppress("DEPRECATION")
private fun Window.applyAppSystemBars(themeStyle: AppThemeStyleOption) {
    val backgroundColor = themeStyle.background.toArgb()
    statusBarColor = backgroundColor
    navigationBarColor = backgroundColor
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val appearance = if (themeStyle.isDark) {
            0
        } else {
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        }
        insetsController?.setSystemBarsAppearance(
            appearance,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        var flags = decorView.systemUiVisibility
        flags = if (themeStyle.isDark) {
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = if (themeStyle.isDark) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
        decorView.systemUiVisibility = flags
    }
}

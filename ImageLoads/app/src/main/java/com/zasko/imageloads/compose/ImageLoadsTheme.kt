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
            title = "白天",
            primary = Color(0xFF018786),
            secondary = Color(0xFF03DAC5),
            background = Color(0xFFF8FAFD),
            surface = Color.White,
            surfaceVariant = Color(0xFFE8EAED),
            onSurface = Color(0xFF333333),
            onSurfaceVariant = Color(0xFF666666),
            outline = Color(0xFFE0E3EB),
            isDark = false,
        ),
        AppThemeStyleOption(
            id = "night",
            title = "夜晚",
            primary = Color(0xFF8AB4F8),
            secondary = Color(0xFF81C995),
            background = Color(0xFF101418),
            surface = Color(0xFF1B1F24),
            surfaceVariant = Color(0xFF2B3036),
            onSurface = Color(0xFFE8EAED),
            onSurfaceVariant = Color(0xFFBDC1C6),
            outline = Color(0xFF3C4043),
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
            onPrimary = Color(0xFF102A43),
            secondary = themeStyle.secondary,
            onSecondary = Color(0xFF0B2E13),
            background = themeStyle.background,
            onBackground = themeStyle.onSurface,
            surface = themeStyle.surface,
            onSurface = themeStyle.onSurface,
            surfaceVariant = themeStyle.surfaceVariant,
            onSurfaceVariant = themeStyle.onSurfaceVariant,
            outline = themeStyle.outline,
        )
    } else {
        lightColorScheme(
            primary = themeStyle.primary,
            onPrimary = Color.White,
            secondary = themeStyle.secondary,
            onSecondary = Color.Black,
            background = themeStyle.background,
            onBackground = themeStyle.onSurface,
            surface = themeStyle.surface,
            onSurface = themeStyle.onSurface,
            surfaceVariant = themeStyle.surfaceVariant,
            onSurfaceVariant = themeStyle.onSurfaceVariant,
            outline = themeStyle.outline,
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

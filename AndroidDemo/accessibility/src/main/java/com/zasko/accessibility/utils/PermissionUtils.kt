package com.zasko.accessibility.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log

object PermissionUtils {

    private const val TAG = "PermissionUtils"


    fun isAccessibilitySettingOn(context: Context, clazz: Class<out AccessibilityService?>): Boolean {

        var enable = false

        try {
            enable = Settings.Secure.getInt(context.applicationContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) == 1

        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }

        if (enable) {
            val splitter = SimpleStringSplitter(':')
            //获取启动的无障碍服务
            val settingValue: String = Settings.Secure.getString(
                context.applicationContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (!TextUtils.isEmpty(settingValue)) {
                splitter.setString(settingValue)
                while (splitter.hasNext()) {
                    val tmpService = splitter.next()
                    Log.d(TAG, "isAccessibilitySettingOn: $tmpService")
                    if (tmpService.equals("${context.packageName}/${clazz.canonicalName}")) {
                        return true
                    }
                }
            }

        }
        return false
    }

    fun openSettingAccess(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /**
     * 浮窗权限
     */
    fun isEnableFloatWindowAndOpen(context: Context): Boolean {
        return Settings.canDrawOverlays(context.applicationContext)

    }

    fun openSettingFloatWindow(context: Context) {
        if (!isEnableFloatWindowAndOpen(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
        }
    }
}
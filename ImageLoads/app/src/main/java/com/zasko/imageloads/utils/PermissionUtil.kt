package com.zasko.imageloads.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zasko.imageloads.components.LogComponent
import kotlin.math.acos

object PermissionUtil {

    private const val TAG = "PermissionUtil"

    fun checkReadExternal(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkWriteExternal(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun getReadAndWriteExternal(context: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            LogComponent.printD(tag = TAG, message = "getReadAndWriteExternal ${Environment.isExternalStorageManager()}")
            if (!Environment.isExternalStorageManager()) {
                context.startActivityForResult(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    this.setData(Uri.parse("package:${context.packageName}"))
                }, 101)
            }
        } else {
            ActivityCompat.requestPermissions(
                context, mutableListOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE/*, android.Manifest.permission.WRITE_EXTERNAL_STORAGE*/
                ).filter {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_DENIED
                }.toTypedArray(), 101
            )
        }
    }

}
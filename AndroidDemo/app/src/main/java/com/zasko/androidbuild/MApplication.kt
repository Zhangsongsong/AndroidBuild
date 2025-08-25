package com.zasko.androidbuild

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.core.content.getSystemService
import com.zasko.androidbuild.activity.PlayerCache
import com.zasko.androidbuild.components.HttpComponent
import com.zasko.androidbuild.components.LogComponent


class MApplication : Application() {

    companion object {

        lateinit var application: Application
    }


    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        application = this
    }


    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate() {
        super.onCreate()
        LogComponent.printD(tag = "MApplication", message = getCurrentProcessName())
        if (!isMainProcess()) {
            return
        }
        HttpComponent.init(this)
        PlayerCache.init(this)
    }

    private fun isMainProcess(): Boolean {
        return packageName == getCurrentProcessName()
    }

    private fun getCurrentProcessName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            val am = getSystemService<ActivityManager>()
            val runningProcess = am?.runningAppProcesses ?: emptyList()
            val currentPid = Process.myPid()
            val currentProcess = runningProcess.find { it.pid == currentPid }
            currentProcess?.processName ?: ""
        }
    }
}
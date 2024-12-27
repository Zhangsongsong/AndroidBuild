package com.zasko.accessibility

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zasko.accessibility.databinding.ActivityMainBinding
import com.zasko.accessibility.service.DemoAccessibilityService
import com.zasko.accessibility.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var currentActivity: MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).root)
        Log.d(MainActivity::class.simpleName, "onCreate: ")

        currentActivity = this
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    private fun checkPermission() {
        if (!PermissionUtils.isAccessibilitySettingOn(this, DemoAccessibilityService::class.java)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }


}
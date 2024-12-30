package com.zasko.accessibility

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zasko.accessibility.databinding.ActivityMainBinding
import com.zasko.accessibility.manager.FloatWindowManager
import com.zasko.accessibility.service.DemoAccessibilityService
import com.zasko.accessibility.utils.PermissionUtils
import com.zasko.accessibility.utils.onClick

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.simpleName
    }

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: ")
        initView()
    }


    override fun onResume() {
        super.onResume()
        checkStatus()
    }

    private fun initView() {
        binding.accessSwitchView.onClick {
            if (!PermissionUtils.isAccessibilitySettingOn(this, DemoAccessibilityService::class.java)) {
                PermissionUtils.openSettingAccess(this)
            }
        }

        binding.openFloatSwitchView.setSwitch(false)
        binding.openFloatSwitchView.onClick {
            if (PermissionUtils.isEnableFloatWindowAndOpen(this)) {
                binding.openFloatSwitchView.setSwitch(true)
                FloatWindowManager.showFloatWindow(this)
            } else {
                PermissionUtils.openSettingFloatWindow(this)
            }
        }
    }

    private fun checkStatus() {
        binding.accessSwitchView.setSwitch(PermissionUtils.isAccessibilitySettingOn(this, DemoAccessibilityService::class.java))
    }

}
package com.zasko.androidbuild.activity

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.zasko.androidbuild.BaseActivity
import com.zasko.androidbuild.databinding.ActivityChangeThemeBinding
import com.zasko.androidbuild.utils.onClick

class ChangeThemeActivity : BaseActivity() {

    companion object {

        const val TAG = "ChangeThemeActivity"
        fun start(context: Context) {
            context.startActivity(Intent(context, ChangeThemeActivity::class.java))
        }
    }

    private lateinit var binding: ActivityChangeThemeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        binding = ActivityChangeThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {

        binding.defaultBtn.onClick {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
        }

        binding.nightBtn.onClick {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)


        }

        binding.lightBtn.onClick {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)


        }

    }
}

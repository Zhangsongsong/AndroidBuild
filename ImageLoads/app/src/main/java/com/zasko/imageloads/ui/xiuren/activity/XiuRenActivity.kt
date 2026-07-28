package com.zasko.imageloads.ui.xiuren.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.ui.xiuren.XiuRenFragment
import com.zasko.imageloads.ui.xiuren.XiuRenRandomFragment

class XiuRenActivity : BaseActivity() {

    companion object {
        private const val KEY_DATA = "key_data"
        private const val KEY_MODE = "key_mode"
        private const val MODE_LIST = "mode_list"
        private const val MODE_RANDOM = "mode_random"

        fun start(context: Context, data: MainThemeSelectInfo) {
            context.startActivity(Intent(context, XiuRenActivity::class.java).apply {
                putExtra(KEY_DATA, data)
                putExtra(KEY_MODE, MODE_LIST)
            })
        }

        fun startRandom(context: Context, data: MainThemeSelectInfo) {
            context.startActivity(Intent(context, XiuRenActivity::class.java).apply {
                putExtra(KEY_DATA, data)
                putExtra(KEY_MODE, MODE_RANDOM)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val containerId = View.generateViewId()
        setContentView(
            FrameLayout(this).apply {
                id = containerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            },
        )

        if (savedInstanceState == null) {
            val data = readThemeInfo()
            supportFragmentManager
                .beginTransaction()
                .replace(containerId, createFragment(data = data))
                .commit()
        }
    }

    private fun createFragment(data: MainThemeSelectInfo) = when (intent.getStringExtra(KEY_MODE)) {
        MODE_RANDOM -> XiuRenRandomFragment.newInstance(data = data)
        else -> XiuRenFragment.newInstance(data = data)
    }

    private fun readThemeInfo(): MainThemeSelectInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(KEY_DATA, MainThemeSelectInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(KEY_DATA) as? MainThemeSelectInfo
        } ?: MainThemeSelectInfo()
    }
}

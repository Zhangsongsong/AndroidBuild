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

class XiuRenActivity : BaseActivity() {

    companion object {
        private const val KEY_DATA = "key_data"

        fun start(context: Context, data: MainThemeSelectInfo) {
            context.startActivity(Intent(context, XiuRenActivity::class.java).apply {
                putExtra(KEY_DATA, data)
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
            supportFragmentManager
                .beginTransaction()
                .replace(containerId, XiuRenFragment.newInstance(readThemeInfo()))
                .commit()
        }
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

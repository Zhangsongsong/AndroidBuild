package com.zasko.imageloads.ui.taotu

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.MainThemeSelectInfo

class TaoTuActivity : BaseActivity() {

    companion object {
        private const val KEY_DATA = "key_data"
        private const val KEY_SHOW_FAVORITES = "key_show_favorites"

        fun start(context: Context, data: MainThemeSelectInfo) {
            context.startActivity(Intent(context, TaoTuActivity::class.java).apply {
                putExtra(KEY_DATA, data)
            })
        }

        fun startFavorite(context: Context, data: MainThemeSelectInfo) {
            context.startActivity(Intent(context, TaoTuActivity::class.java).apply {
                putExtra(KEY_DATA, data)
                putExtra(KEY_SHOW_FAVORITES, true)
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
                .replace(
                    containerId,
                    TaoTuFragment.newInstance(
                        data = readThemeInfo(),
                        showFavoritesOnly = intent.getBooleanExtra(KEY_SHOW_FAVORITES, false),
                    ),
                )
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

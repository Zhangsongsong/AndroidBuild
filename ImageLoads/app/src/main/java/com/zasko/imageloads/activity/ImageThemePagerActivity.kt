package com.zasko.imageloads.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.fragment.main.XiuRenFragment

class ImageThemePagerActivity : BaseActivity() {
    companion object {
        private const val TAG = "ImageThemePager"


        private const val KEY_THEME = "key_theme"
        const val THEME_XIUREN = 1


        fun start(context: Context, theme: Int) {
            context.startActivity(Intent(context, ImageThemePagerActivity::class.java).apply {
                putExtra(KEY_THEME, theme)
            })
        }


    }


    private var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_theme_pager)


        fragment = when (intent.getIntExtra(KEY_THEME, THEME_XIUREN)) {

            else -> {
                XiuRenFragment()
            }
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragmentLayout, fragment!!).commit()

    }
}
package com.zasko.imageloads.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.MainThemeSelectInfo
import com.zasko.imageloads.fragment.LoadBaseFragment
import com.zasko.imageloads.ui.xiuren.XiuRenFragment

class PersonListActivity : BaseActivity() {
    companion object {
        private const val TAG = "ImageThemePager"


        private const val KEY_DATA = "key_theme"

        fun start(context: Context, data: MainThemeSelectInfo) {
            context.startActivity(Intent(context, PersonListActivity::class.java).apply {
                putExtra(KEY_DATA, data)
            })
        }
    }

    private var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_theme_pager)
        val dataInfo = intent.getSerializableExtra(KEY_DATA) as MainThemeSelectInfo
        fragment = when (dataInfo.theme) {
            else -> {
                XiuRenFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(LoadBaseFragment.KEY_DATA, dataInfo)
                    }
                }
            }
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragmentLayout, fragment!!).commit()

    }
}
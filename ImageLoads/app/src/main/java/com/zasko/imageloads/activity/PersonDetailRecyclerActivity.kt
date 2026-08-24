package com.zasko.imageloads.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.fragment.PersonDetailRecyclerFragment

class PersonDetailRecyclerActivity : BaseActivity() {

    companion object {
        private const val KEY_DATA = "key_data"

        fun start(activity: Activity, data: ImageLoadsInfo) {
            activity.startActivity(Intent(activity, PersonDetailRecyclerActivity::class.java).apply {
                putExtra(KEY_DATA, data)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover_detail)
        if (savedInstanceState == null) {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(KEY_DATA, ImageLoadsInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(KEY_DATA) as? ImageLoadsInfo
            }
            if (info != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentLayout, PersonDetailRecyclerFragment.newInstance(info))
                    .commit()
            }
        }
    }
}

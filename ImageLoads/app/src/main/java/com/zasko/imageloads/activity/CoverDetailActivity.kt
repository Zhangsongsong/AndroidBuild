package com.zasko.imageloads.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.MainLoadsInfo
import com.zasko.imageloads.fragment.ImageDetailFragment

class CoverDetailActivity : BaseActivity() {

    companion object {

        private const val KEY_DATA = "key_data"

        fun start(activity: Activity, data: MainLoadsInfo) {
            val intent = Intent(activity, CoverDetailActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(KEY_DATA, data)
            activity.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover_detail)
        val fragment = ImageDetailFragment().apply {
            arguments = Bundle().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    putSerializable(ImageDetailFragment.KEY_DATA, intent.getSerializableExtra(KEY_DATA, MainLoadsInfo::class.java))
                } else {
                    putSerializable(ImageDetailFragment.KEY_DATA, intent.getSerializableExtra(KEY_DATA))
                }
            }
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragmentLayout, fragment).commit()
    }

}
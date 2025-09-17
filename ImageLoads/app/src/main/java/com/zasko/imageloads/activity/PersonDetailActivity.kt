package com.zasko.imageloads.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.fragment.PersonDetailFragment

class PersonDetailActivity : BaseActivity() {

    companion object {

        private const val KEY_DATA = "key_data"

        fun start(activity: Activity, data: ImageLoadsInfo) {
            val intent = Intent(activity, PersonDetailActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(KEY_DATA, data)
            activity.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover_detail)
        val fragment = PersonDetailFragment().apply {
            arguments = Bundle().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    putSerializable(PersonDetailFragment.KEY_DATA, intent.getSerializableExtra(KEY_DATA, ImageLoadsInfo::class.java))
                } else {
                    putSerializable(PersonDetailFragment.KEY_DATA, intent.getSerializableExtra(KEY_DATA))
                }
            }
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragmentLayout, fragment).commit()
    }

}
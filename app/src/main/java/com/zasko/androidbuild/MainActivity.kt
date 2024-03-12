package com.zasko.androidbuild

import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.zasko.androidbuild.activity.ChangeThemeActivity
import com.zasko.androidbuild.activity.CustomViewActivity
import com.zasko.androidbuild.adapter.NormalAdapter
import com.zasko.androidbuild.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    companion object {

        const val TAG = ""
        const val ID_CUSTOM_VIEW = 1

        const val ID_CHANGE_THEME = 2

    }

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: ")


        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = NormalAdapter { pos, info ->
            when (info.id) {
                ID_CUSTOM_VIEW -> {
                    CustomViewActivity.start(this)
                }

                ID_CHANGE_THEME -> {
                    ChangeThemeActivity.start(this)

                }
            }
        }.apply {
            this.setData(
                arrayListOf(
                    NormalAdapter.ItemInfo(id = ID_CUSTOM_VIEW, title = getString(R.string.custom_view)),
                    NormalAdapter.ItemInfo(id = ID_CHANGE_THEME, title = getString(R.string.change_theme))
                )
            )
        }

        binding.recyclerView.adapter = adapter

    }


}
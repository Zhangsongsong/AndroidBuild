package com.zasko.androidbuild

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.zasko.androidbuild.activity.CustomViewActivity
import com.zasko.androidbuild.adapter.NormalAdapter
import com.zasko.androidbuild.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    companion object {
        const val ID_CUSTOM_VIEW = 1

    }

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = NormalAdapter { pos, info ->
            when (info.id) {
                ID_CUSTOM_VIEW -> {
                    CustomViewActivity.start(this)
                }
            }
        }.apply {
            this.setData(
                arrayListOf(
                    NormalAdapter.ItemInfo(id = ID_CUSTOM_VIEW, title = getString(R.string.custom_view)),
                )
            )
        }

        binding.recyclerView.adapter = adapter

    }


}
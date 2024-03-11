package com.zasko.androidbuild.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.zasko.androidbuild.BaseActivity
import com.zasko.androidbuild.R
import com.zasko.androidbuild.adapter.NormalAdapter
import com.zasko.androidbuild.databinding.ActivityCustomViewBinding

class CustomViewActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, CustomViewActivity::class.java))
        }
    }

    private lateinit var binding: ActivityCustomViewBinding

    private var mAdapter: NormalAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAdapter = NormalAdapter { _, info ->
            when (info.id) {
                1 -> {
                    SeekBarActivity.start(this)
                }

            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = mAdapter

        mAdapter?.setData(arrayListOf(NormalAdapter.ItemInfo(1, getString(R.string.custom_seekbar))))

    }

}
package com.zasko.androidbuild.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.zasko.androidbuild.BaseActivity
import com.zasko.androidbuild.databinding.ActivitySeekBarBinding
import com.zasko.androidbuild.utils.onClick
import com.zasko.androidbuild.views.CustomSeekBar

class SeekBarActivity : BaseActivity() {


    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SeekBarActivity::class.java))

        }
    }

    private lateinit var binding: ActivitySeekBarBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeekBarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {

        binding.titleInclude.backIv.onClick {
            finish()
        }

        binding.seekbar.setProgressBarListener(object : CustomSeekBar.ProgressBarListener {
            override fun onProgressChanged(progress: Int, fromUser: Boolean) {
                super.onProgressChanged(progress, fromUser)
//                binding.timeTv.text = "$progress"
                binding.timeTv.text = "${binding.seekbar.getProgress()}"
                binding.timeIntTv.text = "$progress"
            }
        })


    }
}
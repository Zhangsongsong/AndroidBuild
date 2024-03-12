package com.zasko.androidbuild.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.zasko.androidbuild.BaseActivity
import com.zasko.androidbuild.databinding.ActivityMaterialBinding
import com.zasko.androidbuild.utils.onClick

class MaterialActivity : BaseActivity() {

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, MaterialActivity::class.java))
        }
    }

    private lateinit var binding: ActivityMaterialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaterialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.springBtn.onClick {
            SpringAnimationActivity.start(this)
        }
    }
}
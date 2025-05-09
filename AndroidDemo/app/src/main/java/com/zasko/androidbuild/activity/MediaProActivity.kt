package com.zasko.androidbuild.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.zasko.androidbuild.BaseActivity
import com.zasko.androidbuild.BaseComposeActivity
import com.zasko.androidbuild.databinding.ActivtiyMediaProjectionBinding

class MediaProActivity : BaseActivity() {


    companion object {
        fun startAct(context: Context) {
            val intent = Intent(context, MediaProActivity::class.java)
            context.startActivity(intent)
        }
    }


    private lateinit var binding: ActivtiyMediaProjectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivtiyMediaProjectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        
    }


}



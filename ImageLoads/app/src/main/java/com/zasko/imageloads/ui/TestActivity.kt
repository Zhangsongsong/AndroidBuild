package com.zasko.imageloads.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.ui.meizi5.toMeizi5ImageModel
import com.zasko.imageloads.utils.loadImage

class TestActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, TestActivity::class.java))
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<ImageView>(R.id.textIv).loadImage(
            "https://meizi5.com/wp-content/uploads/2026/04/VOL_348_face.jpg".toMeizi5ImageModel(),
        )

    }
}

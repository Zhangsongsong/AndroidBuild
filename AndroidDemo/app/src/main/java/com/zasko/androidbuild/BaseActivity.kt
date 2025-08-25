package com.zasko.androidbuild

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zasko.androidbuild.base.BindLife

open class BaseActivity : AppCompatActivity(), BindLife by BindLife() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


}
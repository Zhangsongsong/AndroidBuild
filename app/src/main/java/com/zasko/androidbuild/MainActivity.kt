package com.zasko.androidbuild

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.zasko.androidbuild.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
package com.zasko.accessibility

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zasko.accessibility.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).root)
    }

}
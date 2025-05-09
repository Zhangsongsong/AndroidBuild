package com.zasko.androidbuild

import android.os.Bundle
import androidx.activity.ComponentActivity

open class BaseComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
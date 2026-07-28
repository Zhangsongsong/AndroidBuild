package com.zasko.imageloads.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

open class BaseComposeActivity : ComponentActivity(), BindLife by BindLife() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        cancelJobs()
        lifeCompositeDisposable.clear()
        super.onDestroy()
    }
}

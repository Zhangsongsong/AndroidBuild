package com.zasko.imageloads.base

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity

open class BaseComposeActivity : FragmentActivity(), BindLife by BindLife() {

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

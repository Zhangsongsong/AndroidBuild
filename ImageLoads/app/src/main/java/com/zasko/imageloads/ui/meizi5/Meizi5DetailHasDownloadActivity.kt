package com.zasko.imageloads.ui.meizi5

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zasko.imageloads.base.BaseActivity

class Meizi5DetailHasDownloadActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, Meizi5DetailHasDownloadActivity::class.java))
        }
    }

    private var containerId = View.NO_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        containerId = View.generateViewId()
        setContentView(
            FrameLayout(this).apply {
                id = containerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            },
        )

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(containerId, Meizi5DetailHasDownloadFragment.newInstance())
                .commit()
        }
    }

    fun showDownloadedImages(name: String, path: String) {
        supportFragmentManager
            .beginTransaction()
            .replace(containerId, Meizi5DownloadedImagesFragment.newInstance(name = name, path = path))
            .addToBackStack(null)
            .commit()
    }
}

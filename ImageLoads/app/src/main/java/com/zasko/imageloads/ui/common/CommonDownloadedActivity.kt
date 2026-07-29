package com.zasko.imageloads.ui.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zasko.imageloads.base.BaseActivity

class CommonDownloadedActivity : BaseActivity() {

    companion object {
        private const val KEY_PARENT_PATH = "key_parent_path"

        fun start(context: Context, parentPath: String) {
            context.startActivity(Intent(context, CommonDownloadedActivity::class.java).apply {
                putExtra(KEY_PARENT_PATH, parentPath)
            })
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
                .replace(
                    containerId,
                    CommonDownloadedGroupsFragment.newInstance(parentPath = readParentPath()),
                )
                .commit()
        }
    }

    fun showDownloadedImages(name: String, path: String) {
        supportFragmentManager
            .beginTransaction()
            .replace(containerId, CommonDownloadedImagesFragment.newInstance(name = name, path = path))
            .addToBackStack(null)
            .commit()
    }

    private fun readParentPath(): String {
        return intent.getStringExtra(KEY_PARENT_PATH).orEmpty()
    }
}

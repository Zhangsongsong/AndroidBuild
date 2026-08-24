package com.zasko.imageloads.ui.common

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.zasko.imageloads.R
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.fragment.CommonImageDetailRecyclerFragment

class CommonImageDetailRecyclerActivity : BaseActivity() {

    companion object {
        private const val KEY_TITLE = "common_recycler_title"
        private const val KEY_SUBTITLES = "common_recycler_subtitles"
        private const val KEY_PICTURES = "common_recycler_pictures"
        private const val KEY_COVER_URL = "common_recycler_cover_url"
        private const val KEY_REFERER = "common_recycler_referer"

        fun start(
            context: Context,
            title: String,
            subtitles: List<String>,
            pictures: List<ImageLoadsInfo>,
            coverUrl: String,
            referer: String,
        ) {
            context.startActivity(Intent(context, CommonImageDetailRecyclerActivity::class.java).apply {
                putExtra(KEY_TITLE, title)
                putStringArrayListExtra(KEY_SUBTITLES, ArrayList(subtitles))
                putExtra(KEY_PICTURES, ArrayList(pictures))
                putExtra(KEY_COVER_URL, coverUrl)
                putExtra(KEY_REFERER, referer)
            })
        }

        internal fun readTitle(intent: Intent): String {
            return intent.getStringExtra(KEY_TITLE).orEmpty()
        }

        internal fun readSubtitles(intent: Intent): List<String> {
            return intent.getStringArrayListExtra(KEY_SUBTITLES).orEmpty()
        }

        internal fun readPictures(intent: Intent): List<ImageLoadsInfo> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(KEY_PICTURES, ArrayList::class.java)
                    ?.let { (it as? ArrayList<*>)?.filterIsInstance<ImageLoadsInfo>() }
                    .orEmpty()
            } else {
                @Suppress("DEPRECATION")
                (intent.getSerializableExtra(KEY_PICTURES) as? ArrayList<*>)
                    ?.filterIsInstance<ImageLoadsInfo>()
                    .orEmpty()
            }
        }

        internal fun readCoverUrl(intent: Intent): String {
            return intent.getStringExtra(KEY_COVER_URL).orEmpty()
        }

        internal fun readReferer(intent: Intent): String {
            return intent.getStringExtra(KEY_REFERER).orEmpty()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover_detail)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentLayout,
                    CommonImageDetailRecyclerFragment.newInstance(
                        title = readTitle(intent),
                        subtitles = readSubtitles(intent),
                        pictures = readPictures(intent),
                        coverUrl = readCoverUrl(intent),
                        referer = readReferer(intent),
                    ),
                )
                .commit()
        }
    }
}

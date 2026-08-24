package com.zasko.imageloads.fragment

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.zasko.imageloads.components.HttpHeaderConfigStore
import com.zasko.imageloads.compose.ImageLoadsTheme
import com.zasko.imageloads.compose.ImageLoadsTopBar
import com.zasko.imageloads.data.ImageInfo
import com.zasko.imageloads.data.ImageLoadsInfo
import com.zasko.imageloads.ui.common.CommonDetailRecyclerAdapter
import com.zasko.imageloads.ui.common.CommonImageDetailInfo

class CommonImageDetailRecyclerFragment : ComposeBaseFragment() {

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_SUBTITLES = "subtitles"
        private const val KEY_PICTURES = "pictures"
        private const val KEY_REFERER = "referer"

        fun newInstance(
            title: String,
            subtitles: List<String>,
            pictures: List<ImageLoadsInfo>,
            coverUrl: String,
            referer: String,
        ): CommonImageDetailRecyclerFragment {
            return CommonImageDetailRecyclerFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_TITLE, title)
                    putStringArrayList(KEY_SUBTITLES, ArrayList(subtitles))
                    putSerializable(KEY_PICTURES, ArrayList(pictures))
                    putString(KEY_REFERER, referer)
                }
            }
        }
    }

    private val title: String
        get() = arguments?.getString(KEY_TITLE).orEmpty()
    private val subtitles: List<String>
        get() = arguments?.getStringArrayList(KEY_SUBTITLES).orEmpty()
    private val pictures: List<ImageLoadsInfo>
        get() {
            @Suppress("DEPRECATION")
            return (arguments?.getSerializable(KEY_PICTURES) as? ArrayList<*>)
                .orEmpty()
                .filterIsInstance<ImageLoadsInfo>()
        }
    private val referer: String
        get() = arguments?.getString(KEY_REFERER).orEmpty()

    @Composable
    override fun FragmentContent() {
        val context = LocalContext.current
        val detailInfo = remember(title, subtitles, pictures) {
            CommonImageDetailInfo(
                title = title,
                subtitles = subtitles,
                pictures = pictures,
            )
        }
        ImageLoadsTheme {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    ImageLoadsTopBar(
                        title = title,
                        onBack = { activity?.finish() },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            RecyclerView(context).apply {
                                itemAnimator = null
                                layoutManager = LinearLayoutManager(context)
                                adapter = CommonDetailRecyclerAdapter(
                                    imageColumnCount = 1,
                                    imageModelProvider = ::toHeaderAwareModel,
                                    onLoadMore = {},
                                    onImageClick = { _, index ->
                                        ImagePreviewFragment.show(
                                            fragmentManager = parentFragmentManager,
                                            imageUrls = detailInfo.pictures.map { it.url },
                                            initialIndex = index,
                                            referer = referer,
                                        )
                                    },
                                ).also { adapter ->
                                    adapter.submit(detailInfo = detailInfo, isLoadingMore = false)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    private fun toHeaderAwareModel(imageInfo: ImageLoadsInfo): GlideUrl {
        val imageUrl = imageInfo.url.trim()
        return GlideUrl(
            imageUrl,
            LazyHeaders.Builder().apply {
                HttpHeaderConfigStore.getHeadersForUrl(url = imageUrl).forEach { header ->
                    addHeader(header.name, header.value)
                }
                if (referer.isNotBlank()) {
                    addHeader("Referer", referer)
                }
            }.build(),
        )
    }
}

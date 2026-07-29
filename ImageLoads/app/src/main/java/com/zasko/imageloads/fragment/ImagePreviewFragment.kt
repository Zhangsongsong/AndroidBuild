package com.zasko.imageloads.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.zasko.imageloads.R
import com.zasko.imageloads.compose.GlideImage
import com.zasko.imageloads.compose.ImageLoadsTheme

private const val MAX_PREVIEW_SCALE = 5f

class ImagePreviewFragment : DialogFragment() {

    companion object {
        private const val TAG = "ImagePreviewFragment"
        private const val KEY_URL = "key_url"
        private const val KEY_REFERER = "key_referer"

        fun show(
            fragmentManager: FragmentManager,
            imageUrl: String,
            referer: String,
        ) {
            if (imageUrl.isBlank() || fragmentManager.isStateSaved) {
                return
            }
            ImagePreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_URL, imageUrl)
                    putString(KEY_REFERER, referer)
                }
            }.show(fragmentManager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ImageLoadsTheme {
                    ImagePreviewContent(
                        model = readImageUrl().toPreviewImageModel(referer = readReferer()),
                        onClose = { dismissAllowingStateLoss() },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    private fun readImageUrl(): String {
        return arguments?.getString(KEY_URL).orEmpty()
    }

    private fun readReferer(): String {
        return arguments?.getString(KEY_REFERER).orEmpty()
    }
}

@Composable
private fun ImagePreviewContent(
    model: Any?,
    onClose: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        GlideImage(
            model = model,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, MAX_PREVIEW_SCALE)
                        scale = nextScale
                        offset = if (nextScale <= 1f) {
                            Offset.Zero
                        } else {
                            offset + pan
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
            scaleType = ImageView.ScaleType.FIT_CENTER,
        )
        IconButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 28.dp)
                .size(48.dp),
            onClick = onClose,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

private fun String.toPreviewImageModel(referer: String): Any {
    val imageUrl = trim()
    if (imageUrl.startsWith("/") || imageUrl.startsWith("file:") || imageUrl.startsWith("content:")) {
        return imageUrl
    }
    val builder = LazyHeaders.Builder()
        .addHeader(
            "User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
        )
        .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
        .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
    if (referer.isNotBlank()) {
        builder.addHeader("Referer", referer)
    }
    return GlideUrl(imageUrl, builder.build())
}

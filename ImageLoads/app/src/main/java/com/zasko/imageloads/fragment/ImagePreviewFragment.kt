package com.zasko.imageloads.fragment

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.zasko.imageloads.R
import com.zasko.imageloads.components.HttpHeaderConfigStore
import com.zasko.imageloads.compose.GlideImage
import com.zasko.imageloads.compose.ImageLoadsTheme
import android.graphics.Color as AndroidColor

private const val MAX_PREVIEW_SCALE = 5f

class ImagePreviewFragment : DialogFragment() {

    private var hostWindowState: SystemBarWindowState? = null

    companion object {
        private const val TAG = "ImagePreviewFragment"
        private const val KEY_URL = "key_url"
        private const val KEY_URLS = "key_urls"
        private const val KEY_INDEX = "key_index"
        private const val KEY_REFERER = "key_referer"

        fun show(
            fragmentManager: FragmentManager,
            imageUrl: String,
            referer: String,
        ) {
            show(
                fragmentManager = fragmentManager,
                imageUrls = listOf(imageUrl),
                initialIndex = 0,
                referer = referer,
            )
        }

        fun show(
            fragmentManager: FragmentManager,
            imageUrls: List<String>,
            initialIndex: Int,
            referer: String,
        ) {
            val validUrls = imageUrls.map { it.trim() }.filter { it.isNotBlank() }
            if (validUrls.isEmpty() || fragmentManager.isStateSaved) {
                return
            }
            ImagePreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_URL, validUrls[initialIndex.coerceIn(validUrls.indices)])
                    putStringArrayList(KEY_URLS, ArrayList(validUrls))
                    putInt(KEY_INDEX, initialIndex.coerceIn(validUrls.indices))
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
        val imageUrls = readImageUrls()
        val initialIndex = readInitialIndex(imageUrls = imageUrls)
        val referer = readReferer()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ImageLoadsTheme {
                    ImagePreviewContent(
                        imageUrls = imageUrls,
                        initialIndex = initialIndex,
                        referer = referer,
                        onClose = { dismissAllowingStateLoss() },
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        activity?.window?.let { window ->
            if (hostWindowState == null) {
                hostWindowState = window.readSystemBarWindowState()
            }
            window.applyPreviewSystemBars()
        }
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            applyPreviewSystemBars()
        }
    }

    override fun onStop() {
        hostWindowState?.let { state ->
            activity?.window?.restoreSystemBarWindowState(state)
            hostWindowState = null
        }
        super.onStop()
    }

    private fun readImageUrl(): String {
        return arguments?.getString(KEY_URL).orEmpty()
    }

    private fun readImageUrls(): List<String> {
        val urls = arguments?.getStringArrayList(KEY_URLS).orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return urls.ifEmpty {
            listOf(readImageUrl()).filter { it.isNotBlank() }
        }
    }

    private fun readInitialIndex(imageUrls: List<String>): Int {
        if (imageUrls.isEmpty()) {
            return 0
        }
        return arguments?.getInt(KEY_INDEX, 0)?.coerceIn(imageUrls.indices) ?: 0
    }

    private fun readReferer(): String {
        return arguments?.getString(KEY_REFERER).orEmpty()
    }

    @Suppress("DEPRECATION")
    private fun Window.applyPreviewSystemBars() {
        setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
        decorView.setBackgroundColor(AndroidColor.BLACK)
        clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        statusBarColor = AndroidColor.BLACK
        navigationBarColor = AndroidColor.BLACK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            decorView.systemUiVisibility = flags
        }
    }

    @Suppress("DEPRECATION")
    private fun Window.readSystemBarWindowState(): SystemBarWindowState {
        return SystemBarWindowState(
            statusBarColor = statusBarColor,
            navigationBarColor = navigationBarColor,
            systemUiVisibility = decorView.systemUiVisibility,
            systemBarsAppearance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController?.systemBarsAppearance ?: 0
            } else {
                0
            },
        )
    }

    @Suppress("DEPRECATION")
    private fun Window.restoreSystemBarWindowState(state: SystemBarWindowState) {
        statusBarColor = state.statusBarColor
        navigationBarColor = state.navigationBarColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insetsController?.setSystemBarsAppearance(
                state.systemBarsAppearance,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            )
        } else {
            decorView.systemUiVisibility = state.systemUiVisibility
        }
    }
}

private data class SystemBarWindowState(
    val statusBarColor: Int,
    val navigationBarColor: Int,
    val systemUiVisibility: Int,
    val systemBarsAppearance: Int,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagePreviewContent(
    imageUrls: List<String>,
    initialIndex: Int,
    referer: String,
    onClose: () -> Unit,
) {
    if (imageUrls.isEmpty()) {
        return
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(imageUrls.indices),
        pageCount = { imageUrls.size },
    )
    var currentScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pagerState.currentPage) {
        currentScale = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize(),
            userScrollEnabled = imageUrls.size > 1 && currentScale <= 1f,
            key = { page -> imageUrls[page] },
        ) { page ->
            ImagePreviewPage(
                imageUrl = imageUrls[page],
                referer = referer,
                isCurrentPage = page == pagerState.currentPage,
                onScaleChanged = { scale ->
                    currentScale = scale
                },
            )
        }
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
        if (imageUrls.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 42.dp)
                    .background(Color(0x66000000), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ImagePreviewPage(
    imageUrl: String,
    referer: String,
    isCurrentPage: Boolean,
    onScaleChanged: (Float) -> Unit,
) {
    var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    val model = remember(imageUrl, referer) {
        imageUrl.toPreviewImageModel(referer = referer)
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
        }
    }
    LaunchedEffect(isCurrentPage, scale) {
        if (isCurrentPage) {
            onScaleChanged(scale)
        }
    }

    GlideImage(
        model = model,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imageUrl) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        val pressedCount = pressedChanges.size
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (pressedCount > 1) {
                            val nextScale = (scale * zoomChange).coerceIn(1f, MAX_PREVIEW_SCALE)
                            scale = nextScale
                            offset = if (nextScale <= 1f) {
                                Offset.Zero
                            } else {
                                offset + panChange
                            }
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = pressedChanges.firstOrNull()
                            if (change != null && scale > 1f) {
                                offset += change.positionChange()
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
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
}

private fun String.toPreviewImageModel(referer: String): Any {
    val imageUrl = trim()
    if (imageUrl.startsWith("/") || imageUrl.startsWith("file:") || imageUrl.startsWith("content:")) {
        return imageUrl
    }
    val configuredHeaders = HttpHeaderConfigStore.getHeadersForUrl(url = imageUrl)
    val builder = LazyHeaders.Builder()
    if (configuredHeaders.isNotEmpty()) {
        configuredHeaders.forEach { header ->
            builder.addHeader(header.name, header.value)
        }
    } else {
        builder.addHeader(
            "User-Agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
        )
            .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        if (referer.isNotBlank()) {
            builder.addHeader("Referer", referer)
        }
    }
    return GlideUrl(imageUrl, builder.build())
}

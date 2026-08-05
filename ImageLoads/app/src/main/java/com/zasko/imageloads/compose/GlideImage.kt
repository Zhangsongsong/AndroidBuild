package com.zasko.imageloads.compose

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.zasko.imageloads.R

@Composable
fun GlideImage(
    model: Any?,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    @DrawableRes placeholderRes: Int? = null,
    requestWidth: Int? = null,
    requestHeight: Int? = null,
    diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.DATA,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(Color(0xFFEFEFEF)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = placeholderRes ?: R.mipmap.icon_pic),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentScale = ContentScale.Fit,
            )
        }
        return
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                adjustViewBounds = false
                this.scaleType = scaleType
            }
        },
        update = { imageView ->
            imageView.scaleType = scaleType
            val requestKey = GlideImageRequestKey(
                model = model,
                placeholderRes = placeholderRes,
                requestWidth = requestWidth,
                requestHeight = requestHeight,
                diskCacheStrategy = diskCacheStrategy,
            )
            if (imageView.getTag(R.id.tag_glide_image_request_key) == requestKey) {
                return@AndroidView
            }
            imageView.setTag(R.id.tag_glide_image_request_key, requestKey)
            val request = Glide.with(imageView)
                .load(model)
                .diskCacheStrategy(diskCacheStrategy)
                .dontAnimate()
            if (requestWidth != null && requestHeight != null && requestWidth > 0 && requestHeight > 0) {
                request.override(requestWidth, requestHeight)
            }
            if (placeholderRes != null) {
                request.placeholder(placeholderRes)
            }
            request.into(imageView)
        },
    )
}

private data class GlideImageRequestKey(
    val model: Any?,
    @DrawableRes val placeholderRes: Int?,
    val requestWidth: Int?,
    val requestHeight: Int?,
    val diskCacheStrategy: DiskCacheStrategy,
)

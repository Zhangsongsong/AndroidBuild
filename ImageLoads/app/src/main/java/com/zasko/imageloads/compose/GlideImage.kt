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
            val request = Glide.with(imageView)
                .load(model)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
            if (placeholderRes != null) {
                request.placeholder(placeholderRes)
            }
            request.into(imageView)
        },
    )
}

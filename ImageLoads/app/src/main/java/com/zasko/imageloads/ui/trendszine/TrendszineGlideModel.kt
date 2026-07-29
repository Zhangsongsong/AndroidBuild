package com.zasko.imageloads.ui.trendszine

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders

private const val TRENDSZINE_HOME_URL = "https://trendszine.com/"
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"

fun String.toTrendszineImageModel(): GlideUrl {
    return GlideUrl(
        trim(),
        LazyHeaders.Builder()
            .addHeader("User-Agent", DESKTOP_USER_AGENT)
            .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .addHeader("Referer", TRENDSZINE_HOME_URL)
            .build(),
    )
}

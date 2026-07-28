package com.zasko.imageloads.ui.meizi5

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders

private const val MEIZI5_HOME_URL = "https://meizi5.com/"
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"

fun String.toMeizi5ImageModel(): GlideUrl {
    return GlideUrl(
        trim(),
        LazyHeaders.Builder()
            .addHeader("User-Agent", DESKTOP_USER_AGENT)
            .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .addHeader("Referer", MEIZI5_HOME_URL)
            .build(),
    )
}

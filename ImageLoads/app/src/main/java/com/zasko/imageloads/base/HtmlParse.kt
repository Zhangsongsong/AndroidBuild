package com.zasko.imageloads.base

import com.zasko.imageloads.data.ImageLoadsInfo

interface HtmlParse {


    fun getDomain(): String {
        return ""
    }

    fun transformHome(data: String): List<ImageLoadsInfo> {
        return emptyList()
    }

    fun transformDetail(data: String): List<Any>? {
        return null
    }
}
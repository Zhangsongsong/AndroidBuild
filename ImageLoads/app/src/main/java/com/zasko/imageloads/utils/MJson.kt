package com.zasko.imageloads.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object MJson {


    fun parse(html: String): Document {
        return Jsoup.parse(html)

    }

}
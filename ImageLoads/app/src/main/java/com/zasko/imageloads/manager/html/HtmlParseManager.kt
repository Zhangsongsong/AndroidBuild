package com.zasko.imageloads.manager.html

import android.content.Context
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.utils.FileUtil
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single
import org.jsoup.Jsoup

object HtmlParseManager {

    private const val TAG = "HtmlParseManager"


    fun parseBaiduByLocal(context: Context): Single<StringBuilder> {
        return Single.just(true).map {
            FileUtil.getFileToHtml(context = context, fileName = "html/baidu.html") ?: StringBuilder("")
        }.doOnSuccess { data ->
            LogComponent.printD(tag = TAG, message = "parseBaiduByLocal Thread:${Thread.currentThread().name}")
            val doc = Jsoup.parse(data.toString())

        }.switchThread()
    }

    fun parseXiuRenByLocal(context: Context): Single<StringBuilder> {
        return Single.just(true).map {
            FileUtil.getFileToHtml(context = context, fileName = "html/xiuren.html") ?: StringBuilder("")
        }
    }
}
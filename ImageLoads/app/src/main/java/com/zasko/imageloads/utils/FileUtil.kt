package com.zasko.imageloads.utils

import android.content.Context
import android.os.Environment
import com.zasko.imageloads.MApplication
import com.zasko.imageloads.components.LogComponent
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

object FileUtil {

    private const val TAG = "FileUtil"


    private const val APP_CACHE_NAME = "ImageLoads"
    private const val APP_DOWNLOAD = "download"


    private const val PICTURE_OTHERS = "others"

    const val PICTURE_XIUREN = "xiuren"

    /**
     *               --xiuren
     *                       --others
     *                       --01.img
     *                       --02.img
     *               --heisi
     *
     *
     *
     */


    fun getFileToHtml(context: Context, fileName: String): StringBuilder? {
        LogComponent.printD(tag = "FileUtil", message = "getFileToHtml Thread:${Thread.currentThread().name}")
        var inputStream: InputStream? = null
        var reader: BufferedReader? = null
        var inputStreamReader: InputStreamReader? = null
        try {
            inputStream = context.assets.open(fileName)
            inputStreamReader = InputStreamReader(inputStream)
            reader = BufferedReader(inputStreamReader)
            val stringBuild = StringBuilder()
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                stringBuild.append(line)
            }
            return stringBuild
        } catch (e: Exception) {
        } finally {
            reader?.close()
            inputStreamReader?.close()
            inputStream?.close()
        }
        return null
    }

    fun createExternalDir() {
        val rootFile = Environment.getExternalStorageDirectory()
        val appFile = File(rootFile, APP_CACHE_NAME)
        LogComponent.printD(
            tag = TAG, message = "createExternalDir appFile:${appFile.exists()} permission:${appFile.absolutePath}"
        )
        if (!appFile.exists()) {
            appFile.mkdirs()
        }
        val downloadFile = File(appFile, APP_DOWNLOAD)
        if (!downloadFile.exists()) {
            downloadFile.mkdirs()
        }
    }

    fun getDownloadPath(): String {
        return "${Environment.getExternalStorageDirectory()}/${APP_CACHE_NAME}/${APP_DOWNLOAD}"
    }

}

fun String.getUrlToName(): Pair<String, String> {
    if (this.isEmpty()) {
        return "" to ""
    }
    //https://i.xiutaku.com/photo/uploadfile/pic/17383.webp
    val name = this.split("/").lastOrNull() ?: ""
    val last = name.split(".")
    return last[0] to last[1]
}

fun String.getUrlToSuffix(): String {
    if (this.isEmpty()) {
        return this
    }
    return this.split(".").lastOrNull() ?: ""
}

fun Int.makeUpTen(): String {
    return this.toString().padStart(7, '0')
}

fun String.toFileNameByIndex(index: Int): String {
    return "${index.makeUpTen()}.${this.getUrlToSuffix()}"
}

package com.zasko.imageloads.utils

import android.content.Context
import android.os.Environment
import com.zasko.imageloads.MApplication
import com.zasko.imageloads.components.LogComponent
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader

object FileUtil {

    private const val TAG = "FileUtil"


    private const val APP_CACHE_NAME = "ImageLoads"
    private const val APP_DOWNLOAD = "download"


    private const val PICTURE_OTHERS = "others"

    const val PICTURE_XIUREN = "xiuren"
    const val PICTURE_MEIZI5 = "meizi5"
    const val PICTURE_MEIZI5_COVERS = "covers"
    const val PICTURE_MEIZI5_DETAIL = "detail"
    const val PICTURE_TAOTU = "taotu"
    const val PICTURE_TAOTU_DETAIL = "detail"
    const val PICTURE_TRENDSZINE = "trendszine"
    const val PICTURE_TRENDSZINE_COVERS = "covers"
    const val PICTURE_TRENDSZINE_DETAIL = "detail"
    const val NAME_XIUREN = "xiuren"
    const val NAME_MEIZI5 = "meizi5"
    const val NAME_TAOTU = "taotu"
    const val NAME_TRENDSZINE = "trendszine"

    /**
     * Local storage layout currently used by the app:
     *
     * Private app external files:
     * /storage/emulated/0/Android/data/com.zasko.imageloads/files/
     *   html/
     *     xiuren/
     *       {page}
     *     meizi5/
     *       {page}
     *       detail/
     *         {detail-html-file}
     *     taotu/
     *       home
     *       {page}
     *       detail/
     *         {detail-html-file}
     *
     * Public download files:
     * /storage/emulated/0/ImageLoads/
     *   download/
     *     xiuren/
     *       {album-name}/
     *         0000000.jpg
     *         0000001.jpg
     *     meizi5/
     *       covers/
     *         {cover-file-name}
     *       detail/
     *         {detail-title}/
     *           {image-file-name}
     *     taotu/
     *       detail/
     *         {detail-title}/
     *           {image-file-name}
     *     trendszine/
     *       covers/
     *         {cover-file-name}
     *       detail/
     *         {detail-title}/
     *           {image-file-name}
     */


    fun getAssessFileToHtml(context: Context, fileName: String): StringBuilder? {
        LogComponent.printD(tag = "FileUtil", message = "getAssessFileToHtml Thread:${Thread.currentThread().name}")
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

    fun getFileToHtml(file: File): StringBuilder? {
        var inputStream: InputStream? = null
        var reader: BufferedReader? = null
        var inputStreamReader: InputStreamReader? = null
        try {
            inputStream = FileInputStream(file)
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
        runCatching {
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
        }.onFailure { throwable ->
            LogComponent.printE(tag = TAG, message = "createExternalDir failed:${throwable}")
        }
    }

    fun getDownloadPath(): String {
        return "${Environment.getExternalStorageDirectory()}/${APP_CACHE_NAME}/${APP_DOWNLOAD}"
    }


    /**
     * 本地跟目录
     */
    fun getPrivateDir(): String {
        val file = MApplication.application.getExternalFilesDir(null)
        LogComponent.printD(TAG, "getPrivateDir file:${file?.absolutePath}")
        return file?.absolutePath ?: ""
    }

    /**
     * 本地Html目录
     */
    fun getPrivateHtmlDir(): String {
        return "${getPrivateDir()}/html"
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

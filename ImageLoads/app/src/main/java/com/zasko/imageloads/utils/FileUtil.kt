package com.zasko.imageloads.utils

import android.content.Context
import com.zasko.imageloads.components.LogComponent
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object FileUtil {

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
}
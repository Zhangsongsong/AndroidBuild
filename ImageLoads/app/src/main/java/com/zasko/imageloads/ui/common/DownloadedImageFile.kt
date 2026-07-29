package com.zasko.imageloads.ui.common

import java.io.File

internal fun File.isDownloadedImageFile(): Boolean {
    val fileName = name.lowercase()
    return fileName.endsWith(".jpg") ||
        fileName.endsWith(".jpeg") ||
        fileName.endsWith(".png") ||
        fileName.endsWith(".webp")
}

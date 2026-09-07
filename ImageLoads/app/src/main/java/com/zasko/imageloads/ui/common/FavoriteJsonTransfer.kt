package com.zasko.imageloads.ui.common

import android.content.Context
import android.net.Uri
import com.zasko.imageloads.data.MainThemeSelectInfo

object FavoriteJsonTransfer {

    fun sourceFromTheme(theme: MainThemeSelectInfo?): FavoriteBackupSource? {
        return theme?.let(FavoriteBackupManager::sourceFromTheme)
    }

    fun createExportFileName(source: FavoriteBackupSource): String {
        return FavoriteBackupManager.createFavoritesExportFileName(source = source)
    }

    fun exportToUri(context: Context, uri: Uri, source: FavoriteBackupSource): Int {
        val json = FavoriteBackupManager.createFavoritesBackupJson(source = source)
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { output ->
            output.write(json)
        } ?: throw IllegalStateException("open output stream failed")
        return FavoriteBackupManager.getFavoriteCount(source = source)
    }

    fun importFromUri(context: Context, uri: Uri, source: FavoriteBackupSource): FavoriteImportResult {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { input ->
            input.readText()
        } ?: throw IllegalStateException("open input stream failed")
        return FavoriteBackupManager.importFavoritesBackupJson(rawData = json, source = source)
    }
}

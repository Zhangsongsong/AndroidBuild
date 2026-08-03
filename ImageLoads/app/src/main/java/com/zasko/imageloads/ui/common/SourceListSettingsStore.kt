package com.zasko.imageloads.ui.common

import android.content.Context
import com.zasko.imageloads.MApplication
import com.zasko.imageloads.components.SourceLocalDataStore

object SourceListSettingsStore {

    private const val PREF_NAME = "source_list_settings_store"
    private const val KEY_USE_LOCAL_DATA_PREFIX = "use_local_data_"

    fun isLocalDataEnabled(sourceType: Int): Boolean {
        SourceLocalDataStore.getLocalDataEnabled(sourceType = sourceType)?.let { enabled ->
            return enabled
        }
        val enabled = getPreferences().getBoolean(sourceType.toUseLocalDataKey(), true)
        SourceLocalDataStore.setLocalDataEnabled(sourceType = sourceType, enabled = enabled)
        return enabled
    }

    fun setLocalDataEnabled(sourceType: Int, enabled: Boolean) {
        SourceLocalDataStore.setLocalDataEnabled(sourceType = sourceType, enabled = enabled)
    }

    private fun Int.toUseLocalDataKey(): String {
        return KEY_USE_LOCAL_DATA_PREFIX + this
    }

    private fun getPreferences() = MApplication.application.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE,
    )
}

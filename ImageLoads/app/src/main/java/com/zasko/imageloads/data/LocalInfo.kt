package com.zasko.imageloads.data

import kotlinx.serialization.Serializable


enum class DataUseFrom(val value: Int) {
    NETWORK(0), PRIVATE_FILE(1)
}

@Serializable
data class MainThemeSelectInfo(
    var cover: String = "", var title: String = "", var dataUseFrom: Int = DataUseFrom.NETWORK.value, var theme: Int = 0
) : java.io.Serializable
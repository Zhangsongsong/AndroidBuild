package com.zasko.imageloads.data

import kotlinx.serialization.Serializable


@Serializable
data class MainLoadsInfo(var url: String = "", var width: Int = -1, var height: Int = -1, var isDownload: Boolean = false)


@Serializable
data class HeiSiInfo(var data: String = "")
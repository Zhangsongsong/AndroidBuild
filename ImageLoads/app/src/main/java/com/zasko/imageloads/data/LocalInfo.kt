package com.zasko.imageloads.data

import kotlinx.serialization.Serializable


@Serializable
data class MainThemeSelectInfo(var cover: String = "", var title: String = "")
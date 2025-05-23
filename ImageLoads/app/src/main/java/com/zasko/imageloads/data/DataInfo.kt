package com.zasko.imageloads.data

import kotlinx.serialization.Serializable
import java.io.Serializable as SerializableJava

@Serializable
data class MainLoadsInfo(var url: String = "", var width: Int = -1, var height: Int = -1, var href: String = "") : SerializableJava


@Serializable
data class HeiSiInfo(var data: String = "")


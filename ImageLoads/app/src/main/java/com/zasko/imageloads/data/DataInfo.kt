package com.zasko.imageloads.data

import kotlinx.serialization.Serializable
import java.io.Serializable as SerializableJava

@Serializable
data class MainLoadsInfo(var url: String = "", var width: Int = -1, var height: Int = -1, var href: String = "") : SerializableJava


@Serializable
data class HeiSiInfo(var data: String = "")


@Serializable
data class ImageInfo(var url: String = "", var width: Int = 0, var height: Int = 0)

@Serializable
data class TagsInfo(var id: String = "")


@Serializable
data class ImageDetailInfo(
    var name: String = "", var desc: String = "", var time: String = "", var tags: List<TagsInfo>? = null, var pictures: List<ImageInfo>? = null
)


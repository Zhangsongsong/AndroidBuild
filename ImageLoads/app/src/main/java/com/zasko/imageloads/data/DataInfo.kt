package com.zasko.imageloads.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

object DataInfo {}


data class MainLoadsInfo(var url: String = "")


@Serializable
data class TestInfo(var code: Int = 0)
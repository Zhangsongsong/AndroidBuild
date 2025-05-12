package com.zasko.imageloads.data

import kotlinx.serialization.Serializable


@Serializable
data class MainLoadsInfo(var data: String = "")


@Serializable
data class TestInfo(var code: Int = 0)
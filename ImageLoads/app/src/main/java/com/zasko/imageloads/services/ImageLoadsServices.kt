package com.zasko.imageloads.services

import com.zasko.imageloads.data.TestInfo
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.Serializer
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ImageLoadsServices {


    @GET
    fun getImage(@Url url: String = "https://v2.api-m.com/api/heisi?return=2"): Single<TestInfo>
}


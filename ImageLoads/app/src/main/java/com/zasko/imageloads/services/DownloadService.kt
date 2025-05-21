package com.zasko.imageloads.services

import io.reactivex.rxjava3.core.Single
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DownloadService {


    @Streaming
    @GET
    fun downloadImage(@Url url: String = ""): Single<ResponseBody>
}
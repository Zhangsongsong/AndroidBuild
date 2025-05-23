package com.zasko.imageloads.services

import com.zasko.imageloads.data.HeiSiInfo
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface ImageLoadsServices {


    @GET
    fun getImage(@Url url: String = "https://v2.api-m.com/api/heisi?return=2"): Single<HeiSiInfo>


    /**
     * 秀人网站
     */
    @GET
    fun getXiuRen(@Url url: String = "https://xiutaku.com/", @Query("start") start: Int = 0): Single<String>

    @GET
    fun getXiuRenDetail(@Url url: String = ""): Single<String>

}


package com.zasko.imageloads.services

import com.zasko.imageloads.data.HeiSiInfo
import com.zasko.imageloads.data.MainLoadsInfo
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Url

interface ImageLoadsServices {


    @GET
    fun getImage(@Url url: String = "https://v2.api-m.com/api/heisi?return=2"): Single<HeiSiInfo>

}


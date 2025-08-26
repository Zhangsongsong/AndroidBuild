package com.zasko.androidbuild.services

import com.zasko.androidbuild.components.HttpComponent
import com.zasko.androidbuild.utils.switchThread
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

object KCHttpHelper {

    private const val TAG = "KCHttpHelper"

    private val server by lazy {
        HttpComponent.getRetrofit().create(KCServices::class.java)
    }

    fun postVerify(params: VideoVerifyParams): Single<NetworkBaseResponse<VideoVerifyInfo>> {
        return server.postVideoVerify(params = params)
    }


}


interface KCServices {

    @POST
    fun postVideoVerify(
        @Url url: String = "https://testservice.kcredshort.com/api/skits/verify?cc=GF10000&smid=DUOwYv1qECPiryUd7B2CK02_nh9PtekL096eRFVPd1l2MXFFQ1BpcnlVZDdCMkNLMDJfbmg5UHRla0wwOTZlc2h1&dev_name=Google&ik_appid=cmVkc2hvcnQ6UkVEU0hPUlQ&conn=wifi&ua=GooglePixel5&sid=20TEAb7jVLv1dNgtDEqXAUSuZUR3Ptz7tEzaR4hHtpZXkpuFgzVPWA1dTzvl3BAMgT3n8i3&uid=100724&mtid=d9c471b76710fca876003681b785db87&cv=REDSHORT1.12.0_Android&lc=3000000000000000&eaid=35613537373036363961373634633864&mtxid=020000000000&osversion=android_34&oaid=930570ad-a46e-4dbd-a9f1-10ca1ad1abe1&cv_new=REDSHORT1.12.0_Android_APP_REDSHORT&log_id=&lang=en-US&device_id=930570ad-a46e-4dbd-a9f1-10ca1ad1abe1",
        @Body params: VideoVerifyParams
    ): Single<NetworkBaseResponse<VideoVerifyInfo>>


}

@Serializable
data class NetworkBaseResponse<Data>(val dm_error: Int, val error_msg: String = "", val data: Data?)


@Serializable
data class VideoVerifyParams(
    val skits_id: Int,
    val part_no: Int,
)

@Serializable
data class VideoVerifyInfo(
    var resource_url: String = ""
)


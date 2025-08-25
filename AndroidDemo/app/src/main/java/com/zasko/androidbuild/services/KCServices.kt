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

    fun postVerify(params: VideoVerifyParams): Single<String> {
        return server.postVideoVerify(params = params).switchThread()
    }


}


interface KCServices {


    @POST
    fun postVideoVerify(
        @Url url: String = "https://testservice.kcredshort.com/api/skits/verify?cc=GF10000&smid=DUOwYv1qECPiryUd7B2CK02_nh9PtekL096eRFVPd1l2MXFFQ1BpcnlVZDdCMkNLMDJfbmg5UHRla0wwOTZlc2h1&dev_name=Google&ik_appid=cmVkc2hvcnQ6UkVEU0hPUlQ&conn=wifi&ua=GooglePixel5&sid=20dyHd1Vgt10JsuaWi2tOUxBfdQ2Ui0aFnAVjqZMrmMDHi2Wk4F7fX4O19eUoZfg6vrj9dli2APh3ObNHhcwpv&uid=1000002866&mtid=d9c471b76710fca876003681b785db87&cv=REDSHORT1.12.0_Android&lc=3000000000000000&eaid=35613537373036363961373634633864&mtxid=020000000000&osversion=android_34&oaid=930570ad-a46e-4dbd-a9f1-10ca1ad1abe1&cv_new=REDSHORT1.12.0_Android_APP_REDSHORT&log_id=&lang=en-US&device_id=930570ad-a46e-4dbd-a9f1-10ca1ad1abe1",
        @Body params: VideoVerifyParams
    ): Single<String>


}

@Serializable
data class VideoVerifyParams(
    val skits_id: Int,
    val part_no: Int,
)
package com.zasko.imageloads.manager

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.zasko.imageloads.components.HttpComponent
import com.zasko.imageloads.components.LogComponent
import com.zasko.imageloads.services.DownloadService
import com.zasko.imageloads.utils.switchThread
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody
import java.io.File

object DownloadManager {

    private const val TAG = "DownloadManager"

    private val downloadService by lazy {
        HttpComponent.getRetrofit().create(DownloadService::class.java)
    }

    fun downloadImageByHttp(url: String = "", file: File): Single<ResponseBody> {
        return downloadService.downloadImage(url).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).doOnSuccess { body ->

        }
    }

    /**
     *  FileUtil.getXiuRenDir()?.let { parentFile ->
     *             val dir = FileUtil.getOthersFile(parentFile = parentFile).absolutePath
     *             val url = "https://i.xiutaku.com/photo/uploadfile/pic/17383.webp"
     *             val name = url.getUrlToName()
     *             val saveFile = File("${dir}/${name.first}.${name.second}")
     *             DownloadManager.downloadImageByGlide(
     *                 context = requireContext(), url = url, file = saveFile
     *             ).bindLife()
     *  }
     */
    fun downloadImageByGlide(context: Context, url: String = "", file: File): Single<File> {
        return Single.just(false).map {
            val requestBuild = Glide.with(context).asFile().diskCacheStrategy(DiskCacheStrategy.DATA).load(url).submit()
            LogComponent.printD(tag = TAG, message = "downloadImageByGlide url:${url}")
            requestBuild.get()
        }.subscribeOn(Schedulers.io()).doOnSuccess {
            it.copyTo(file)
            LogComponent.printD(tag = TAG, message = "downloadImageByGlide file:$it  saveFile:${file.exists()}")
        }.doOnError {
            LogComponent.printD(tag = TAG, message = "downloadImageByGlide error:$it")
        }

    }
}
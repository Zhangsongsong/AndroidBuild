package com.zasko.imageloads.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import com.zasko.imageloads.data.HeiSiInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.IOException

object ComposeHttpComponent {

    private const val MAX_IO_RETRY_COUNT = 2

    private val imageServer by lazy {
        HttpComponent.getRetrofit().create(ComposeImageLoadsServices::class.java)
    }

    suspend fun getImage(url: String = "https://v2.api-m.com/api/heisi?return=2"): HeiSiInfo {
        return ioRequest {
            imageServer.getImage(url = url)
        }
    }

    suspend fun getXiuRen(url: String = "https://xiutaku.com/", start: Int = 0): String {
        return ioRequest {
            imageServer.getXiuRen(url = url, start = start)
        }
    }

    suspend fun getXiuRenDetail(url: String = ""): String {
        return ioRequest {
            imageServer.getXiuRenDetail(url = url)
        }
    }

    suspend fun getMeizi5(url: String = "https://meizi5.com/"): String {
        return ioRequest {
            imageServer.getMeizi5(url = url)
        }
    }

    @Composable
    fun <T> rememberRequestState(
        key: Any?,
        request: suspend () -> T,
    ): State<ComposeRequestState<T>> {
        val currentRequest = rememberUpdatedState(newValue = request)
        return produceState<ComposeRequestState<T>>(
            initialValue = ComposeRequestState.Loading,
            key1 = key,
        ) {
            value = ComposeRequestState.Loading
            try {
                value = ComposeRequestState.Success(currentRequest.value())
            } catch (e: CancellationException) {
                throw e
            } catch (throwable: Throwable) {
                value = ComposeRequestState.Error(throwable)
            }
        }
    }

    @Composable
    fun rememberImageState(
        url: String = "https://v2.api-m.com/api/heisi?return=2",
    ): State<ComposeRequestState<HeiSiInfo>> {
        return rememberRequestState(key = url) {
            getImage(url = url)
        }
    }

    @Composable
    fun rememberXiuRenState(
        url: String = "https://xiutaku.com/",
        start: Int = 0,
    ): State<ComposeRequestState<String>> {
        return rememberRequestState(key = url to start) {
            getXiuRen(url = url, start = start)
        }
    }

    @Composable
    fun rememberXiuRenDetailState(url: String): State<ComposeRequestState<String>> {
        return rememberRequestState(key = url) {
            getXiuRenDetail(url = url)
        }
    }

    private suspend fun <T> ioRequest(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            retryIoRequest(block = block)
        }
    }

    private suspend fun <T> retryIoRequest(block: suspend () -> T): T {
        var lastError: IOException? = null
        repeat(MAX_IO_RETRY_COUNT + 1) { attempt ->
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                lastError = e
                if (attempt == MAX_IO_RETRY_COUNT) {
                    throw e
                }
                delay(400L * (attempt + 1))
            }
        }
        throw lastError ?: IOException("Compose request failed")
    }
}

sealed interface ComposeRequestState<out T> {
    data object Loading : ComposeRequestState<Nothing>
    data class Success<T>(val data: T) : ComposeRequestState<T>
    data class Error(val throwable: Throwable) : ComposeRequestState<Nothing>
}

private interface ComposeImageLoadsServices {

    @GET
    suspend fun getImage(@Url url: String = "https://v2.api-m.com/api/heisi?return=2"): HeiSiInfo

    @GET
    @Headers(
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,ko;q=0.7",
        "Referer: https://xiutaku.com/",
        "Sec-CH-UA: \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Google Chrome\";v=\"150\"",
        "Sec-CH-UA-Mobile: ?0",
        "Sec-CH-UA-Platform: \"macOS\"",
        "Upgrade-Insecure-Requests: 1",
        "Sec-Fetch-Dest: document",
        "Sec-Fetch-Mode: navigate",
        "Sec-Fetch-Site: none",
    )
    suspend fun getXiuRen(@Url url: String = "https://xiutaku.com/", @Query("start") start: Int = 0): String

    @GET
    suspend fun getXiuRenDetail(@Url url: String = ""): String

    @GET
    @Headers(
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8",
        "Referer: https://meizi5.com/",
    )
    suspend fun getMeizi5(@Url url: String = "https://meizi5.com/"): String
}

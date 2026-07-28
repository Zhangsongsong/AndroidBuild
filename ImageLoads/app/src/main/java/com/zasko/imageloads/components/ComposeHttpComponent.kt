package com.zasko.imageloads.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import com.zasko.imageloads.data.HeiSiInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

object ComposeHttpComponent {

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
            block()
        }
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
    suspend fun getXiuRen(@Url url: String = "https://xiutaku.com/", @Query("start") start: Int = 0): String

    @GET
    suspend fun getXiuRenDetail(@Url url: String = ""): String
}

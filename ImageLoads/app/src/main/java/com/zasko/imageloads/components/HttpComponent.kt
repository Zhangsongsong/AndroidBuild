package com.zasko.imageloads.components

import android.app.Application
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import java.lang.reflect.Type

object HttpComponent {


    private lateinit var retrofit: Retrofit

    fun init(application: Application) {

        val httpClient = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor(logger = {
            LogComponent.printD(tag = "HttpLoggingInterceptor", message = it)
        }).setLevel(HttpLoggingInterceptor.Level.BODY)).build()

        val json = Json {
            ignoreUnknownKeys = true
        }
        retrofit = Retrofit.Builder().baseUrl("https://www.baidu.com").client(httpClient).addCallAdapterFactory(RxJava3CallAdapterFactory.create())
//            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType())).addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(CustomConverterFactory(contentType = "application/json".toMediaType(), serializer = Serializer.FromString(json)))
            .build()
    }

    fun getRetrofit(): Retrofit {
        return retrofit
    }
}

class CustomConverterFactory(
    private val contentType: MediaType, private val serializer: Serializer
) : Converter.Factory() {


    override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<ResponseBody, *> {
        LogComponent.printD(tag = "CustomConverterFactory", message = "type:${type}")
        if (type == String::class.java) {
            return StringResponseBodyConverter()
        }
        val loader = serializer.serializer(type)
        return DeserializationStrategyConverter(loader, serializer)
    }

    override fun requestBodyConverter(
        type: Type, parameterAnnotations: Array<out Annotation>, methodAnnotations: Array<out Annotation>, retrofit: Retrofit
    ): Converter<*, RequestBody> {
        val saver = serializer.serializer(type)
        return SerializationStrategyConverter(contentType, saver, serializer)
    }
}

sealed class Serializer {
    abstract fun <T> fromResponseBody(loader: DeserializationStrategy<T>, body: ResponseBody): T
    abstract fun <T> toRequestBody(contentType: MediaType, saver: SerializationStrategy<T>, value: T): RequestBody

    protected abstract val format: SerialFormat

    fun serializer(type: Type): KSerializer<Any> = format.serializersModule.serializer(type)

    class FromString(override val format: StringFormat) : Serializer() {
        override fun <T> fromResponseBody(loader: DeserializationStrategy<T>, body: ResponseBody): T {
            val string = body.string()
            return format.decodeFromString(loader, string)
        }

        override fun <T> toRequestBody(contentType: MediaType, saver: SerializationStrategy<T>, value: T): RequestBody {
            val string = format.encodeToString(saver, value)
            return RequestBody.create(contentType, string)
        }
    }
}

internal class SerializationStrategyConverter<T>(
    private val contentType: MediaType, private val saver: SerializationStrategy<T>, private val serializer: Serializer
) : Converter<T, RequestBody> {
    override fun convert(value: T) = serializer.toRequestBody(contentType, saver, value)
}

internal class DeserializationStrategyConverter<T>(
    private val loader: DeserializationStrategy<T>, private val serializer: Serializer
) : Converter<ResponseBody, T> {
    override fun convert(value: ResponseBody) = serializer.fromResponseBody(loader, value)
}

class StringResponseBodyConverter : Converter<ResponseBody, String> {
    override fun convert(value: ResponseBody): String {
        return value.string()
    }

}



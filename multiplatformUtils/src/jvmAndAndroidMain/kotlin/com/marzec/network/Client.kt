package com.marzec.network

import com.marzec.cache.FileCache
import com.marzec.cache.getTyped
import com.marzec.cache.putTyped
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

fun createHttpClient(
    fileCache: FileCache,
    authorizationHeader: String,
    preferencesHeaderKey: String
) = HttpClient(OkHttp) {
    install(JsonFeature) {
        serializer = KotlinxSerializer(
            Json(KotlinxSerializer.DefaultJson) {
                ignoreUnknownKeys = true
            }
        )
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
        println("REQUEST: ${method.value} ${url.buildString()}")
    }

    engine {
        addNetworkInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                var request = chain.request()
                runBlocking { fileCache.getTyped<String>(preferencesHeaderKey) }?.let { authForRequest ->
                    request = request.newBuilder()
                        .addHeader(authorizationHeader, authForRequest)
                        .build()
                }

                val response = chain.proceed(request)
                val authorization = response.headers[authorizationHeader]
                if (authorization != null) {
                    runBlocking { fileCache.putTyped(preferencesHeaderKey, authorization) }
                } else {
                    if (response.code == HTTP_STATUS_UNAUTHORIZED) {
                        runBlocking { fileCache.putTyped<String>(preferencesHeaderKey, null) }
                    }
                }
                return response
            }
        })
//        addNetworkInterceptor(HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        })
    }
}

private const val HTTP_STATUS_UNAUTHORIZED = 401

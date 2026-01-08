package com.marzec.network

import com.marzec.cache.FileCache
import com.marzec.cache.getTyped
import com.marzec.cache.putTyped
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.logging.HttpLoggingInterceptor

actual fun createHttpClient(
    fileCache: FileCache,
    authorizationHeader: String,
    preferencesHeaderKey: String,
    block: HttpClientConfig<*>.() -> Unit
): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
    }

    engine {
        addNetworkInterceptor { chain ->
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
            response
        }
        addNetworkInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
    }
    block()
}

private const val HTTP_STATUS_UNAUTHORIZED = 401

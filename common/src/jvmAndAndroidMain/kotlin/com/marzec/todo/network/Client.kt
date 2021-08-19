package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.DI
import com.marzec.todo.PreferencesKeys
import com.marzec.todo.cache.getTyped
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

val httpClient = HttpClient(OkHttp) {
    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
        DI.logger.log("HTTP", url.encodedPath)
    }

    engine {
        addNetworkInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                var request = chain.request()
                runBlocking { DI.fileCache.getTyped<String>(PreferencesKeys.AUTHORIZATION) }?.let { authForRequest ->
                    request = request.newBuilder()
                        .addHeader(Api.Headers.AUTHORIZATION, authForRequest)
                        .build()
                }

                val response = chain.proceed(request)
                val authorization = response.headers[Api.Headers.AUTHORIZATION]
                if (authorization != null) {
                    runBlocking { DI.fileCache.put(PreferencesKeys.AUTHORIZATION, authorization) }
                } else if (response.code == 401) {
                    runBlocking { DI.fileCache.put(PreferencesKeys.AUTHORIZATION, null) }
                }
                return response
            }
        })
    }
}
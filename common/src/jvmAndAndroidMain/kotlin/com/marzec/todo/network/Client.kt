package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.DI
import com.marzec.todo.PreferencesKeys
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

val httpClient = HttpClient(OkHttp) {
    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }

    engine {

        addNetworkInterceptor(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                var request = chain.request()
                runBlocking { DI.preferences.getString(PreferencesKeys.AUTHORIZATION) }?.let { authForRequest ->
                    request = request.newBuilder()
                        .addHeader(Api.Headers.AUTHORIZATION, authForRequest)
                        .build()
                }

                val response = chain.proceed(request)
                val authorization = response.headers[Api.Headers.AUTHORIZATION]
                if (authorization != null) {
                    runBlocking { DI.preferences.set(PreferencesKeys.AUTHORIZATION, authorization) }
                } else if (response.code == 401) {
                    runBlocking { DI.preferences.remove(PreferencesKeys.AUTHORIZATION) }
                }
                return response
            }
        })
    }
}
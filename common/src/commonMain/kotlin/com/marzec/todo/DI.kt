package com.marzec.todo

import com.marzec.todo.preferences.MemoryPreferences
import com.marzec.todo.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

object DI {

    val preferences: Preferences = MemoryPreferences()

    val client: HttpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }

        engine {

            addNetworkInterceptor(object : Interceptor{
                override fun intercept(chain: Interceptor.Chain): Response {
                    var request = chain.request()
                    runBlocking { preferences.getString(PreferencesKeys.AUTHORIZATION) }?.let { authForRequest ->
                        request = request.newBuilder()
                            .addHeader(Api.Headers.AUTHORIZATION, authForRequest)
                            .build()
                    }

                    val response = chain.proceed(request)
                    val authorization = response.headers[Api.Headers.AUTHORIZATION]
                    if (authorization != null) {
                        runBlocking { preferences.set(PreferencesKeys.AUTHORIZATION, authorization) }
                    } else {
                        runBlocking { preferences.remove(PreferencesKeys.AUTHORIZATION) }
                    }
                    return response
                }
            })
        }
    }
}

object PreferencesKeys {
    const val AUTHORIZATION = "AUTHORIZATION"
}

object Api {

    const val HOST = "http://fiteo-env.eba-mpctrvdb.us-east-2.elasticbeanstalk.com"
//    const val HOST = "http://localhost:500"

    const val BASE = "$HOST/fiteo/api/1"

    const val LOGIN = "$BASE/login"
    const val USER = "$BASE/user"
    const val LOGOUT = "$BASE/logout"

    object Headers {
        const val AUTHORIZATION  = "Authorization"
    }
}
package com.marzec.todo

import com.marzec.todo.preferences.MemoryPreferences
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import io.ktor.client.HttpClient
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

object DI {

    var ioDispatcher: CoroutineContext = Dispatchers.Default

    val preferences: Preferences = MemoryPreferences()

    lateinit var client: HttpClient

    fun provideLoginRepository() = LoginRepository(client)
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
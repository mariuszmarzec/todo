package com.marzec.todo

import androidx.compose.runtime.Composable
import com.marzec.todo.navigation.model.NavigationState
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.preferences.MemoryPreferences
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import com.marzec.todo.screen.login.LoginScreen
import com.marzec.todo.screen.login.model.LoginStore
import io.ktor.client.HttpClient
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

object DI {

    var ioDispatcher: CoroutineContext = Dispatchers.Default

    val preferences: Preferences = MemoryPreferences()

    val navigationStore by lazy {
        NavigationStore(NavigationState(listOf( @Composable { LoginScreen(provideLoginStore()) })))
    }

    lateinit var client: HttpClient

    fun provideLoginRepository() = LoginRepository(client)

    fun provideLoginStore() = LoginStore(provideLoginRepository())
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
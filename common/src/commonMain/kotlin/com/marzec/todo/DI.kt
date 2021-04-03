package com.marzec.todo

import androidx.compose.runtime.Composable
import com.marzec.todo.navigation.model.Destinations
import com.marzec.todo.navigation.model.NavigationActions
import com.marzec.todo.navigation.model.NavigationEntry
import com.marzec.todo.navigation.model.NavigationState
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.preferences.MemoryPreferences
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.screen.lists.ListsScreen
import com.marzec.todo.screen.lists.ListsScreenState
import com.marzec.todo.screen.lists.ListsScreenStore
import com.marzec.todo.screen.login.LoginScreen
import com.marzec.todo.screen.login.model.LoginData
import com.marzec.todo.screen.login.model.LoginStore
import com.marzec.todo.screen.login.model.LoginViewState
import io.ktor.client.HttpClient
import io.ktor.util.date.getTimeMillis
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DI {

    var ioDispatcher: CoroutineContext = Dispatchers.Default
    lateinit var navigationScope: CoroutineScope

    val preferences: Preferences = MemoryPreferences()

    private val router: Map<Destinations, (@Composable (cacheKey: String) -> Unit)> = mapOf(
        Destinations.LOGIN to @Composable { provideLoginScreen(it) },
        Destinations.LISTS to @Composable { provideListScreen(it) }
    )

    private val cacheKeyProvider by lazy { { getTimeMillis().toString() } }

    val navigationStore: NavigationStore by lazy {
        NavigationStore(
            router = router,
            stateCache = preferences,
            cacheKeyProvider = cacheKeyProvider,
            initialState = NavigationState(
                backStack = listOf(
                    NavigationEntry(cacheKeyProvider()) @Composable { provideLoginScreen(it) }
                )
            )
        )
    }

    @Composable
    private fun provideListScreen(cacheKey: String) {
        ListsScreen(navigationStore, provideListScreenStore(cacheKey = cacheKey))
    }

    @Composable
    private fun provideListScreenStore(cacheKey: String) = ListsScreenStore(
        todoRepository = provideTodoRepository(),
        stateCache = preferences,
        cacheKey = cacheKey,
        initialState = provideListScreenDefaultState()
    )

    private fun provideListScreenDefaultState(): ListsScreenState {
        return ListsScreenState.INITIAL
    }

    @Composable
    private fun provideLoginScreen(cacheKey: String) =
        LoginScreen(loginStore = provideLoginStore(cacheKey))

    lateinit var client: HttpClient

    fun provideLoginRepository() = LoginRepository(client)

    fun provideLoginStore(cacheKey: String): LoginStore = LoginStore(
        loginRepository = provideLoginRepository(),
        stateCache = preferences,
        cacheKey = cacheKey,
        onLoginSuccess = {
            navigationScope.launch {
                navigationStore.sendAction(NavigationActions.Next(Destinations.LISTS))
            }
        },
        initialState = LoginViewState.Data(
            loginData = LoginData(
                login = "mariusz.marzec00@gmail.com",
                password = "password"
            )
        )
    )

    fun provideTodoRepository() = TodoRepository(client)
}

object PreferencesKeys {
    const val AUTHORIZATION = "AUTHORIZATION"
}

object Api {

    const val HOST = "http://fiteo-env.eba-mpctrvdb.us-east-2.elasticbeanstalk.com/test"
//    const val HOST = "http://localhost:500"

    object Login {
        const val BASE = "$HOST/fiteo/api/1"

        const val LOGIN = "$BASE/login"
        const val USER = "$BASE/user"
        const val LOGOUT = "$BASE/logout"
    }

    object Todo {
        const val BASE = "$HOST/todo/api/1"

        const val TODO_LISTS = "$BASE/lists"
        const val TODO_LIST = "$BASE/list"
    }

    object Headers {
        const val AUTHORIZATION = "Authorization-Test"
    }
}
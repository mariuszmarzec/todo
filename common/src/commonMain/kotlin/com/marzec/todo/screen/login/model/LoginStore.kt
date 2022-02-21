package com.marzec.todo.screen.login.model

import com.marzec.content.Content
import com.marzec.content.ifDataSuspend
import com.marzec.model.User
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.mvi.reduceContentNoChanges
import com.marzec.mvi.reduceData
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationOptions
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.Preferences
import com.marzec.repository.LoginRepository
import com.marzec.todo.navigation.TodoDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class LoginStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val stateCache: Preferences,
    private val cacheKey: String,
    initialState: State<LoginData>,
    private val loginRepository: LoginRepository
) : Store3<State<LoginData>>(
    scope, stateCache.get(cacheKey) ?: initialState
) {

    override suspend fun onNewState(newState: State<LoginData>) {
        stateCache.set(cacheKey, newState)
    }

    fun login() = intent<Content<User>>("login") {
        onTrigger {
            state.ifDataAvailable {
                loginRepository.login(login, password)
            }
        }

        cancelTrigger(runSideEffectAfterCancel = true) {
            resultNonNull() is Content.Data
        }

        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }

        sideEffect {
            resultNonNull().ifDataSuspend {
                navigationStore.next(
                    NavigationAction(
                        TodoDestination.Tasks,
                        options = NavigationOptions(
                            popTo = TodoDestination.Tasks,
                            popToInclusive = true
                        )
                    )
                )
            }
        }
    }

    fun onLoginChanged(login: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(login = login) }
        }
    }

    fun onPasswordChanged(password: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(password = password) }
        }
    }
}

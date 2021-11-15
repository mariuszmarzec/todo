package com.marzec.todo.screen.login.model

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentNoChanges
import com.marzec.mvi.reduceData
import com.marzec.todo.model.User
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationAction
import com.marzec.todo.navigation.model.NavigationOptions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.network.Content
import com.marzec.todo.network.ifDataSuspend
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class LoginStore(
    private val navigationStore: NavigationStore,
    private val stateCache: Preferences,
    private val cacheKey: String,
    initialState: State<LoginData>,
    private val loginRepository: LoginRepository
) : Store2<State<LoginData>>(
    stateCache.get(cacheKey) ?: initialState
) {

    override suspend fun onNewState(newState: State<LoginData>) {
        stateCache.set(cacheKey, newState)
    }

    fun login() = intent<Content<User>> {
        onTrigger(
            isCancellableFlowTrigger = true,
            runSideEffectAfterCancel = true
        ) {
            state.ifDataAvailable {
                loginRepository.login(login, password)
                    .cancelFlowsIf { it is Content.Data }
            }
        }

        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }

        sideEffect {
            resultNonNull().ifDataSuspend {
                navigationStore.next(
                    NavigationAction(
                        Destination.Lists,
                        options = NavigationOptions(
                            popTo = Destination.Lists,
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

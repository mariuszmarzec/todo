package com.marzec.todo.screen.login.model

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentNoChanges
import com.marzec.mvi.reduceData
import com.marzec.todo.model.User
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow

@ExperimentalCoroutinesApi
class LoginStore(
    private val loginRepository: LoginRepository,
    private val stateCache: Preferences,
    private val onLoginSuccess: () -> Unit,
    private val cacheKey: String,
    initialState: State<LoginData>
) : Store2<State<LoginData>>(
    stateCache.get(cacheKey) ?: initialState
) {

    override suspend fun onNewState(newState: State<LoginData>) {
        stateCache.set(cacheKey, newState)
    }

    suspend fun sendAction(action: LoginActions) {
        when (action) {
            is LoginActions.PasswordChanged -> onPasswordChanged(action.password)
            is LoginActions.LoginChanged -> onLoginChanged(action.login)
            LoginActions.LoginButtonClick -> login()
        }
    }

    private suspend fun login() = intent<Content<User>> {
        onTrigger {
            state.ifDataAvailable {
                loginRepository.login(login, password)
            }
        }

        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }

        sideEffect {
            if (result is Content.Data) {
                onLoginSuccess()
            }
        }

    }

    private suspend fun onLoginChanged(login: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(login = login) }
        }
    }

    private suspend fun onPasswordChanged(password: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(password = password) }
        }
    }
}
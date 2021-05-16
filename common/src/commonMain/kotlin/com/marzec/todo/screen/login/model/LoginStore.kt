package com.marzec.todo.screen.login.model

import com.marzec.mvi.newMvi.Store2
import com.marzec.todo.extensions.getMessage
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
    initialState: LoginViewState
) : Store2<LoginViewState>(
    stateCache.get(cacheKey) ?: initialState
) {

    override suspend fun onNewState(newState: LoginViewState) {
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
            flow {
                emit(Content.Loading())
                emit(loginRepository.login(state.loginData.login, state.loginData.password))
            }
        }

        reducer {
            when (val res = resultNonNull()) {
                is Content.Data -> {
                    LoginViewState.Data(state.loginData)
                }
                is Content.Loading -> {
                    LoginViewState.Pending(state.loginData)
                }
                is Content.Error -> LoginViewState.Error(state.loginData, error = res.getMessage())
            }
        }

        sideEffect {
            if (result is Content.Data) {
                onLoginSuccess()
            }
            println(result)
        }

    }

    private suspend fun onLoginChanged(login: String) = intent<Unit> {
        reducer {
            val loginData = state.loginData.copy(login = login)
            when (state) {
                is LoginViewState.Data -> state.copy(loginData = loginData)
                is LoginViewState.Pending -> state.copy(loginData = loginData)
                is LoginViewState.Error -> state.copy(loginData = loginData)
            }
        }
    }

    private suspend fun onPasswordChanged(password: String) = intent<Unit> {
        reducer {
            val loginData = state.loginData.copy(password = password)
            when (state) {
                is LoginViewState.Data -> state.copy(loginData = loginData)
                is LoginViewState.Pending -> state.copy(loginData = loginData)
                is LoginViewState.Error -> state.copy(loginData = loginData)
            }
        }
    }
}
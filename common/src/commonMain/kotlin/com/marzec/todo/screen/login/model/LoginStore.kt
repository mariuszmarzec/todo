package com.marzec.todo.screen.login.model

import com.marzec.mvi.Store
import com.marzec.todo.model.User
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class LoginStore(
    private val loginRepository: LoginRepository,
    private val stateCache: Preferences,
    onLoginSuccess: () -> Unit,
    private val cacheKey: String,
    initialState: LoginViewState
) : Store<LoginViewState, LoginActions>(
    stateCache.get(cacheKey) ?: initialState
) {
    init {
        addIntent<LoginActions.LoginButtonClick, Content<User>> {
            onTrigger {
                loginRepository.login(state.loginData.login, state.loginData.password)
            }
            sideEffect {
                if (result is Content.Data) {
                    onLoginSuccess()
                }
                println(result)
            }
        }
        addIntent<LoginActions.LoginChanged> {
            reducer {
                val loginData = state.loginData.copy(login = action.login)
                when (state) {
                    is LoginViewState.Data -> state.copy(loginData = loginData)
                    is LoginViewState.Pending -> state.copy(loginData = loginData)
                    is LoginViewState.Error -> state.copy(loginData = loginData)
                }
            }
        }
        addIntent<LoginActions.PasswordChanged> {
            reducer {
                val loginData = state.loginData.copy(password = action.password)
                when (state) {
                    is LoginViewState.Data -> state.copy(loginData = loginData)
                    is LoginViewState.Pending -> state.copy(loginData = loginData)
                    is LoginViewState.Error -> state.copy(loginData = loginData)
                }
            }
        }
    }

    override suspend fun onNewState(newState: LoginViewState) {
        stateCache.set(cacheKey, newState)
    }
}
package com.marzec.todo.screen.login.model

import com.marzec.mvi.Store
import com.marzec.todo.model.User
import com.marzec.todo.network.Content
import com.marzec.todo.repository.LoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class LoginStore(
    private val loginRepository: LoginRepository
) : Store<LoginViewState, LoginActions>(
    LoginViewState.Data(LoginData(login = "", password = ""))
) {
    init {
        addIntent<LoginActions.LoginButtonClick, Content<User>> {
            onTrigger {
                loginRepository.login(state.loginData.login, state.loginData.password)
            }
            sideEffect {
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
}
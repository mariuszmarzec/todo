package com.marzec.todo.screen.login.model

import com.marzec.mvi.Intent
import com.marzec.mvi.Store
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class LoginStore : Store<LoginViewState, LoginActions>(
    LoginViewState.Data(LoginData(login = "", password = ""))
) {
    init {
        intents = mapOf(
            LoginActions.LoginButtonClick::class to Intent(
                sideEffect = { _: Any?, _ ->

                }
            ),
            LoginActions.LoginChanged::class to Intent(
                reducer = { action: Any, _: Any?, state: LoginViewState ->
                    action as LoginActions.LoginChanged
                    val loginData = state.loginData.copy(login = action.login)
                    when (state) {
                        is LoginViewState.Data -> state.copy(loginData = loginData)
                        is LoginViewState.Pending -> state.copy(loginData = loginData)
                        is LoginViewState.Error -> state.copy(loginData = loginData)
                    }
                }
            ),
            LoginActions.PasswordChanged::class to Intent(
                reducer = { action: Any, _: Any?, state: LoginViewState ->
                    action as LoginActions.PasswordChanged
                    val loginData = state.loginData.copy(password = action.password)
                    when (state) {
                        is LoginViewState.Data -> state.copy(loginData = loginData)
                        is LoginViewState.Pending -> state.copy(loginData = loginData)
                        is LoginViewState.Error -> state.copy(loginData = loginData)
                    }
                }
            )
        )
    }
}
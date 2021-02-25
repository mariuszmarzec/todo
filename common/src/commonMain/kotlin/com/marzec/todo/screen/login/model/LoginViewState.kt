package com.marzec.todo.screen.login.model

sealed class LoginViewState(open val loginData: LoginData) {

    data class Data(override val loginData: LoginData) : LoginViewState(loginData)
    data class Pending(override val loginData: LoginData) : LoginViewState(loginData)
    data class Error(override val loginData: LoginData, val error: String) : LoginViewState(loginData)
}

data class LoginData(val login: String, val password: String)
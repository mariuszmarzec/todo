package com.marzec.todo.screen.login.model

sealed class LoginActions {
    data class PasswordChanged(val password: String) : LoginActions()
    data class LoginChanged(val login: String) : LoginActions()
    object LoginButtonClick : LoginActions()
}

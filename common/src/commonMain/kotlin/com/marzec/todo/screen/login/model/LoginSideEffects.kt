package com.marzec.todo.screen.login.model

sealed class LoginSideEffects {
    object OnLoginSuccessful : LoginSideEffects()
}
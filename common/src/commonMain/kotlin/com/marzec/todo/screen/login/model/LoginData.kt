package com.marzec.todo.screen.login.model

import com.marzec.mvi.State

data class LoginData(val login: String, val password: String) {

    companion object {
        val INITIAL = State.Data(
            LoginData(
                login = "mariusz.marzec00@gmail.com",
                password = "password"
            )
        )
    }
}

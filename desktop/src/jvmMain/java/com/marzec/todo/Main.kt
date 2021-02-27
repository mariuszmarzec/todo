package com.marzec.todo

import androidx.compose.desktop.Window
import androidx.compose.ui.unit.IntSize
import com.marzec.todo.screen.login.LoginScreen
import com.marzec.todo.screen.login.model.LoginStore
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
fun main() = Window(
    title = "ToDo",
    size = IntSize(700, 768)
) {
    LoginScreen(LoginStore())
}
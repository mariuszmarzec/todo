package com.marzec.todo

import androidx.compose.desktop.Window
import androidx.compose.ui.unit.IntSize
import com.marzec.todo.screen.login.LoginScreen
import com.marzec.todo.screen.login.model.LoginStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.marzec.todo.network.httpClient
import com.marzec.todo.screen.main.MainScreen
import kotlinx.coroutines.Dispatchers

@ExperimentalCoroutinesApi
fun main() = Window(
    title = "ToDo",
    size = IntSize(700, 768)
) {
    DI.client = httpClient
    DI.ioDispatcher = Dispatchers.IO

    MainScreen(DI.navigationStore)
}
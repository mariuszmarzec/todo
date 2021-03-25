package com.marzec.todo

import androidx.compose.desktop.Window
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntSize
import com.marzec.todo.network.httpClient
import com.marzec.todo.screen.main.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
fun main() = Window(
    title = "ToDo",
    size = IntSize(700, 768)
) {
    DI.client = httpClient
    DI.ioDispatcher = Dispatchers.IO
    DI.navigationScope = rememberCoroutineScope()

    MainScreen(DI.navigationStore)
}
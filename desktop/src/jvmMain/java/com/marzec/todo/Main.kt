package com.marzec.todo

import androidx.compose.desktop.Window
import androidx.compose.ui.unit.IntSize
import com.marzec.todo.screen.login.LoginScreen

fun main() = Window(
    title = "ToDo",
    size = IntSize(700, 768)
) {
    LoginScreen()
}
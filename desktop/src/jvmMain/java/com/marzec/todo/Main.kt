package com.marzec.todo

import androidx.compose.desktop.Window
import androidx.compose.ui.unit.IntSize
import com.marzec.todo.widget.Greeting


fun main() = Window(
    title = "ToDo",
    size = IntSize(1440, 768)
) {
    Greeting("Mariusz")
}
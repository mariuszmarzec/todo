package com.marzec.todo

import androidx.compose.desktop.Window
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntSize
import com.marzec.todo.cache.FileCacheImpl
import com.marzec.todo.cache.MemoryCache
import com.marzec.todo.network.httpClient
import com.marzec.todo.screen.main.MainScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

@ExperimentalCoroutinesApi
fun main() {

    DI.fileCache = FileCacheImpl(
        "todo.cache",
        Json,
        MemoryCache()
    )
    DI.client = httpClient
    DI.ioDispatcher = Dispatchers.IO

    DI.navigationStore = runBlocking {
        DI.provideNavigationStore()
    }

    Window(
        title = "ToDo",
        size = IntSize(700, 768)
    ) {
        DI.navigationScope = rememberCoroutineScope()

        MainScreen(DI.navigationStore)
    }
}

package com.marzec.todo

import androidx.compose.desktop.Window
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntSize
import com.marzec.todo.cache.FileCacheImpl
import com.marzec.todo.cache.MemoryCache
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.network.httpClient
import com.marzec.todo.screen.main.HomeScreen
import java.awt.Toolkit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

import java.awt.datatransfer.StringSelection

@ExperimentalCoroutinesApi
fun main() {

    DI.memoryCache = MemoryCache()

    DI.fileCache = FileCacheImpl(
        "todo.cache",
        Json,
        MemoryCache()
    )

    DI.copyToClipBoardHelper = object : CopyToClipBoardHelper {
        override fun copy(text: String) {
            Toolkit.getDefaultToolkit()
                .systemClipboard
                .setContents(StringSelection(text), null)
        }
    }

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

        HomeScreen(DI.navigationStore)
    }
}

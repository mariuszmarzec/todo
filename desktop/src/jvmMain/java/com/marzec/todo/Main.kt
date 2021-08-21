package com.marzec.todo

import androidx.compose.desktop.Window
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntSize
import com.marzec.todo.cache.FileCacheImpl
import com.marzec.todo.cache.MemoryCache
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.logger.Logger
import com.marzec.todo.network.httpClient
import com.marzec.todo.screen.main.HomeScreen
import java.awt.Desktop
import java.awt.Toolkit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

import java.awt.datatransfer.StringSelection
import java.net.URI

@ExperimentalCoroutinesApi
fun main() {

    DI.logger = object : Logger {
        override fun log(tag: String, message: String) {
            println("$tag: $message")
        }
    }

    DI.memoryCache = MemoryCache()

    DI.fileCache = FileCacheImpl(
        "todo.cache",
        Json {
             isLenient = true
        },
        MemoryCache()
    )

    DI.copyToClipBoardHelper = object : CopyToClipBoardHelper {
        override fun copy(text: String) {
            Toolkit.getDefaultToolkit()
                .systemClipboard
                .setContents(StringSelection(text), null)
        }
    }

    DI.openUrlHelper = object : OpenUrlHelper {
        override fun open(url: String) = Desktop.getDesktop().browse(URI(url))
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

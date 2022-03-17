package com.marzec.todo

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.marzec.cache.FileCacheImpl
import com.marzec.cache.MemoryCache
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.logger.Logger
import com.marzec.network.createHttpClient
import com.marzec.todo.screen.main.HomeScreen
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

@ExperimentalCoroutinesApi
fun main() {

    Logger.logger = object : Logger {
        override fun log(tag: String, message: String) {
            println("$tag: $message")
        }

        override fun log(tag: String, message: String, t: Throwable) {
            println("$tag: $message")
            t.printStackTrace()
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

    DI.client =
        createHttpClient(DI.fileCache, Api.Headers.AUTHORIZATION, PreferencesKeys.AUTHORIZATION)
    DI.ioDispatcher = Dispatchers.IO

    application {
        Window(
            ::exitApplication,
            title = "ToDo",
            state = rememberWindowState(
                position = WindowPosition(Alignment.Center),
                width = 700.dp,
                height = 768.dp
            )
        ) {
            DI.navigationScope = rememberCoroutineScope()

            DI.navigationStore = runBlocking {
                DI.provideNavigationStore(DI.navigationScope)
            }

            HomeScreen(DI.navigationStore)
        }
    }
}

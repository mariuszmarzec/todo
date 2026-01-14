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
import com.marzec.common.CopyToClipBoardHelper
import com.marzec.common.OpenUrlHelper
import com.marzec.logger.Logger
import com.marzec.logger.MultiLogger
import com.marzec.resource.ResourceLoaderImpl
import com.marzec.todo.repository.NoOpDeviceTokenRepositoryImpl
import com.marzec.todo.screen.main.HomeScreen
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

@ExperimentalCoroutinesApi
fun main() {

    val consoleLogger = object : Logger {
        override fun log(tag: String, message: String) {
            println("$tag: $message")
        }

        override fun log(tag: String, message: String, t: Throwable) {
            println("$tag: $message")
            t.printStackTrace()
        }
    }
    Logger.logger = MultiLogger(listOf(consoleLogger))

    DI.memoryCache = MemoryCache()
    DI.resultMemoryCache = MemoryCache()

    DI.fileCache = FileCacheImpl(
        "todo.cache",
        Json {
            isLenient = true
        },
        MemoryCache()
    )

    DI.deviceTokenRepository = NoOpDeviceTokenRepositoryImpl()

    DI.copyToClipBoardHelper = CopyToClipBoardHelper()

    DI.openUrlHelper = object : OpenUrlHelper {
        override fun open(url: String) = Desktop.getDesktop().browse(URI(url))
    }

    DI.resourceLoader = ResourceLoaderImpl()

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

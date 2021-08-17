package com.marzec.todo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.getSystemService
import androidx.datastore.preferences.createDataStore
import com.marzec.todo.cache.MemoryCache
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.screen.main.HomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.marzec.todo.network.httpClient
import com.marzec.todo.view.cache.PreferencesCache

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colors = lightColors()) {
                DI.memoryCache = MemoryCache()

                DI.fileCache = PreferencesCache(
                    createDataStore(name = "user_preferences")
                )

                DI.copyToClipBoardHelper = object : CopyToClipBoardHelper {
                    override fun copy(text: String) {
                        getSystemService<ClipboardManager>()
                            ?.setPrimaryClip(ClipData.newPlainText("label", text))
                    }
                }

                DI.openUrlHelper = object : OpenUrlHelper {
                    override fun open(url: String) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                            )
                        )
                    }
                }

                DI.client = httpClient
                DI.ioDispatcher = Dispatchers.IO

                DI.navigationStore = runBlocking {
                    DI.provideNavigationStore()
                }

                DI.navigationScope = rememberCoroutineScope()

                HomeScreen(DI.navigationStore)
            }
        }
    }
}
package com.marzec.todo

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.getSystemService
import androidx.datastore.preferences.createDataStore
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.logger.Logger
import com.marzec.todo.view.cache.PreferencesCache
import kotlinx.coroutines.Dispatchers
import com.marzec.todo.network.httpClient
import com.marzec.todo.cache.MemoryCache
import kotlinx.coroutines.runBlocking

class ToDoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        DI.logger = object : Logger {
            override fun log(tag: String, message: String) {
                Log.d(tag, message)
            }
        }

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
    }
}
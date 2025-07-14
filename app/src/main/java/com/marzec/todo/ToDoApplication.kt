package com.marzec.todo

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import androidx.core.content.getSystemService
import androidx.datastore.preferences.createDataStore
import com.marzec.common.CopyToClipBoardHelper
import com.marzec.logger.Logger
import com.marzec.cache.PreferencesCache
import kotlinx.coroutines.Dispatchers
import com.marzec.network.createHttpClient
import com.marzec.cache.MemoryCache
import com.marzec.resource.ResourceLoaderImpl
import kotlinx.serialization.json.Json

class ToDoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        DI.quickCacheEnabled = false

        Logger.logger = object : Logger {
            override fun log(tag: String, message: String) {
                Log.d(tag, message)
            }

            override fun log(tag: String, message: String, t: Throwable) {
                Log.e(tag, message, t)
            }
        }

        DI.resourceLoader = ResourceLoaderImpl(this)

        DI.memoryCache = MemoryCache()
        DI.resultMemoryCache = MemoryCache()

        DI.fileCache = PreferencesCache(
            createDataStore(name = "user_preferences"),
            Json
        )

        DI.copyToClipBoardHelper = CopyToClipBoardHelper(this)

        DI.ioDispatcher = Dispatchers.IO
    }
}

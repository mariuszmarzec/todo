package com.marzec.todo.repository

import com.marzec.cache.FileCache
import io.ktor.client.HttpClient

class DeviceTokenRepositoryImpl(
    fileCache: FileCache,
    client: HttpClient
) : DeviceTokenRepository {
    override suspend fun saveToken(token: String) {
        TODO("Not yet implemented")
    }

    override suspend fun removeCurrentToken() {
        TODO("Not yet implemented")
    }
}

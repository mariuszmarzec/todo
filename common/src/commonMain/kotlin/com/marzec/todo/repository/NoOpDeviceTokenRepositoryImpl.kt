package com.marzec.todo.repository

class NoOpDeviceTokenRepositoryImpl : DeviceTokenRepository {
    override suspend fun saveToken(token: String) = Unit

    override suspend fun removeCurrentToken() = Unit
}
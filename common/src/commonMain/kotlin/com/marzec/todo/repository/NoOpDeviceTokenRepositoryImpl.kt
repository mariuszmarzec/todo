package com.marzec.todo.repository

class NoOpDeviceTokenRepositoryImpl : DeviceTokenRepository {
    override suspend fun updateToken(token: String, isLogged: Boolean) = Unit

    override suspend fun removeCurrentToken() = Unit

    override suspend fun sendCurrentToken() = Unit
}
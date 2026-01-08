package com.marzec.todo.repository

interface DeviceTokenRepository {

    suspend fun updateToken(token: String, isLogged: Boolean)

    suspend fun removeCurrentToken()

    suspend fun sendCurrentToken()
}
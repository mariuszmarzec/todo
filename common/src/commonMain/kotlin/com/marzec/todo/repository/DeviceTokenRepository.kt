package com.marzec.todo.repository

interface DeviceTokenRepository {

    suspend fun saveToken(token: String)

    suspend fun removeCurrentToken()
}
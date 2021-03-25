package com.marzec.todo.repository

import com.marzec.todo.Api
import com.marzec.todo.DI
import com.marzec.todo.model.LoginRequestDto
import com.marzec.todo.model.User
import com.marzec.todo.network.Content
import com.marzec.todo.network.asContent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.withContext

class LoginRepository(private val client: HttpClient) {

    suspend fun login(login: String, password: String): Content<User> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.post<Unit>(Api.Login.LOGIN) {
                    contentType(ContentType.Application.Json)
                    body = LoginRequestDto(login, password)
                }
                client.get(Api.Login.USER)
            }
        }
}
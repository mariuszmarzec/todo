package com.marzec.todo.repository

import com.marzec.todo.Api
import com.marzec.todo.DI
import com.marzec.todo.api.LoginRequestDto
import com.marzec.todo.api.UserDto
import com.marzec.todo.api.toDomain
import com.marzec.todo.model.User
import com.marzec.todo.network.Content
import com.marzec.todo.network.asContent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.withContext

interface LoginRepository {

    suspend fun login(login: String, password: String): Content<User>
    suspend fun logout(): Content<Unit>
}

class LoginRepositoryMock : LoginRepository {
    override suspend fun login(login: String, password: String): Content<User> =
        Content.Data(User(1, "mock@user.com"))

    override suspend fun logout(): Content<Unit> = Content.Data(Unit)

}

class LoginRepositoryImpl(private val client: HttpClient) : LoginRepository {

    override suspend fun login(login: String, password: String): Content<User> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.post<UserDto>(Api.Login.LOGIN) {
                    contentType(ContentType.Application.Json)
                    body = LoginRequestDto(login, password)
                }.toDomain()
            }
        }

    override suspend fun logout(): Content<Unit> = withContext(DI.ioDispatcher) {
        asContent {
            client.get<Unit>(Api.Login.LOGOUT)
        }
    }
}
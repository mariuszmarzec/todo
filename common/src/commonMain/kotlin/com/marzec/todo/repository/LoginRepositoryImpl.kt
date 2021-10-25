package com.marzec.todo.repository

import com.marzec.todo.Api
import com.marzec.todo.PreferencesKeys
import com.marzec.todo.api.LoginRequestDto
import com.marzec.todo.api.UserDto
import com.marzec.todo.api.toDomain
import com.marzec.todo.cache.FileCache
import com.marzec.todo.cache.putTyped
import com.marzec.todo.model.User
import com.marzec.todo.network.Content
import com.marzec.todo.network.asContentFlow
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

interface LoginRepository {

    suspend fun login(login: String, password: String): Flow<Content<User>>
    suspend fun logout(): Flow<Content<Unit>>
}

class LoginRepositoryMock : LoginRepository {
    override suspend fun login(login: String, password: String): Flow<Content<User>> =
        flowOf(Content.Data(User(1, "mock@user.com")))

    override suspend fun logout(): Flow<Content<Unit>> = flowOf(Content.Data(Unit))

}

class LoginRepositoryImpl(
    private val client: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val fileCache: FileCache
) : LoginRepository {

    override suspend fun login(login: String, password: String): Flow<Content<User>> =
        asContentFlow {
            client.post<UserDto>(Api.Login.LOGIN) {
                contentType(ContentType.Application.Json)
                body = LoginRequestDto(login, password)
            }.toDomain()
        }.flowOn(dispatcher)

    override suspend fun logout(): Flow<Content<Unit>> =
        asContentFlow {
            client.get<Unit>(Api.Login.LOGOUT)
            fileCache.putTyped<String>(PreferencesKeys.AUTHORIZATION, null)
        }.flowOn(dispatcher)
}

package com.marzec.todo

import com.marzec.todo.model.LoginRequestDto
import com.marzec.todo.model.User
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking

fun main() {
    val client = DI.client

    val user = runBlocking {
        client.post<Unit>(Api.LOGIN) {
            contentType(ContentType.Application.Json)
            body = LoginRequestDto("mariusz.marzec00@gmail.com", "password")
        }
        client.get<User>(Api.USER)
    }
    println(user)
    exitProcess(0)
}
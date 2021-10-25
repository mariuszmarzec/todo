package com.marzec.todo.api

import com.marzec.todo.model.User
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(val id: Int, val email: String)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

fun UserDto.toDomain(): User = User(id, email)

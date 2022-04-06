package com.marzec.api

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post

open class ApiCrudDataSource<MODEL : Any, CREATE : Any, UPDATE : Any>(
    private val endpointAll: String,
    private val client: HttpClient
) {
    suspend fun remove(id: Int) {
        client.delete<Unit>("$endpointAll/$id")
    }

    suspend fun getAll() = client.get<List<MODEL>>(endpointAll)

    suspend fun create(createDto: CREATE) {
        client.post<Unit>(endpointAll) {
            body = createDto
        }
    }
    
    suspend fun updateTask(updateDto: UPDATE, id: Int) {
        client.patch<Unit>("endpointAll/$id") {
            body = updateDto
        }
    }
}

package com.marzec.repository

import com.marzec.api.ApiCrudDataSource
import com.marzec.cache.Cache
import com.marzec.cache.asContentWithListUpdate
import com.marzec.cache.cacheCall
import com.marzec.content.Content
import com.marzec.content.asContent
import com.marzec.content.ifDataSuspend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

open class CrudRepository<
        MODEL : Any,
        CREATE : Any,
        UPDATE : Any,
        MODEL_DTO : Any,
        CREATE_DTO : Any,
        UPDATE_DTO : Any
        >(
    private val dataSource: ApiCrudDataSource<MODEL_DTO, CREATE_DTO, UPDATE_DTO>,
    private val memoryCache: Cache,
    private val dispatcher: CoroutineDispatcher,
    private val cacheKey: String,
    private val toDomain: MODEL_DTO.() -> MODEL,
    private val updateToDto: UPDATE.() -> UPDATE_DTO,
    private val createToDto: CREATE.() -> CREATE_DTO
) {

    suspend fun observeAll(): Flow<Content<List<MODEL>>> = getTasksCacheFirst()

    suspend fun remove(id: Int): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.remove(id)
        }

    suspend fun update(id: Int, model: UPDATE): Flow<Content<Unit>> = asContentWithListUpdate { 
        dataSource.updateTask(model.updateToDto(), id)
    }

    suspend fun create(
        create: CREATE
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.create(create.createToDto())
        }

    suspend fun getTasksCacheFirst() =
        cacheCall(cacheKey) {
            asContent {
                dataSource.getAll()
                    .map { it.toDomain() }
            }
        }

    suspend fun refreshListsCache() = asContent {
        dataSource.getAll().map { it.toDomain() }
    }.ifDataSuspend {
        memoryCache.put(cacheKey, data)
    }

    suspend fun <T : Any> cacheCall(
        key: String,
        networkCall: suspend () -> Content<T>
    ): Flow<Content<T>> = cacheCall(key, dispatcher, memoryCache, networkCall)

    fun asContentWithListUpdate(
        request: suspend () -> Unit
    ) = asContentWithListUpdate(dispatcher, request) {
        refreshListsCache()
    }
}

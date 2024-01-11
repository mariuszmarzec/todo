package com.marzec.repository

import com.marzec.cache.Cache
import com.marzec.cache.asContentWithListUpdate
import com.marzec.cache.cacheCall
import com.marzec.content.Content
import com.marzec.content.asContent
import com.marzec.content.asContentFlow
import com.marzec.content.ifDataSuspend
import com.marzec.datasource.CommonDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class CrudRepository<
        ID,
        MODEL : Any,
        CREATE : Any,
        UPDATE : Any,
        MODEL_DTO : Any,
        CREATE_DTO : Any,
        UPDATE_DTO : Any
        >(
    private val dataSource: CommonDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>,
    private val memoryCache: Cache,
    private val dispatcher: CoroutineDispatcher,
    private val cacheKey: String,
    private val toDomain: MODEL_DTO.() -> MODEL,
    private val updateToDto: UPDATE.() -> UPDATE_DTO,
    private val createToDto: CREATE.() -> CREATE_DTO,
    private val isSameId: MODEL.(id: ID) -> Boolean,
) {

    suspend fun observeAll(): Flow<Content<List<MODEL>>> = getTasksCacheFirst()

    suspend fun getById(id: ID): Flow<Content<MODEL>> =
        byIdCall(id) {
            asContent { dataSource.getById(id).toDomain() }
        }

    private suspend fun byIdCall(
        id: ID,
        getCached: suspend () -> MODEL? = {
            memoryCache.get<List<MODEL>>(cacheKey)?.firstOrNull { it.isSameId(id) }
        },
        observeCached: suspend () -> Flow<MODEL?> = {
            memoryCache.observe<List<MODEL>>(cacheKey)
                .map { list -> list?.firstOrNull { it.isSameId(id) } }
        },
        networkCall: suspend () -> Content<MODEL>,
    ) = cacheCall(
        cacheKey,
        dispatcher,
        memoryCache,
        networkCall = networkCall,
        getCached = getCached,
        observeCached = observeCached,
        cacheUpdate = { newCallData ->
            updateCache(id, newCallData.data)
        }
    )

    private suspend fun updateCache(id: ID, data: MODEL) = updateCache(id) { old ->
        old?.map {
            if (it.isSameId(id)) {
                data
            } else {
                it
            }
        } ?: listOf(data)
    }

    private suspend fun updateCache(id: ID, update: (List<MODEL>?) -> List<MODEL>?) {
        val oldValue = memoryCache.get<List<MODEL>>(cacheKey)
        val newValue = update(oldValue)
        memoryCache.put(cacheKey, newValue)
    }

    suspend fun remove(id: ID): Flow<Content<Unit>> = asContentFlow {
        dataSource.remove(id)
        updateCache(id) { cachedList ->
            cachedList?.toMutableList()?.apply {
                removeIf { it.isSameId(id) }
            }
        }
    }

    suspend fun update(id: ID, model: UPDATE): Flow<Content<Unit>> = asContentFlow {
        val updatedModel = dataSource.update(id, model.updateToDto()).toDomain()
        updateCache(id, updatedModel)
    }.flowOn(dispatcher)

    suspend fun create(
        create: CREATE
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.create(create.createToDto())
        }

    private suspend fun getTasksCacheFirst() =
        cacheCall(cacheKey) {
            asContent { dataSource.getAll().map(toDomain) }
        }

    private suspend fun refreshListsCache() = asContent {
        dataSource.getAll().map { it.toDomain() }
    }.ifDataSuspend {
        memoryCache.put(cacheKey, data)
    }

    private suspend fun <T : Any> cacheCall(
        key: String,
        networkCall: suspend () -> Content<T>
    ): Flow<Content<T>> = cacheCall(key, dispatcher, memoryCache, networkCall)

    private fun asContentWithListUpdate(
        request: suspend () -> Unit
    ) = asContentWithListUpdate(dispatcher, request) {
        refreshListsCache()
    }
}

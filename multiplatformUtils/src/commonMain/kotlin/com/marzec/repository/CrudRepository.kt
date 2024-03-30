package com.marzec.repository

import com.marzec.cache.CacheSaver
import com.marzec.cache.GetWithCacheCall
import com.marzec.cache.ManyItemsCacheSaver
import com.marzec.content.Content
import com.marzec.content.asContent
import com.marzec.content.asContentFlow
import com.marzec.content.ifDataSuspend
import com.marzec.datasource.CommonDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CrudRepository<ID, MODEL : Any, CREATE : Any, UPDATE : Any, MODEL_DTO : Any, CREATE_DTO : Any, UPDATE_DTO : Any>(
    private val dataSource: CommonDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>,
    private val dispatcher: CoroutineDispatcher,
    private val cacheSaver: ManyItemsCacheSaver<ID, MODEL>,
    private val toDomain: MODEL_DTO.() -> MODEL,
    private val updateToDto: UPDATE.() -> UPDATE_DTO,
    private val createToDto: CREATE.() -> CREATE_DTO,
    private val updaterCoroutineScope: CoroutineScope
) {
    enum class RefreshPolicy {
        NO_REFRESH, SEPARATE_DISPATCHER, BLOCKING
    }

    suspend fun observeAll(): Flow<Content<List<MODEL>>> =
        GetWithCacheCall(
            dispatcher = dispatcher,
            cacheSaver = cacheSaver,
            call = {
                asContent { loadAll() }
            }
        ).run()

    suspend fun observeById(id: ID): Flow<Content<MODEL>> = byIdCall(id) {
        asContent { dataSource.getById(id).toDomain() }
    }

    private suspend fun byIdCall(
        id: ID,
        networkCall: suspend () -> Content<MODEL>,
    ): Flow<Content<MODEL>> = GetWithCacheCall(
            dispatcher = dispatcher,
            cacheSaver = object : CacheSaver<MODEL> {
                override suspend fun get(): MODEL? {
                    return cacheSaver.getById(id)
                }

                override suspend fun observeCached(): Flow<MODEL?> {
                    return cacheSaver.observeCachedById(id)
                }

                override suspend fun updateCache(update: (MODEL?) -> MODEL?) = Unit

                override suspend fun updateCache(data: MODEL) {
                    cacheSaver.updateItem(id, data)
                }

            },
            call = networkCall
        ).run()

    suspend fun remove(
        id: ID, policy: RefreshPolicy = RefreshPolicy.SEPARATE_DISPATCHER
    ): Flow<Content<Unit>> = asContentFlow {
        dataSource.remove(id)
        cacheSaver.removeItem(id)
    }.triggerUpdateIfNeeded(policy).flowOn(dispatcher)

    suspend fun update(
        id: ID, model: UPDATE, policy: RefreshPolicy = RefreshPolicy.SEPARATE_DISPATCHER
    ): Flow<Content<Unit>> = asContentFlow {
        val updatedModel = dataSource.update(id, model.updateToDto()).toDomain()
        cacheSaver.updateItem(id, updatedModel)
    }.triggerUpdateIfNeeded(policy).flowOn(dispatcher)

    suspend fun create(
        create: CREATE, policy: RefreshPolicy = RefreshPolicy.SEPARATE_DISPATCHER
    ): Flow<Content<MODEL>> = asContentFlow {
        val createdModel = dataSource.create(create.createToDto()).toDomain()
        cacheSaver.addItem(createdModel)
        createdModel
    }.triggerUpdateIfNeeded(policy).flowOn(dispatcher)

    private suspend fun <T> Flow<Content<T>>.triggerUpdateIfNeeded(policy: RefreshPolicy): Flow<Content<T>> =
        onEach {
            if (it is Content.Data) {
                when (policy) {
                    RefreshPolicy.NO_REFRESH -> Unit
                    RefreshPolicy.SEPARATE_DISPATCHER -> updaterCoroutineScope.launch {
                        refreshAll()
                    }

                    RefreshPolicy.BLOCKING -> refreshAll()
                }
            }
        }

    private suspend fun refreshAll() = asContent {
        loadAll()
    }.ifDataSuspend {
        cacheSaver.updateCache(data)
    }

    private suspend fun loadAll() = dataSource.getAll().map(toDomain)
}

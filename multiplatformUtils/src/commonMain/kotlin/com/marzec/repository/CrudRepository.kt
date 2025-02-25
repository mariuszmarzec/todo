package com.marzec.repository

import com.marzec.cache.Cache
import com.marzec.cache.CacheSaver
import com.marzec.cache.CompositeUpdatableCacheSaver
import com.marzec.cache.FileCache
import com.marzec.cache.FileCacheSaver
import com.marzec.cache.GetWithCacheCall
import com.marzec.cache.ListCacheSaver
import com.marzec.cache.ManyItemsCacheSaver
import com.marzec.cache.MemoryCacheSaver
import com.marzec.cache.atFirstPositionInserter
import com.marzec.cache.map
import com.marzec.cache.synchronized
import com.marzec.cache.toReversed
import com.marzec.cache.withInserter
import com.marzec.content.Content
import com.marzec.content.asContent
import com.marzec.content.asContentFlow
import com.marzec.content.ifDataSuspend
import com.marzec.datasource.CrudDataSource
import com.marzec.datasource.CrudDataSourceImpl
import com.marzec.datasource.EndpointProviderImpl
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.coroutines.coroutineContext
import kotlin.reflect.typeOf

class CrudRepository<ID, MODEL : Any, CREATE : Any, UPDATE : Any, MODEL_DTO : Any, CREATE_DTO : Any, UPDATE_DTO : Any>(
    private val dataSource: CrudDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>,
    private val dispatcher: CoroutineDispatcher,
    private val cacheSaver: ManyItemsCacheSaver<ID, MODEL>,
    private val toDomain: MODEL_DTO.() -> MODEL,
    private val updateToDto: UPDATE.() -> UPDATE_DTO,
    private val createToDto: CREATE.() -> CREATE_DTO,
    private val updaterCoroutineScope: CoroutineScope,
    private val onUpdate: List<suspend () -> Unit> = emptyList()
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

            override suspend fun saveCache(data: MODEL) {
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
    ): Flow<Content<MODEL>> = asContentFlow {
        val updatedModel = dataSource.update(id, model.updateToDto()).toDomain()
        cacheSaver.updateItem(id, updatedModel)
        updatedModel
    }.triggerUpdateIfNeeded(policy).flowOn(dispatcher)

    suspend fun create(
        create: CREATE, policy: RefreshPolicy = RefreshPolicy.SEPARATE_DISPATCHER
    ): Flow<Content<MODEL>> = asContentFlow {
        val createdModel = dataSource.create(create.createToDto()).toDomain()
        cacheSaver.addItem(createdModel)
        createdModel
    }.triggerUpdateIfNeeded(policy).flowOn(dispatcher)

    suspend fun <T> Flow<Content<T>>.triggerUpdateIfNeeded(policy: RefreshPolicy): Flow<Content<T>> =
        onEach {
            if (it is Content.Data) {
                when (policy) {
                    RefreshPolicy.NO_REFRESH -> Unit
                    RefreshPolicy.SEPARATE_DISPATCHER -> updaterCoroutineScope.launch {
                        runRefreshing()
                    }

                    RefreshPolicy.BLOCKING -> runRefreshing()
                }
            }
        }

    private suspend fun runRefreshing() = with(CoroutineScope(SupervisorJob() + coroutineContext)) {
        val updaters = listOf<suspend () -> Unit> { refreshCache() } + onUpdate

        updaters.forEach {
            launch {
                it()
            }
        }
    }

    suspend fun refreshCache() = asContent {
        loadAll()
    }.ifDataSuspend {
        cacheSaver.saveCache(data)
    }



    private suspend fun loadAll() = dataSource.getAll().map(toDomain)
}

inline fun <
        ID,
        reified MODEL : Any,
        CREATE : Any,
        UPDATE : Any,
        reified MODEL_DTO : Any,
        reified CREATE_DTO : Any,
        reified UPDATE_DTO : Any
        > fileAndMemoryCacheCrudRepository(
    dispatcher: CoroutineDispatcher,
    client: HttpClient,
    updaterCoroutineScope: CoroutineScope,
    endpoint: String,
    fileCache: FileCache,
    memoryCache: Cache,
    reversed: Boolean = false,
    noinline isSameId: MODEL.(id: ID) -> Boolean,
    noinline toDomain: MODEL_DTO.() -> MODEL,
    noinline toDto: MODEL.() -> MODEL_DTO,
    noinline createToDto: CREATE.() -> CREATE_DTO,
    noinline updateToDto: UPDATE.() -> UPDATE_DTO,
    noinline inserter: List<MODEL>?.(MODEL) -> MutableList<MODEL> = atFirstPositionInserter(),
    dataSource: CrudDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>? = null
): CrudRepository<ID, MODEL, CREATE, UPDATE, MODEL_DTO, CREATE_DTO, UPDATE_DTO> =
    CrudRepository(
        dataSource = dataSource ?: createDataSource<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO>(
            endpoint,
            client
        ),
        cacheSaver = ListCacheSaver(
            cacheSaver = CompositeUpdatableCacheSaver(
                savers = listOf(
                    MemoryCacheSaver(
                        key = endpoint,
                        memoryCache = memoryCache
                    ),
                    FileCacheSaver(
                        key = endpoint,
                        fileCache = fileCache,
                        serializer = serializer(typeOf<List<MODEL_DTO>>()) as KSerializer<List<MODEL_DTO>>
                    ).map(
                        toNested = { map { it.toDto() } },
                        toTarget = { map { it.toDomain() } }
                    )
                )
            ),
            isSameId = isSameId
        ).withInserter(inserter)
            .let {
                if (reversed) {
                    it.toReversed()
                } else {
                    it
                }
            }.synchronized(),
        dispatcher = dispatcher,
        toDomain = toDomain,
        createToDto = createToDto,
        updateToDto = updateToDto,
        updaterCoroutineScope = updaterCoroutineScope
    )

inline fun <ID, reified MODEL_DTO : Any, reified CREATE_DTO : Any, reified UPDATE_DTO : Any> createDataSource(
    endpoint: String,
    client: HttpClient
): CrudDataSourceImpl<ID, MODEL_DTO, CREATE_DTO, UPDATE_DTO> =
    CrudDataSource(
        endpointProvider = EndpointProviderImpl(endpoint),
        client = client,
        json = Json
    )

package com.marzec.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.cache.Cache
import com.marzec.cache.FileCache
import com.marzec.cache.MemoryCache
import com.marzec.common.CopyToClipBoardHelper
import com.marzec.common.OpenUrlHelper
import com.marzec.datasource.CrudDataSource
import com.marzec.datasource.EndpointProviderImpl
import com.marzec.delegate.DialogDelegateImpl
import com.marzec.delegate.ScrollDelegateImpl
import com.marzec.delegate.SearchDelegateImpl
import com.marzec.delegate.SelectionDelegateImpl
import com.marzec.dto.FeatureToggleDto
import com.marzec.dto.NewFeatureToggleDto
import com.marzec.dto.UpdateFeatureToggleDto
import com.marzec.dto.toDomain
import com.marzec.featuretoggle.FeatureTogglesManagerInitializerImpl
import com.marzec.logger.Logger
import com.marzec.model.FeatureToggle
import com.marzec.model.NewFeatureToggle
import com.marzec.model.UpdateFeatureToggle
import com.marzec.model.toDto
import com.marzec.mvi.Store
import com.marzec.mvi.toCachable
import com.marzec.navigation.Destination
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationEntryCache
import com.marzec.navigation.NavigationFlow
import com.marzec.navigation.NavigationState
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.MemoryStateCache
import com.marzec.preferences.StateCache
import com.marzec.repository.LoginRepository
import com.marzec.repository.LoginRepositoryImpl
import com.marzec.repository.LoginRepositoryMock
import com.marzec.repository.fileAndMemoryCacheCrudRepository
import com.marzec.resource.ResourceLoader
import com.marzec.screen.featuretoggle.FeatureToggleDetails
import com.marzec.screen.featuretoggle.FeatureToggleScreen
import com.marzec.screen.featuretoggle.FeatureToggleState
import com.marzec.screen.featuretoggle.FeatureToggleStore
import com.marzec.screen.featuretoggle.FeatureToggles
import com.marzec.screen.featuretoggle.featureTogglePickOptions
import com.marzec.screen.featuretoggle.provideFeatureTogglesScreen
import com.marzec.screen.pickitemscreen.PickItemDataStore
import com.marzec.screen.pickitemscreen.PickItemOptions
import com.marzec.screen.pickitemscreen.PickItemScreen
import com.marzec.screen.pickitemscreen.providePickItemStore
import com.marzec.todo.Api.Todo.FEATURE_TOGGLES
import com.marzec.todo.delegates.dialog.ChangePriorityDelegateImpl
import com.marzec.todo.delegates.dialog.RemoveTaskDelegateImpl
import com.marzec.todo.delegates.dialog.UrlDelegateImpl
import com.marzec.todo.delegates.reorder.ReorderDelegateImpl
import com.marzec.todo.navigation.TodoDestination
import com.marzec.todo.network.ApiDataSource
import com.marzec.todo.network.CommonTodoDataSource
import com.marzec.todo.network.CompositeDataSource
import com.marzec.todo.network.DataSource
import com.marzec.todo.network.LocalDataSource
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.screen.addnewtask.AddNewTaskScreen
import com.marzec.todo.screen.addnewtask.model.AddNewTaskState
import com.marzec.todo.screen.addnewtask.model.AddNewTaskStore
import com.marzec.todo.screen.addsubtask.AddSubTaskScreen
import com.marzec.todo.screen.addsubtask.model.AddSubTaskData
import com.marzec.todo.screen.addsubtask.model.AddSubTaskStore
import com.marzec.todo.screen.login.LoginScreen
import com.marzec.todo.screen.login.model.LoginData
import com.marzec.todo.screen.login.model.LoginStore
import com.marzec.todo.screen.scheduler.SchedulerScreen
import com.marzec.todo.screen.scheduler.SchedulerState
import com.marzec.todo.screen.scheduler.SchedulerStore
import com.marzec.todo.screen.taskdetails.TaskDetailsScreen
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.screen.tasks.TasksScreen
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.view.ActionBarProvider
import com.marzec.view.DateDelegateImpl
import com.marzec.view.DatePickerScreen
import com.marzec.view.DatePickerState
import com.marzec.view.DatePickerStore
import com.marzec.view.initialState
import com.marzec.view.navigationStore
import io.ktor.client.HttpClient
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object DI {

    private val isJustLocalStorageMode: Boolean
        get() = BuildKonfig.ENVIRONMENT == "m"

    val logger: Logger by lazy {
        Logger.logger
    }
    lateinit var copyToClipBoardHelper: CopyToClipBoardHelper
    lateinit var openUrlHelper: OpenUrlHelper
    var ioDispatcher: CoroutineDispatcher = Dispatchers.Default
    lateinit var navigationScope: CoroutineScope

    lateinit var memoryCache: Cache
    lateinit var resultMemoryCache: Cache

    lateinit var resourceLoader: ResourceLoader

    lateinit var fileCache: FileCache
    var quickCacheEnabled: Boolean = false

    val stateCache: StateCache = MemoryStateCache()

    val updaterCoroutineScope = CoroutineScope(newSingleThreadContext("updater"))

    val featureToggleRepository by lazy {
        fileAndMemoryCacheCrudRepository<Int, FeatureToggle, NewFeatureToggle, UpdateFeatureToggle, FeatureToggleDto, NewFeatureToggleDto, UpdateFeatureToggleDto>(
            dispatcher = ioDispatcher,
            client = client,
            updaterCoroutineScope = updaterCoroutineScope,
            endpoint = FEATURE_TOGGLES,
            fileCache = fileCache,
            memoryCache = memoryCache,
            isSameId = { it == id },
            toDomain = { toDomain() },
            toDto = { toDto() },
            createToDto = { toDto() },
            updateToDto = { toDto() },
        )
    }
    val navigationStoreCacheKey by lazy { cacheKeyProvider.invoke() }

    val scrollStateCache: Cache by lazy {
        NavigationEntryCache(navigationStore, MemoryCache())
    }
    val listScrollStateCache: Cache by lazy {
        NavigationEntryCache(navigationStore, MemoryCache())
    }


    val featureTogglesManagerInitializer by lazy {
        FeatureTogglesManagerInitializerImpl(
            resourceLoader,
                    Json,
                    featureToggleRepository,
                    memoryCache,
                    updaterCoroutineScope,
                    "feature_toggle_conf.json"
        )
    }

    val featureTogglesManager by lazy { featureTogglesManagerInitializer.create() }

    fun router(
        destination: Destination
    ): @Composable (destination: Destination, cacheKey: String) -> Unit =
        when (destination) {
            is FeatureToggles -> @Composable { destination, cacheKey ->
                destination as FeatureToggles
                provideFeatureTogglesScreen(
                    provideActionBarProvider(),
                    destination,
                    cacheKey,
                    navigationStore,
                    stateCache
                )
            }

            is FeatureToggleDetails -> @Composable { destination, cacheKey ->
                destination as FeatureToggleDetails
                provideFeatureToggleScreen(destination, cacheKey)
            }

            is TodoDestination.AddNewTask -> @Composable { destination, cacheKey ->
                destination as TodoDestination.AddNewTask
                provideAddNewTaskScreen(
                    taskId = destination.taskToEditId,
                    parentTaskId = destination.parentTaskId,
                    cacheKey
                )
            }

            is TodoDestination.AddSubTask -> @Composable { destination, cacheKey ->
                destination as TodoDestination.AddSubTask
                provideAddSubTaskScreen(destination.taskId, cacheKey)
            }

            is TodoDestination.DatePicker -> @Composable { destination, cacheKey ->
                destination as TodoDestination.DatePicker
                provideDatePickerScreen(cacheKey, destination.date)
            }

            TodoDestination.Login -> @Composable { _, cacheKey ->
                provideLoginScreen(cacheKey)
            }

            is TodoDestination.PickItem<*> -> @Composable { destination, cacheKey ->
                destination as TodoDestination.PickItem<Any>
                providePickItemScreen(destination, cacheKey)
            }

            is TodoDestination.Schedule -> @Composable { destination, cacheKey ->
                destination as TodoDestination.Schedule
                provideSchedulerScreen(cacheKey, destination)
            }

            is TodoDestination.TaskDetails -> @Composable { destination, cacheKey ->
                destination as TodoDestination.TaskDetails
                provideTaskDetailsScreen(destination.taskId, cacheKey)
            }

            TodoDestination.Tasks -> @Composable { _, cacheKey ->
                provideTasksScreen(cacheKey)
            }

            else -> {
                throw error("Unknown destination: $destination")
            }
        }

    @Composable
    private fun provideFeatureToggleScreen(destination: FeatureToggleDetails, cacheKey: String) {
        FeatureToggleScreen(
            store = FeatureToggleStore(
                rememberCoroutineScope(),
                args = destination,
                navigationStore = navigationStore,
                cacheKey = cacheKey,
                stateCache = stateCache,
                initialState = FeatureToggleState.initial(destination.id, destination.name, destination.value),
                repository = featureToggleRepository,
                dialogDelegate = DialogDelegateImpl<Int, FeatureToggleState>()
            ),
            actionBarProvider = provideActionBarProvider()
        )
    }

    @Composable
    private fun provideTasksScreen(cacheKey: String) {
        TasksScreen(
            store = provideTasksStore(
                scope = rememberCoroutineScope(),
                cacheKey = cacheKey
            ),
            actionBarProvider = provideActionBarProvider()
        )
    }

    private fun provideTasksStore(
        scope: CoroutineScope,
        cacheKey: String
    ): TasksStore {
        val changePriorityDelegate = ChangePriorityDelegateImpl<TasksScreenState>(
            provideTodoRepository()
        )
        return TasksStore(
            navigationStore = navigationStore,
            todoRepository = provideTodoRepository(),
            loginRepository = loginRepository,
            stateCache = stateCache,
            cacheKey = cacheKey,
            urlDelegate = UrlDelegateImpl<TasksScreenState>(openUrlHelper),
            dialogDelegate = DialogDelegateImpl<Int, TasksScreenState>(),
            removeTaskDelegate = RemoveTaskDelegateImpl<TasksScreenState>(
                provideTodoRepository()
            ),
            changePriorityDelegate = changePriorityDelegate,
            searchDelegate = SearchDelegateImpl<TasksScreenState>(),
            scheduledOptions = provideScheduledOptions(),
            selectionDelegate = SelectionDelegateImpl<Int, TasksScreenState>(),
            reorderDelegate = ReorderDelegateImpl<TasksScreenState>(
                changePriorityDelegate = changePriorityDelegate,
                tasksToReorder = { tasks }
            ) {
                copy(reorderMode = it)
            },
            featureTogglesManager = featureTogglesManager,
            store = Store(
                scope = scope,
                defaultState = TasksScreenState.initial()
            ).toCachable(
                cacheKey = cacheKey,
                stateCache = stateCache
            )
        )
    }

    @Composable
    private fun provideAddNewTaskScreen(
        taskId: Int?,
        parentTaskId: Int?,
        cacheKey: String
    ) {
        AddNewTaskScreen(
            provideAddNewTaskStore(
                scope = rememberCoroutineScope(),
                taskId = taskId,
                parentTaskId = parentTaskId,
                cacheKey = cacheKey
            ),
            actionBarProvider = provideActionBarProvider(),
        )
    }

    private fun provideAddNewTaskStore(
        scope: CoroutineScope,
        taskId: Int?,
        parentTaskId: Int?,
        cacheKey: String
    ): AddNewTaskStore = AddNewTaskStore(
        scope = scope,
        navigationStore = navigationStore,
        cacheKey = cacheKey,
        stateCache = stateCache,
        initialState = AddNewTaskState.initial(
            taskId = taskId,
            parentTaskId = parentTaskId
        ),
        todoRepository = provideTodoRepository(),
    )

    @Composable
    private fun provideTaskDetailsScreen(taskId: Int, cacheKey: String) {
        TaskDetailsScreen(
            store = provideTaskDetailsStore(
                scope = rememberCoroutineScope(),
                taskId = taskId,
                cacheKey = cacheKey
            ),
            actionBarProvider = provideActionBarProvider()
        )
    }

    private fun provideTaskDetailsStore(
        scope: CoroutineScope,
        taskId: Int,
        cacheKey: String
    ): TaskDetailsStore {
        val changePriorityDelegate = ChangePriorityDelegateImpl<TaskDetailsState>(
            provideTodoRepository()
        )
        return TaskDetailsStore(
            scope = scope,
            navigationStore = navigationStore,
            todoRepository = provideTodoRepository(),
            stateCache = stateCache,
            cacheKey = cacheKey,
            initialState = TaskDetailsState.INITIAL,
            taskId = taskId,
            copyToClipBoardHelper = copyToClipBoardHelper,
            dialogDelegate = DialogDelegateImpl<Int, TaskDetailsState>(),
            removeTaskDelegate = RemoveTaskDelegateImpl<TaskDetailsState>(provideTodoRepository()),
            urlDelegate = UrlDelegateImpl<TaskDetailsState>(openUrlHelper),
            changePriorityDelegate = changePriorityDelegate,
            selectionDelegate = SelectionDelegateImpl<Int, TaskDetailsState>(),
            searchDelegate = SearchDelegateImpl<TaskDetailsState>(),
            reorderDelegate = ReorderDelegateImpl<TaskDetailsState>(
                changePriorityDelegate = changePriorityDelegate,
                tasksToReorder = { task.subTasks }
            ) {
                copy(reorderMode = it)
            }
        )
    }

    @Composable
    private fun provideAddSubTaskScreen(taskId: Int, cacheKey: String) {
        AddSubTaskScreen(
            store = provideAddSubTaskStore(
                scope = rememberCoroutineScope(),
                taskId = taskId,
                cacheKey = cacheKey
            ),
            actionBarProvider = provideActionBarProvider()
        )
    }

    private fun provideAddSubTaskStore(
        scope: CoroutineScope,
        taskId: Int,
        cacheKey: String
    ) = AddSubTaskStore(
        scope = scope,
        navigationStore = navigationStore,
        todoRepository = provideTodoRepository(),
        stateCache = stateCache,
        cacheKey = cacheKey,
        initialState = AddSubTaskData.INITIAL,
        taskId = taskId,
        selectionDelegate = SelectionDelegateImpl<Int, AddSubTaskData>(),
        searchDelegate = SearchDelegateImpl<AddSubTaskData>()
    )

    @Composable
    private fun provideSchedulerScreen(cacheKey: String, destination: TodoDestination.Schedule) {
        SchedulerScreen(
            store = provideSchedulerStore(
                scope = rememberCoroutineScope(),
                cacheKey = cacheKey,
                destination = destination
            ),
            actionBarProvider = provideActionBarProvider()
        )
    }

    private fun provideSchedulerStore(
        scope: CoroutineScope,
        cacheKey: String,
        destination: TodoDestination.Schedule
    ): SchedulerStore {
        return SchedulerStore(
            scope = scope,
            navigationStore = navigationStore,
            stateCache = stateCache,
            cacheKey = cacheKey,
            initialState = SchedulerState.from(destination.scheduler, destination.additionalOptionsAvailable),
            dateDelegate = DateDelegateImpl<SchedulerState>(
                navigationStore = navigationStore,
                datePickerDestinationFactory = { TodoDestination.DatePicker(it) }
            )
        )
    }

    @Composable
    private fun provideDatePickerScreen(cacheKey: String, date: LocalDateTime?) {
        DatePickerScreen(
            store = provideDatePickerStore(
                scope = rememberCoroutineScope(),
                cacheKey = cacheKey,
                date = date
            ),
            actionBarProvider = provideActionBarProvider()
        )
    }

    private fun provideDatePickerStore(
        scope: CoroutineScope,
        cacheKey: String,
        date: LocalDateTime?
    ): DatePickerStore {
        return DatePickerStore(
            scope = scope,
            navigationStore = navigationStore,
            stateCache = stateCache,
            cacheKey = cacheKey,
            initialState = DatePickerState.from(date, blockPastDates = true)
        )
    }

    private val cacheKeyProvider by lazy {
        { Random.nextInt(Int.MAX_VALUE).toString() }
    }

    lateinit var navigationStore: NavigationStore

    fun provideNavigationStore(
        scope: CoroutineScope
    ): NavigationStore {
        val defaultScreen = getDefaultNavigationState()
        return navigationStore(
            scope = scope,
            stateCache = stateCache,
            cacheKeyProvider = cacheKeyProvider,
            navigationStoreCacheKey = navigationStoreCacheKey,
            defaultDestination = defaultScreen,
            onAfterClosed = {
                runBlocking {
                    scrollStateCache.remove(it.cacheKey)
                    listScrollStateCache.remove(it.cacheKey)
                }
            }
        )
    }

    fun provideNavigationStore(
        scope: CoroutineScope,
        initialState: NavigationFlow = navigationInitialState(),
        onNewStateCallback: ((NavigationState) -> Unit)? = null
    ): NavigationStore {
        return navigationStore(
            scope = scope,
            stateCache = stateCache,
            cacheKeyProvider = cacheKeyProvider,
            navigationStoreCacheKey = navigationStoreCacheKey,
            initialState = initialState
        )
    }

    fun navigationInitialState() = initialState(getDefaultNavigationState(), cacheKeyProvider)

    private fun getDefaultNavigationState(): TodoDestination {
        val authToken = runBlocking {
            fileCache.get(PreferencesKeys.AUTHORIZATION, String.serializer())
        }

        val defaultScreen = if (!authToken.isNullOrEmpty() || isJustLocalStorageMode) {
            TodoDestination.Tasks
        } else {
            TodoDestination.Login
        }
        return defaultScreen
    }

    @Composable
    private fun provideLoginScreen(cacheKey: String) =
        LoginScreen(
            actionBarProvider = provideActionBarProvider(),
            store = provideLoginStore(
                scope = rememberCoroutineScope(),
                cacheKey = cacheKey,
                featureTogglePickOptions(
                    featureToggleRepository,
                    navigationStore
                )

            )
        )

    lateinit var client: HttpClient

    val loginRepository: LoginRepository by lazy {
        if (BuildKonfig.ENVIRONMENT == "m") LoginRepositoryMock() else LoginRepositoryImpl(
            client,
            ioDispatcher,
            fileCache,
            Api.Login.LOGIN,
            Api.Login.LOGOUT,
            PreferencesKeys.AUTHORIZATION
        )
    }

    fun provideLoginStore(
        scope: CoroutineScope,
        cacheKey: String,
        featureTogglePickOptions: PickItemOptions<FeatureToggle>
    ): LoginStore = LoginStore(
        scope = scope,
        navigationStore = navigationStore,
        loginRepository = loginRepository,
        stateCache = stateCache,
        cacheKey = cacheKey,
        initialState = LoginData.INITIAL,
        featureTogglePickOptions = featureTogglePickOptions
    )

    val localDataSource by lazy {
        LocalDataSource(fileCache).apply { runBlocking { init() } }
    }

    private fun provideTodoRepository() = TodoRepository(
        dataSource = provideDataSource(),
        memoryCache = memoryCache,
        dispatcher = ioDispatcher
    )

    private fun provideDataSource(): DataSource {
        return if (isJustLocalStorageMode) {
            localDataSource
        } else if (quickCacheEnabled) {
            CompositeDataSource(
                localDataSource,
                ApiDataSource(client, provideCommonDataSource()),
                memoryCache,
            ).apply { runBlocking { init() } }
        } else {
            ApiDataSource(client, provideCommonDataSource())
        }
    }

    private fun provideCommonDataSource(): CommonTodoDataSource = CrudDataSource(
        EndpointProviderImpl(
            Api.Todo.TASKS
        ), client, Json
    )

    @Composable
    private fun <ITEM : Any> providePickItemScreen(
        destination: TodoDestination.PickItem<ITEM>,
        cacheKey: String
    ) =
        PickItemScreen(
            options = destination.options,
            store = providePickItemStore(
                destination.options,
                scope = rememberCoroutineScope(),
                cacheKey = cacheKey,
            ),
            actionBarProvider = provideActionBarProvider()
        )

    private fun <ITEM : Any> providePickItemStore(
        options: PickItemOptions<ITEM>,
        scope: CoroutineScope,
        cacheKey: String
    ): PickItemDataStore<ITEM> = providePickItemStore(
        options = options,
        scope = scope,
        navigationStore = navigationStore,
        stateCache = stateCache,
        cacheKey = cacheKey
    )

    private fun provideScheduledOptions() = PickItemOptions(
        loadData = { provideTodoRepository().observeScheduledTasks() },
        mapItemToId = { it.id.toString() },
        itemRow = { item, _ ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable {
                        navigationStore.next(NavigationAction(TodoDestination.TaskDetails(item.id)))
                    }
                    .padding(16.dp)
            ) {
                Text(item.description)
            }
        },
        stringsToCompare = { listOf(it.description) },
        onAddNavigationAction = {
            NavigationAction(TodoDestination.AddNewTask(taskToEditId = null, parentTaskId = null))
        }
    )

    private fun provideActionBarProvider() = ActionBarProvider(navigationStore)
}

object PreferencesKeys {
    const val AUTHORIZATION = "AUTHORIZATION"
}

object Api {

    val HOST = if (BuildKonfig.ENVIRONMENT == "p") {
        BuildKonfig.PROD_API_URL
    } else {
        BuildKonfig.TEST_API_URL
    }

    @Suppress("MemberNameEqualsClassName")
    object Login {
        val BASE = "$HOST/fiteo/api/1"

        val LOGIN = "$BASE/login"
        val LOGOUT = "$BASE/logout"
    }

    object Todo {
        val CURRENT_API_VERSION = "1"
        val BASE = "$HOST/todo/api/$CURRENT_API_VERSION"

        val TASKS = "$BASE/tasks"
        val ADD_TASKS = "$BASE/tasks"
        val MARK_AS_TO_DO = "$BASE/tasks/mark-as-to-do"
        fun updateTask(taskId: Int) = "$BASE/tasks/$taskId"
        fun copyTask(taskId: Int) = "$BASE/tasks/$taskId/copy"
        fun removeTaskWithSubtask(taskId: Int, removeWithSubtasks: Boolean) =
            "$BASE/tasks/$taskId?removeWithSubtasks=$removeWithSubtasks"

        val FEATURE_TOGGLES = "$HOST/fiteo/api/$CURRENT_API_VERSION/feature_toggles"
    }

    object Headers {
        val AUTHORIZATION = if (BuildKonfig.ENVIRONMENT == "p") {
            BuildKonfig.PROD_AUTH_HEADER
        } else {
            BuildKonfig.TEST_AUTH_HEADER
        }
    }
}

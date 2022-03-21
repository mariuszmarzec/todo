package com.marzec.todo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.cache.Cache
import com.marzec.cache.FileCache
import com.marzec.logger.Logger
import com.marzec.mvi.State
import com.marzec.navigation.Destination
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.delegates.dialog.ChangePriorityDelegateImpl
import com.marzec.delegate.DialogDelegateImpl
import com.marzec.todo.delegates.dialog.RemoveTaskDelegateImpl
import com.marzec.delegate.SearchDelegateImpl
import com.marzec.todo.delegates.dialog.SelectionDelegateImpl
import com.marzec.todo.delegates.dialog.UrlDelegateImpl
import com.marzec.todo.navigation.TodoDestination
import com.marzec.navigation.NavigationEntry
import com.marzec.navigation.NavigationState
import com.marzec.navigation.NavigationStore
import com.marzec.todo.network.ApiDataSource
import com.marzec.todo.network.CompositeDataSource
import com.marzec.todo.network.DataSource
import com.marzec.todo.network.LocalDataSource
import com.marzec.preferences.MemoryPreferences
import com.marzec.preferences.Preferences
import com.marzec.repository.LoginRepository
import com.marzec.repository.LoginRepositoryImpl
import com.marzec.repository.LoginRepositoryMock
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
import com.marzec.todo.screen.scheduler.SchedulerData
import com.marzec.todo.screen.scheduler.SchedulerStore
import com.marzec.todo.screen.taskdetails.TaskDetailsScreen
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.screen.tasks.TasksScreen
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.view.ActionBarProvider
import io.ktor.client.HttpClient
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.serializer

object DI {

    val logger: Logger by lazy {
        Logger.logger
    }
    lateinit var copyToClipBoardHelper: CopyToClipBoardHelper
    lateinit var openUrlHelper: OpenUrlHelper
    var ioDispatcher: CoroutineDispatcher = Dispatchers.Default
    lateinit var navigationScope: CoroutineScope

    lateinit var memoryCache: Cache

    lateinit var fileCache: FileCache
    var quickCacheEnabled: Boolean = false

    val navigationStoreCacheKey by lazy {
        cacheKeyProvider()
    }

    val preferences: Preferences = MemoryPreferences()

    private val ROUTER: Map<KClass<
            out Destination>,
            (@Composable (destination: Destination, cacheKey: String) -> Unit)
            > = mapOf(
        TodoDestination.Login::class to @Composable { _, cacheKey -> provideLoginScreen(cacheKey) },
        TodoDestination.Tasks::class to @Composable { destination, cacheKey ->
            destination as TodoDestination.Tasks
            provideTasksScreen(cacheKey)
        },
        TodoDestination.AddNewTask::class to @Composable { destination, cacheKey ->
            destination as TodoDestination.AddNewTask
            provideAddNewTaskScreen(
                taskId = destination.taskToEditId,
                parentTaskId = destination.parentTaskId,
                cacheKey
            )
        },
        TodoDestination.TaskDetails::class to @Composable { destination, cacheKey ->
            destination as TodoDestination.TaskDetails
            provideTaskDetailsScreen(destination.taskId, cacheKey)
        },
        TodoDestination.AddSubTask::class to @Composable { destination, cacheKey ->
            destination as TodoDestination.AddSubTask
            provideAddSubTaskScreen(destination.taskId, cacheKey)
        },
        TodoDestination.Schedule::class to @Composable { destination, cacheKey ->
            destination as TodoDestination.Schedule
            provideSchedulerScreen(cacheKey)
        },
    )

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
        return TasksStore(
            scope = scope,
            navigationStore = navigationStore,
            todoRepository = provideTodoRepository(),
            loginRepository = loginRepository,
            stateCache = preferences,
            cacheKey = cacheKey,
            initialState = TasksScreenState.INITIAL_STATE,
            urlDelegate = UrlDelegateImpl<TasksScreenState>(openUrlHelper),
            dialogDelegate = DialogDelegateImpl<TasksScreenState>(),
            removeTaskDelegate = RemoveTaskDelegateImpl<TasksScreenState>(
                provideTodoRepository()
            ),
            changePriorityDelegate = ChangePriorityDelegateImpl<TasksScreenState>(
                provideTodoRepository()
            ),
            searchDelegate = SearchDelegateImpl<TasksScreenState>()
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
    ): AddNewTaskStore {
        return AddNewTaskStore(
            scope = scope,
            navigationStore = navigationStore,
            todoRepository = provideTodoRepository(),
            stateCache = preferences,
            cacheKey = cacheKey,
            initialState = State.Data(
                AddNewTaskState.initial(
                    taskId = taskId,
                    parentTaskId = parentTaskId
                )
            ),
        )
    }

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
    ): TaskDetailsStore = TaskDetailsStore(
        scope = scope,
        navigationStore = navigationStore,
        todoRepository = provideTodoRepository(),
        stateCache = preferences,
        cacheKey = cacheKey,
        initialState = TaskDetailsState.INITIAL,
        taskId = taskId,
        copyToClipBoardHelper = copyToClipBoardHelper,
        dialogDelegate = DialogDelegateImpl<TaskDetailsState>(),
        removeTaskDelegate = RemoveTaskDelegateImpl<TaskDetailsState>(provideTodoRepository()),
        urlDelegate = UrlDelegateImpl<TaskDetailsState>(openUrlHelper),
        changePriorityDelegate = ChangePriorityDelegateImpl<TaskDetailsState>(
            provideTodoRepository()
        ),
        selectionDelegate = SelectionDelegateImpl<TaskDetailsState>(),
        searchDelegate = SearchDelegateImpl<TaskDetailsState>()
    )

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
    ): AddSubTaskStore {
        return AddSubTaskStore(
            scope = scope,
            navigationStore = navigationStore,
            todoRepository = provideTodoRepository(),
            stateCache = preferences,
            cacheKey = cacheKey,
            initialState = AddSubTaskData.INITIAL,
            taskId = taskId,
            selectionDelegate = SelectionDelegateImpl<AddSubTaskData>()
        )
    }  
    
    @Composable
    private fun provideSchedulerScreen(cacheKey: String) {
        SchedulerScreen(
            store = provideSchedulerStore(
                scope = rememberCoroutineScope(),
                cacheKey = cacheKey
            ),
            actionBarProvider = provideActionBarProvider()
        )
    }

    private fun provideSchedulerStore(
        scope: CoroutineScope,
        cacheKey: String
    ): SchedulerStore {
        return SchedulerStore(
            scope = scope,
            navigationStore = navigationStore,
            stateCache = preferences,
            cacheKey = cacheKey,
            initialState = SchedulerData.INITIAL
        )
    }

    private val cacheKeyProvider by lazy {
        { Random.nextInt(Int.MAX_VALUE).toString() }
    }

    lateinit var navigationStore: NavigationStore

    fun provideNavigationStore(
        scope: CoroutineScope
    ): NavigationStore {
        val authToken = runBlocking {
            fileCache.get(PreferencesKeys.AUTHORIZATION, String.serializer())
        }

        val defaultScreen = if (authToken.isNullOrEmpty()) {
            NavigationEntry(
                TodoDestination.Login,
                cacheKeyProvider()
            ) @Composable { _, it -> provideLoginScreen(it) }
        } else {
            NavigationEntry(
                TodoDestination.Tasks,
                cacheKeyProvider()
            ) @Composable { _, it -> provideTasksScreen(it) }
        }
        return NavigationStore(
            scope = scope,
            router = ROUTER,
            stateCache = preferences,
            cacheKey = navigationStoreCacheKey,
            cacheKeyProvider = cacheKeyProvider,
            initialState = NavigationState(
                backStack = listOf(
                    defaultScreen
                )
            )
        )
    }

    @Composable
    private fun provideLoginScreen(cacheKey: String) =
        LoginScreen(
            loginStore = provideLoginStore(
                scope = rememberCoroutineScope(),
                cacheKey = cacheKey
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
        cacheKey: String
    ): LoginStore = LoginStore(
        scope = scope,
        navigationStore = navigationStore,
        loginRepository = loginRepository,
        stateCache = preferences,
        cacheKey = cacheKey,
        initialState = LoginData.INITIAL
    )

    val localDataSource by lazy {
        LocalDataSource(fileCache).apply { runBlocking { init() } }
    }

    private fun provideTodoRepository() = TodoRepository(
        dataSource = provideDataSource(),
        memoryCache = memoryCache,
        dispatcher = ioDispatcher
    )

    private fun provideDataSource(): DataSource = if (BuildKonfig.ENVIRONMENT == "m") {
        localDataSource
    } else if (quickCacheEnabled) {
        CompositeDataSource(
            localDataSource,
            ApiDataSource(client),
            memoryCache,
        ).apply { runBlocking { init() } }
    } else {
        ApiDataSource(client)
    }

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
        val BASE = "$HOST/todo/api/1"

        val TASKS = "$BASE/tasks"
        val ADD_TASKS = "$BASE/tasks"
        fun updateTask(taskId: Int) = "$BASE/tasks/$taskId"
        fun removeTask(taskId: Int) = "$BASE/tasks/$taskId"
    }

    object Headers {
        val AUTHORIZATION = if (BuildKonfig.ENVIRONMENT == "p") {
            BuildKonfig.PROD_AUTH_HEADER
        } else {
            BuildKonfig.TEST_AUTH_HEADER
        }
    }
}

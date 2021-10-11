package com.marzec.todo

import androidx.compose.runtime.Composable
import com.marzec.mvi.State
import com.marzec.todo.cache.Cache
import com.marzec.todo.cache.FileCache
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.logger.Logger
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationAction
import com.marzec.todo.navigation.model.NavigationEntry
import com.marzec.todo.navigation.model.NavigationOptions
import com.marzec.todo.navigation.model.NavigationState
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.network.ApiDataSource
import com.marzec.todo.network.CompositeDataSource
import com.marzec.todo.network.DataSource
import com.marzec.todo.network.LocalDataSource
import com.marzec.todo.preferences.MemoryPreferences
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import com.marzec.todo.repository.LoginRepositoryImpl
import com.marzec.todo.repository.LoginRepositoryMock
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.screen.addnewtask.AddNewTaskScreen
import com.marzec.todo.screen.addnewtask.model.AddNewTaskState
import com.marzec.todo.screen.addnewtask.model.AddNewTaskStore
import com.marzec.todo.screen.addsubtask.AddSubTaskScreen
import com.marzec.todo.screen.addsubtask.model.AddSubTaskState
import com.marzec.todo.screen.addsubtask.model.AddSubTaskStore
import com.marzec.todo.screen.lists.ListsScreen
import com.marzec.todo.screen.lists.ListsScreenState
import com.marzec.todo.screen.lists.ListsScreenStore
import com.marzec.todo.screen.login.LoginScreen
import com.marzec.todo.screen.login.model.LoginData
import com.marzec.todo.screen.login.model.LoginStore
import com.marzec.todo.screen.taskdetails.TaskDetailsScreen
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.screen.tasks.TasksScreen
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.todo.view.ActionBarProvider
import io.ktor.client.HttpClient
import io.ktor.util.date.getTimeMillis
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.serializer

object DI {

    lateinit var logger: Logger
    lateinit var copyToClipBoardHelper: CopyToClipBoardHelper
    lateinit var openUrlHelper: OpenUrlHelper
    var ioDispatcher: CoroutineDispatcher = Dispatchers.Default
    lateinit var navigationScope: CoroutineScope

    lateinit var memoryCache: Cache

    lateinit var fileCache: FileCache
    var quickCacheEnabled: Boolean = true

    val navigationStoreCacheKey by lazy {
        cacheKeyProvider()
    }

    val preferences: Preferences = MemoryPreferences()

    private val ROUTER: Map<KClass<out Destination>, (@Composable (destination: Destination, cacheKey: String) -> Unit)> =
        mapOf(
            Destination.Login::class to @Composable { _, cacheKey -> provideLoginScreen(cacheKey) },
            Destination.Lists::class to @Composable { _, cacheKey -> provideListScreen(cacheKey) },
            Destination.Tasks::class to @Composable { destination, cacheKey ->
                destination as Destination.Tasks
                provideTasksScreen(destination.listId, cacheKey)
            },
            Destination.AddNewTask::class to @Composable { destination, cacheKey ->
                destination as Destination.AddNewTask
                provideAddNewTaskScreen(
                    listId = destination.listId,
                    taskId = destination.taskToEditId,
                    parentTaskId = destination.parentTaskId,
                    cacheKey
                )
            },
            Destination.TaskDetails::class to @Composable { destination, cacheKey ->
                destination as Destination.TaskDetails
                provideTaskDetailsScreen(destination.listId, destination.taskId, cacheKey)
            },
            Destination.AddSubTask::class to @Composable { destination, cacheKey ->
                destination as Destination.AddSubTask
                provideAddSubTaskScreen(destination.listId, destination.taskId, cacheKey)
            }
        )

    @Composable
    private fun provideTasksScreen(listId: Int, cacheKey: String) {
        TasksScreen(
            store = provideTasksStore(listId = listId, cacheKey = cacheKey),
            actionBarProvider = provideActionBarProvider()
        )
    }

    private fun provideTasksStore(listId: Int, cacheKey: String): TasksStore {
        return TasksStore(
            navigationStore = navigationStore,
            listId = listId,
            todoRepository = provideTodoRepository(),
            stateCache = preferences,
            cacheKey = cacheKey,
            initialState = TasksScreenState.INITIAL_STATE,
            openUrlHelper = openUrlHelper
        )
    }

    @Composable
    private fun provideAddNewTaskScreen(
        listId: Int,
        taskId: Int?,
        parentTaskId: Int?,
        cacheKey: String
    ) {
        AddNewTaskScreen(
            provideAddNewTaskStore(
                listId = listId,
                taskId = taskId,
                parentTaskId = parentTaskId,
                cacheKey = cacheKey
            ),
            actionBarProvider = provideActionBarProvider(),
        )
    }

    private fun provideAddNewTaskStore(
        listId: Int,
        taskId: Int?,
        parentTaskId: Int?,
        cacheKey: String
    ): AddNewTaskStore {
        return AddNewTaskStore(
            navigationStore = navigationStore,
            todoRepository = provideTodoRepository(),
            stateCache = preferences,
            cacheKey = cacheKey,
            initialState = State.Data(
                AddNewTaskState.initial(
                    listId = listId,
                    taskId = taskId,
                    parentTaskId = parentTaskId
                )
            ),
        )
    }

    @Composable
    private fun provideTaskDetailsScreen(listId: Int, taskId: Int, cacheKey: String) {
        TaskDetailsScreen(
            listId = listId,
            taskId = taskId,
            store = provideTaskDetailsStore(
                listId = listId,
                taskId = taskId,
                cacheKey = cacheKey
            ),
            actionBarProvider = provideActionBarProvider()
        )
    }

    private fun provideTaskDetailsStore(
        listId: Int,
        taskId: Int,
        cacheKey: String
    ): TaskDetailsStore {
        return TaskDetailsStore(
            navigationStore = navigationStore,
            todoRepository = provideTodoRepository(),
            stateCache = preferences,
            cacheKey = cacheKey,
            initialState = TaskDetailsState.INITIAL,
            listId = listId,
            taskId = taskId,
            copyToClipBoardHelper = copyToClipBoardHelper,
            openUrlHelper = openUrlHelper
        )
    }

    @Composable
    private fun provideAddSubTaskScreen(listId: Int, taskId: Int, cacheKey: String) {
        AddSubTaskScreen(
            store = provideAddSubTaskStore(
                listId = listId,
                taskId = taskId,
                cacheKey = cacheKey
            ),
            actionBarProvider = provideActionBarProvider()
        )
    }

    private fun provideAddSubTaskStore(
        listId: Int,
        taskId: Int,
        cacheKey: String
    ): AddSubTaskStore {
        return AddSubTaskStore(
            navigationStore = navigationStore,
            todoRepository = provideTodoRepository(),
            stateCache = preferences,
            cacheKey = cacheKey,
            initialState = AddSubTaskState.INITIAL,
            listId = listId,
            taskId = taskId
        )
    }

    private val cacheKeyProvider by lazy { { getTimeMillis().toString() } }

    lateinit var navigationStore: NavigationStore

    fun provideNavigationStore(): NavigationStore {
        val authToken = runBlocking {
            fileCache.get(PreferencesKeys.AUTHORIZATION, String.serializer())
        }

        val defaultScreen = if (authToken.isNullOrEmpty()) {
            NavigationEntry(
                Destination.Login,
                cacheKeyProvider()
            ) @Composable { _, it -> provideLoginScreen(it) }
        } else {
            NavigationEntry(
                Destination.Lists,
                cacheKeyProvider()
            ) @Composable { _, it -> provideListScreen(it) }
        }
        return NavigationStore(
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
    private fun provideListScreen(cacheKey: String) {
        ListsScreen(
            navigationStore = navigationStore,
            actionBarProvider = provideActionBarProvider(),
            listsScreenStore = provideListScreenStore(cacheKey = cacheKey)
        )
    }

    @Composable
    private fun provideListScreenStore(cacheKey: String) = ListsScreenStore(
        todoRepository = provideTodoRepository(),
        stateCache = preferences,
        cacheKey = cacheKey,
        initialState = provideListScreenDefaultState(),
        navigationStore = navigationStore,
        loginRepository = loginRepository
    )

    private fun provideListScreenDefaultState(): State<ListsScreenState> {
        return State.Data(ListsScreenState.INITIAL)
    }

    @Composable
    private fun provideLoginScreen(cacheKey: String) =
        LoginScreen(loginStore = provideLoginStore(cacheKey))

    lateinit var client: HttpClient

    val loginRepository: LoginRepository by lazy {
        if (BuildKonfig.ENVIRONMENT == "m") LoginRepositoryMock() else LoginRepositoryImpl(
            client,
            ioDispatcher,
            fileCache
        )
    }

    fun provideLoginStore(cacheKey: String): LoginStore = LoginStore(
        loginRepository = loginRepository,
        stateCache = preferences,
        cacheKey = cacheKey,
        onLoginSuccess = {
            navigationScope.launch {
                navigationStore.next(
                    NavigationAction(
                        Destination.Lists,
                        options = NavigationOptions(
                            popTo = Destination.Lists,
                            popToInclusive = true
                        )
                    )
                )
            }
        },
        initialState = LoginData.INITIAL
    )

    val localDataSource by lazy {
        LocalDataSource(fileCache).apply { runBlocking { init() } }
    }

    fun provideTodoRepository() = TodoRepository(provideDataSource(), memoryCache)

    private fun provideDataSource(): DataSource = if (BuildKonfig.ENVIRONMENT == "m") {
        localDataSource
    } else if (quickCacheEnabled) {
        CompositeDataSource(
            localDataSource,
            ApiDataSource(client),
            memoryCache
        ).apply { runBlocking { init() } }
    } else {
        ApiDataSource(client)
    }

    private fun provideActionBarProvider() = ActionBarProvider(navigationStore)
}

object PreferencesKeys {
    val AUTHORIZATION = "AUTHORIZATION"
}

object Api {

    const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

    val HOST = if (BuildKonfig.ENVIRONMENT == "p") {
        BuildKonfig.PROD_API_URL
    } else {
        BuildKonfig.TEST_API_URL
    }

    object Login {
        val BASE = "$HOST/fiteo/api/1"

        val LOGIN = "$BASE/login"
        val LOGOUT = "$BASE/logout"
    }

    object Todo {
        val BASE = "$HOST/todo/api/1"

        val TODO_LISTS = "$BASE/lists"
        val TODO_LIST = "$BASE/list"
        fun createTask(listId: Int) = "$BASE/list/$listId/tasks"
        fun updateTask(taskId: Int) = "$BASE/tasks/$taskId"
        fun removeTask(taskId: Int) = "$BASE/tasks/$taskId"
        fun removeList(listId: Int) = "$BASE/list/$listId"
    }

    object Headers {
        val AUTHORIZATION = if (BuildKonfig.ENVIRONMENT == "p") {
            BuildKonfig.PROD_AUTH_HEADER
        } else {
            BuildKonfig.TEST_AUTH_HEADER
        }
    }
}
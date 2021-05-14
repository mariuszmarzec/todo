package com.marzec.todo

import androidx.compose.runtime.Composable
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationActions
import com.marzec.todo.navigation.model.NavigationEntry
import com.marzec.todo.navigation.model.NavigationState
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.preferences.MemoryPreferences
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.screen.addnewtask.AddNewTaskScreen
import com.marzec.todo.screen.addnewtask.model.AddNewTaskState
import com.marzec.todo.screen.addnewtask.model.AddNewTaskStore
import com.marzec.todo.screen.lists.ListsScreen
import com.marzec.todo.screen.lists.ListsScreenState
import com.marzec.todo.screen.lists.ListsScreenStore
import com.marzec.todo.screen.login.LoginScreen
import com.marzec.todo.screen.login.model.LoginData
import com.marzec.todo.screen.login.model.LoginStore
import com.marzec.todo.screen.login.model.LoginViewState
import com.marzec.todo.screen.taskdetails.TaskDetailsScreen
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.screen.tasks.TasksScreen
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import io.ktor.client.HttpClient
import io.ktor.util.date.getTimeMillis
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DI {

    var ioDispatcher: CoroutineContext = Dispatchers.Default
    lateinit var navigationScope: CoroutineScope

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
                    taskId = destination.taskId,
                    parentTaskId = destination.parentTaskId,
                    cacheKey
                )
            },
            Destination.TaskDetails::class to @Composable { destination, cacheKey ->
                destination as Destination.TaskDetails
                provideTaskDetailsScreen(destination.listId, destination.taskId, cacheKey)
            }
        )

    @Composable
    private fun provideTasksScreen(listId: Int, cacheKey: String) {
        TasksScreen(navigationStore, provideTasksStore(listId = listId, cacheKey = cacheKey))
    }

    private fun provideTasksStore(listId: Int, cacheKey: String): TasksStore {
        return TasksStore(
            navigationStore = navigationStore,
            listId = listId,
            todoRepository = provideTodoRepository(),
            stateCache = preferences,
            cacheKey = cacheKey,
            initialState = TasksScreenState.INITIAL_STATE
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
            navigationStore,
            provideAddNewTaskStore(
                listId = listId,
                taskId = taskId,
                parentTaskId = parentTaskId,
                cacheKey = cacheKey
            )
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
            initialState = AddNewTaskState.initial(
                listId = listId,
                taskId = taskId,
                parentTaskId = parentTaskId
            ),
        )
    }

    @Composable
    private fun provideTaskDetailsScreen(listId: Int, taskId: Int, cacheKey: String) {
        TaskDetailsScreen(
            navigationStore, provideTaskDetailsStore(
                listId = listId,
                taskId = taskId,
                cacheKey = cacheKey
            )
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
            taskId = taskId
        )
    }

    private val cacheKeyProvider by lazy { { getTimeMillis().toString() } }

    val navigationStore: NavigationStore by lazy {
        NavigationStore(
            router = ROUTER,
            stateCache = preferences,
            cacheKeyProvider = cacheKeyProvider,
            initialState = NavigationState(
                backStack = listOf(
                    NavigationEntry(
                        Destination.Login,
                        cacheKeyProvider()
                    ) @Composable { _, it -> provideLoginScreen(it) }
                )
            )
        )
    }

    @Composable
    private fun provideListScreen(cacheKey: String) {
        ListsScreen(navigationStore, provideListScreenStore(cacheKey = cacheKey))
    }

    @Composable
    private fun provideListScreenStore(cacheKey: String) = ListsScreenStore(
        todoRepository = provideTodoRepository(),
        stateCache = preferences,
        cacheKey = cacheKey,
        initialState = provideListScreenDefaultState()
    )

    private fun provideListScreenDefaultState(): ListsScreenState {
        return ListsScreenState.INITIAL
    }

    @Composable
    private fun provideLoginScreen(cacheKey: String) =
        LoginScreen(loginStore = provideLoginStore(cacheKey))

    lateinit var client: HttpClient

    fun provideLoginRepository() = LoginRepository(client)

    fun provideLoginStore(cacheKey: String): LoginStore = LoginStore(
        loginRepository = provideLoginRepository(),
        stateCache = preferences,
        cacheKey = cacheKey,
        onLoginSuccess = {
            navigationScope.launch {
                navigationStore.sendAction(NavigationActions.Next(Destination.Lists))
            }
        },
        initialState = LoginViewState.Data(
            loginData = LoginData(
                login = "mariusz.marzec00@gmail.com",
                password = "password"
            )
        )
    )

    fun provideTodoRepository() = TodoRepository(client)
}

object PreferencesKeys {
    const val AUTHORIZATION = "AUTHORIZATION"
}

object Api {

    const val HOST = "http://fiteo-env.eba-mpctrvdb.us-east-2.elasticbeanstalk.com/test"
//    const val HOST = "http://localhost:500"

    object Login {
        const val BASE = "$HOST/fiteo/api/1"

        const val LOGIN = "$BASE/login"
        const val USER = "$BASE/user"
        const val LOGOUT = "$BASE/logout"
    }

    object Todo {
        const val BASE = "$HOST/todo/api/1"

        const val TODO_LISTS = "$BASE/lists"
        const val TODO_LIST = "$BASE/list"
        fun createTask(listId: Int) = "$BASE/list/$listId/tasks"
        fun updateTask(taskId: Int) = "$BASE/tasks/$taskId"
        fun removeTask(taskId: Int) = "$BASE/tasks/$taskId"
    }

    object Headers {
        const val AUTHORIZATION = "Authorization-Test"
    }
}
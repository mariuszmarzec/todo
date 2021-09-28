package com.marzec.todo.network

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.todo.cache.FileCache
import com.marzec.todo.cache.getTyped
import com.marzec.todo.cache.putTyped
import com.marzec.todo.common.currentTime
import com.marzec.todo.common.formatDate
import com.marzec.todo.common.Lock
import com.marzec.todo.extensions.replaceIf
import kotlinx.serialization.Serializable

@Serializable
data class LocalData(
    val tasks: List<TaskDto> = emptyList(),
    val lists: List<ToDoListDto> = emptyList(),
    val listIdToTaskIds: Map<Int, List<Int>> = emptyMap()
)

class LocalDataSource(private val fileCache: FileCache) : DataSource {

    private val CACHE_KEY = "LOCAL_DATA"

    private val lock = Lock()

    private var localData: LocalData = LocalData(emptyList(), emptyList(), emptyMap())

    suspend fun init() {
        fileCache.getTyped<LocalData>(CACHE_KEY)?.let { localData = it }
    }

    // TODO UPDATE LOCAL LOGIC
    override suspend fun removeTask(taskId: Int) = update {
        localData = localData.copy(
            tasks = localData.tasks.toMutableList().apply { removeIf { it.id == taskId } }
                .replaceIf(
                    condition = { task -> task.parentTaskId == taskId },
                    replace = { it.copy(parentTaskId = null) }
                ),
            listIdToTaskIds = localData.listIdToTaskIds.let { listIdToTaskIds ->
                val listId = listIdToTaskIds.toList().find {
                    it.second.any { taskIdFromList -> taskIdFromList == taskId }
                }!!.first
                listIdToTaskIds.toMutableMap().apply {
                    val taskIds = getValue(listId).apply { remove(taskId) }
                    set(listId, taskIds)
                }
            }
        )
    }

    override suspend fun getTodoLists(): List<ToDoListDto> = try {
        lock.lock()
        val remainedTasks = localData.tasks.toMutableList()
        localData.lists.map { list ->
            val tasksWithoutSubtasks = localData.listIdToTaskIds.getValue(list.id)
                .mapNotNull { taskId ->
                    val task = localData.tasks.firstOrNull { taskId == it.id && it.parentTaskId == null }
                    task?.let { remainedTasks.remove(task) }
                    task
                }
                .sortedWith(compareByDescending(TaskDto::priority).thenBy(TaskDto::modifiedTime))

            val tasks = tasksWithoutSubtasks.map { task ->
                task.copy(subTasks = getSubTasks(remainedTasks, task))
            }

            list.copy(tasks = tasks)
        }
    } finally {
        lock.unlock()
    }

    private fun getSubTasks(
        remainedTasks: MutableList<TaskDto>,
        parentTask: TaskDto
    ): List<TaskDto> = if (remainedTasks.isNotEmpty()) {
        val subtasks = remainedTasks.filter { it.parentTaskId == parentTask.id }
        remainedTasks.removeAll(subtasks)
        subtasks.map { task -> task.copy(subTasks = getSubTasks(remainedTasks, task)) }
    } else {
        emptyList()
    }.sortedWith(compareByDescending(TaskDto::priority).thenBy(TaskDto::modifiedTime))


    override suspend fun removeList(id: Int) = update {
        localData = localData.copy(
            lists = localData.lists.toMutableList().apply { removeIf { it.id == id } },
            listIdToTaskIds = localData.listIdToTaskIds.toMutableMap().apply { remove(id) }
        )
    }

    override suspend fun createToDoList(title: String) = update {
        val newListId = localData.lists.size
        localData = localData.copy(
            lists = localData.lists + ToDoListDto(id = newListId, title = title, tasks = emptyList()),
            listIdToTaskIds = localData.listIdToTaskIds + (newListId to emptyList())
        )
    }

    override suspend fun addNewTask(listId: Int, createTaskDto: CreateTaskDto) = update {
        val tasks = localData.tasks
        val newTaskId = tasks.size
        localData = localData.copy(
            tasks = tasks.toMutableList() + createNewTask(newTaskId, createTaskDto, tasks),
            listIdToTaskIds = localData.listIdToTaskIds.toMutableMap().apply {
                val taskIds = getValue(listId) + newTaskId
                set(listId, taskIds)
            }
        )
    }

    private fun createNewTask(
        newTaskId: Int,
        createTaskDto: CreateTaskDto,
        tasks: List<TaskDto>
    ) = TaskDto(
        id = newTaskId,
        description = createTaskDto.description,
        currentTime().formatDate(),
        currentTime().formatDate(),
        parentTaskId = createTaskDto.parentTaskId,
        subTasks = emptyList(),
        isToDo = true,
        priority = createTaskDto.priority
            ?: if (createTaskDto.highestPriorityAsDefault == true) {
                (subTasksOfParentOrTasks(tasks, createTaskDto).maxOfOrNull { it.priority }
                    ?: 0) + 1
            } else {
                (subTasksOfParentOrTasks(tasks, createTaskDto).minOfOrNull { it.priority }
                    ?: 0) - 1
            }
    )

    private fun subTasksOfParentOrTasks(tasks: List<TaskDto>, createTaskDto: CreateTaskDto) =
        (createTaskDto.parentTaskId?.let { parentTask ->
            getSubTasks(
                tasks.toMutableList(),
                tasks.first { parentTask == it.id })
        } ?: tasks)

    override suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean
    ) = update {
        localData = localData.copy(
            tasks = localData.tasks.replaceIf(
                condition = { it.id == taskId },
                replace = { task ->
                    task.copy(
                        description = description,
                        parentTaskId = parentTaskId,
                        priority = priority,
                        isToDo = isToDo,
                        modifiedTime = currentTime().formatDate(),
                    )
                }
            )
        )
    }

    private suspend fun update(action: () -> Unit) = try {
        lock.lock()
        action()
    } finally {
        updateStorage()
        lock.unlock()
    }

    private suspend fun updateStorage() {
        fileCache.putTyped(CACHE_KEY, localData)
    }
}


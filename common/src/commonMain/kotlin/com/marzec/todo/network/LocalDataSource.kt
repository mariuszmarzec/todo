package com.marzec.todo.network

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.todo.cache.FileCache
import com.marzec.todo.cache.getTyped
import com.marzec.todo.cache.putTyped
import com.marzec.todo.common.currentTime
import com.marzec.todo.common.formatDate
import kotlinx.serialization.Serializable

@Serializable
data class LocalData(
    val tasks: MutableList<TaskDto> = mutableListOf(),
    val lists: MutableList<ToDoListDto> = mutableListOf(),
    val listIdToTaskIds: MutableMap<Int, List<Int>> = mutableMapOf()
)

class LocalDataSource(private val fileCache: FileCache) : DataSource {

    private val CACHE_KEY = "LOCAL_DATA"

    private var tasks: MutableList<TaskDto> = mutableListOf()
    private var lists: MutableList<ToDoListDto> = mutableListOf()
    private var listIdToTaskIds = mutableMapOf<Int, List<Int>>()

    suspend fun init() {
        fileCache.getTyped<LocalData>(CACHE_KEY)?.let {
            this@LocalDataSource.tasks = it.tasks
            this@LocalDataSource.lists = it.lists
            this@LocalDataSource.listIdToTaskIds = it.listIdToTaskIds
        }
    }

    // TODO UPDATE LOCAL LOGIC
    override suspend fun removeTask(taskId: Int) {
        tasks.removeIf { it.id == taskId }
        tasks.forEachIndexed { index, task ->
            if (task.parentTaskId == taskId) {
                tasks[index] = task.copy(parentTaskId = null)
            }
        }
        val listId = listIdToTaskIds.toList().find {
            it.second.any { taskIdFromList -> taskIdFromList == taskId }
        }!!.first
        listIdToTaskIds[listId] = listIdToTaskIds[listId]!!.toMutableList().apply {
            remove(taskId)
        }
        updateStorage()
    }

    private suspend fun updateStorage() {
        val localData = LocalData(tasks, lists, listIdToTaskIds)
        fileCache.putTyped(CACHE_KEY, localData)
    }

    override suspend fun getTodoLists(): List<ToDoListDto> {
        val remainedTasks = tasks.toMutableList()
        return lists.map { list ->
            val tasksWithoutSubtasks = listIdToTaskIds.getValue(list.id)
                .mapNotNull { taskId ->
                    val task = tasks.firstOrNull { taskId == it.id && it.parentTaskId == null }
                    task?.let { remainedTasks.remove(task) }
                    task
                }
                .sortedWith(compareByDescending(TaskDto::priority).thenBy(TaskDto::modifiedTime))

            val tasks = tasksWithoutSubtasks.map { task ->
                task.copy(subTasks = getSubTasks(remainedTasks, task))
            }

            list.copy(tasks = tasks)
        }
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


    override suspend fun removeList(id: Int) {
        lists.removeIf { it.id == id }
        listIdToTaskIds.remove(id)
        updateStorage()
    }

    override suspend fun createToDoList(title: String) {
        val id = lists.size
        lists.add(ToDoListDto(id = id, title = title, tasks = emptyList()))
        listIdToTaskIds[id] = emptyList()
        updateStorage()
    }

    override suspend fun addNewTask(listId: Int, createTaskDto: CreateTaskDto) {
        val newTaskId = tasks.size
        listIdToTaskIds[listId] = listIdToTaskIds[listId]!!.toMutableList().apply {
            add(newTaskId)
        }
        tasks.add(
            TaskDto(
                id = newTaskId,
                description = createTaskDto.description,
                currentTime().formatDate(),
                currentTime().formatDate(),
                parentTaskId = createTaskDto.parentTaskId,
                subTasks = emptyList(),
                isToDo = true,
                priority = createTaskDto.priority
                    ?: if (createTaskDto.highestPriorityAsDefault == true) {
                        (subTasksOfParentOrTasks(createTaskDto).maxOfOrNull { it.priority }
                            ?: 0) + 1
                    } else {
                        (subTasksOfParentOrTasks(createTaskDto).minOfOrNull { it.priority }
                            ?: 0) - 1
                    }
            )
        )
        updateStorage()
    }

    private fun subTasksOfParentOrTasks(createTaskDto: CreateTaskDto) =
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
    ) {
        val indexInList = tasks.indexOfFirst { it.id == taskId }
        val task = tasks[indexInList]

        val newTask = task.copy(
            description = description,
            parentTaskId = parentTaskId,
            priority = priority,
            isToDo = isToDo,
            modifiedTime = currentTime().formatDate(),
        )

        tasks[indexInList] = newTask
        updateStorage()
    }
}


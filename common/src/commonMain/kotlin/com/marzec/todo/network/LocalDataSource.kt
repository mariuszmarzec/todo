package com.marzec.todo.network

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.ToDoListDto
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object LocalDataSource : DataSource {

    private val tasks: MutableList<TaskDto> = mutableListOf()

    private val lists: MutableList<ToDoListDto> = mutableListOf()

    private val listIdToTaskIds = mutableMapOf<Int, List<Int>>()


    override suspend fun removeTask(taskId: Int) {
        tasks.removeIf { it.id == taskId }
        val listId = listIdToTaskIds.toList().find {
            it.second.any { taskIdFromList -> taskIdFromList == taskId }
        }!!.first
        listIdToTaskIds[listId] = listIdToTaskIds[listId]!!.toMutableList().apply {
            remove(taskId)
        }
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
    }

    override suspend fun createToDoList(title: String) {
        val id = lists.size
        lists.add(ToDoListDto(id = id, title = title, tasks = emptyList()))
        listIdToTaskIds[id] = emptyList()
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
                currentTime(),
                currentTime(),
                parentTaskId = createTaskDto.parentTaskId,
                subTasks = emptyList(),
                isToDo = true,
                priority = if (createTaskDto.highestPriorityAsDefault == true) {
                    (subTasksOfParentOrTasks(createTaskDto).maxOfOrNull { it.priority } ?: 0) + 1
                } else {
                    (subTasksOfParentOrTasks(createTaskDto).minOfOrNull { it.priority } ?: 0) - 1
                }
            )
        )
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
    }

    private fun currentTime() =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
}

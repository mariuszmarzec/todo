package com.marzec.todo.network

import androidx.compose.ui.util.fastMaxOfOrNull
import com.marzec.cache.FileCache
import com.marzec.cache.getTyped
import com.marzec.cache.putTyped
import com.marzec.extensions.replaceIf
import com.marzec.locker.Locker
import com.marzec.time.currentTime
import com.marzec.time.formatDate
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.MarkAsToDoDto
import com.marzec.todo.api.SchedulerDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.UpdateTaskDto
import com.marzec.todo.extensions.flatMapTaskDto
import kotlinx.serialization.Serializable

@Serializable
data class LocalData(
    val tasks: List<TaskDto> = emptyList()
)

class LocalDataSource(private val fileCache: FileCache) : DataSource {

    private val cacheKey = "LOCAL_DATA"

    private val lock = Locker()

    private var localData: LocalData = LocalData(emptyList())

    suspend fun init() {
        fileCache.getTyped<LocalData>(cacheKey)?.let { localData = it }
    }

    suspend fun init(tasks: List<TaskDto>) = update {
        val allTasks = tasks.flatMapTaskDto()
        localData = LocalData(tasks = allTasks)
    }

    override suspend fun removeTask(taskId: Int, removeSubtasks: Boolean) = update {
        if (removeSubtasks) {
            val taskDto = localData.tasks.first { task -> task.id == taskId }
            removeTaskWithSubtasks(taskDto)
        } else {
            removeTaskInternal(taskId)
        }
    }

    private fun removeTaskWithSubtasks(taskDto: TaskDto) {
        removeTaskInternal(taskDto.id)
        taskDto.subTasks.forEach { removeTaskWithSubtasks(it) }
    }

    override suspend fun getTasks(): List<TaskDto> = try {
        lock.lock()
        val rootTasks = localData.tasks.filter { it.parentTaskId == null }
        val subTasks = localData.tasks.filter { it.parentTaskId != null }.toMutableList()

        rootTasks.fillSubTasks(subTasks)
    } finally {
        lock.unlock()
    }

    private fun List<TaskDto>.fillSubTasks(subTasks: List<TaskDto>): List<TaskDto> = map { root ->
        root.copy(subTasks = subTasks.filter { it.parentTaskId == root.id }.fillSubTasks(subTasks))
    }


    override suspend fun copyTask(taskId: Int) {
        var taskToCopy: TaskDto? = null
        update {
            taskToCopy = localData.tasks.firstOrNull { it.id == taskId }
        }
        taskToCopy?.let {
            addNewTask(it.toCreateTask())
        }
    }

    private fun removeTaskInternal(taskId: Int) {
        val taskToRemove = localData.tasks.first { it.id == taskId }
        localData = localData.copy(
            tasks = localData.tasks.toMutableList().apply { removeIf { it.id == taskId } }
                .replaceIf(
                    condition = { task -> task.parentTaskId == taskId },
                    replace = { it.copy(parentTaskId = taskToRemove.parentTaskId) }
                )
        )
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
    }.sortedWith(
        compareByDescending(TaskDto::isToDo).thenByDescending(TaskDto::priority)
            .thenBy(TaskDto::modifiedTime)
    )

    override suspend fun addNewTask(createTaskDto: CreateTaskDto) = update {
        val tasks = localData.tasks
        val newTaskId = (tasks.maxOfOrNull { it.id } ?: 0).inc()
        localData = localData.copy(
            tasks = tasks.toMutableList() + createNewTask(newTaskId, createTaskDto, tasks),
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
        scheduler = createTaskDto.scheduler,
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
        task: UpdateTaskDto
    ) = update {
        localData = localData.copy(
            tasks = localData.tasks.replaceIf(
                condition = { it.id == taskId },
                replace = { changedTask ->
                    changedTask.copy(
                        description = task.description ?: changedTask.description,
                        parentTaskId = if (task.parentTaskId != null) {
                            task.parentTaskId.value
                        } else {
                            changedTask.parentTaskId
                        },
                        priority = task.priority ?: changedTask.priority,
                        isToDo = task.isToDo ?: changedTask.isToDo,
                        modifiedTime = currentTime().formatDate(),
                        scheduler = if (task.scheduler != null) {
                            task.scheduler.value
                        } else {
                            changedTask.scheduler
                        }
                    )
                }
            )
        )
    }

    override suspend fun markAsToDo(markAsToDo: MarkAsToDoDto) = update {
        localData = localData.copy(
            tasks = localData.tasks.replaceIf(
                condition = { it.id in markAsToDo.taskIds },
                replace = { task ->
                    task.copy(
                        isToDo = markAsToDo.isToDo,
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
        fileCache.putTyped(cacheKey, localData)
    }
}

private fun TaskDto.toCreateTask() = CreateTaskDto(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
    highestPriorityAsDefault = false,
    scheduler = scheduler
)


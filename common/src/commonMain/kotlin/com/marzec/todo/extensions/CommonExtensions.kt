package com.marzec.todo.extensions

import com.marzec.todo.api.TaskDto
import com.marzec.todo.model.Task
import com.marzec.extensions.filterWithSearch

@Suppress("unchecked_cast")
inline fun <reified T : Any> Any.asInstance(action: T.() -> Unit) = (this as? T)?.action()

@Suppress("unchecked_cast")
inline fun <reified T : Any, R> Any.asInstanceAndReturnOther(action: T.() -> R) =
    (this as? T)?.action()

@Suppress("unchecked_cast")
inline fun <reified T : Any> Any.asInstanceAndReturn(action: T.() -> T) = (this as? T)?.action()

fun Task.urlToOpen(): String? = subTasks
    .firstOrNull { it.isToDo }
    ?.urlToOpen()
    ?: description.urls().firstOrNull()

fun List<Task>.flatMapTask(tasks: MutableList<Task> = mutableListOf()): List<Task> {
    forEach {
        tasks.add(it)
        it.subTasks.flatMapTask(tasks)
    }
    return tasks
}

fun List<TaskDto>.flatMapTaskDto(tasks: MutableList<TaskDto> = mutableListOf()): List<TaskDto> {
    forEach {
        tasks.add(it)
        it.subTasks.flatMapTaskDto(tasks)
    }
    return tasks
}

inline fun <T> Boolean.ifTrue(valueLambda: () -> T): T? = if (this) valueLambda() else null

inline fun <T> Boolean.ifFalse(valueLambda: () -> T): T? = if (this) null else valueLambda()

fun List<Task>.filterWithSearch(search: String): List<Task> = filterWithSearch(search) {
    listOf(it.description)
}

fun List<Task>.findRootIdOrNull(taskId: Int): Int {
    val parentTaskId = findParentIdForTask(taskId)
    return if (parentTaskId != null) {
        findRootIdOrNull(parentTaskId)
    } else {
        taskId
    }
}

fun List<Task>.findParentIdForTask(taskId: Int): Int? = taskId.let {
    flatMapTask().firstOrNull { it.id == taskId }?.parentTaskId
}

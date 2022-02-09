package com.marzec.todo.extensions

import com.marzec.todo.api.TaskDto
import com.marzec.todo.model.Task

const val EMPTY_STRING = ""

@Suppress("unchecked_cast")
inline fun <reified T: Any> Any.asInstance(action: T.() -> Unit) = (this as? T)?.action()

@Suppress("unchecked_cast")
inline fun <reified T: Any, R> Any.asInstanceAndReturnOther(action: T.() -> R) = (this as? T)?.action()

@Suppress("unchecked_cast")
inline fun <reified T: Any> Any.asInstanceAndReturn(action: T.() -> T) = (this as? T)?.action()

fun Task.urlToOpen(): String? = subTasks.firstOrNull()?.urlToOpen()
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

fun <T> Boolean.ifTrue(valueLambda: () -> T): T? = if (this) valueLambda() else null

fun <T> Boolean.ifFalse(valueLambda: () -> T): T? = if (this) null else valueLambda()

fun List<Task>.filterWithSearch(search: String): List<Task> {
    val searchQuery = search.trim().split(" ")
    return filter { task ->
        searchQuery == listOf(EMPTY_STRING) || searchQuery.all {
            task.description.contains(
                it,
                ignoreCase = true
            )
        }
    }
}
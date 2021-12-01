package com.marzec.todo.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.mvi.newMvi.Store2
import com.marzec.todo.api.TaskDto
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.model.Task
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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

@Suppress("unchecked_cast")
fun <STATE : Any> Store2<STATE>.delegates(vararg delegates: Any) {
    delegates.forEach {
        (it as StoreDelegate<STATE>).init(this@delegates)
    }
}

@Composable
fun <T: Any> Store2<T>.collectState(
    context: CoroutineContext = EmptyCoroutineContext,
    onStoreInitAction: suspend () -> Unit = { }
): androidx.compose.runtime.State<T> {

    val state = state.collectAsState(state.value, context)
    LaunchedEffect(key1 = identifier) {
        init {
            onStoreInitAction()
        }
    }
    return state
}

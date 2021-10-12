package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.todo.api.toDomain
import com.marzec.todo.cache.Cache
import com.marzec.todo.common.Lock
import com.marzec.todo.common.currentMillis
import com.marzec.todo.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class DeferrableDataSource(
    private val localDataSource: LocalDataSource,
    private val apiDataSource: ApiDataSource,
    private val memoryCache: Cache,
    private val jobActionRunner: JobActionRunner
) : DataSource {

    suspend fun init() {
        jobActionRunner.onAllJobsComplete = {
            val todoLists = apiDataSource.getTodoLists()
            localDataSource.init(todoLists)
            updateMemory()
        }
        updateMemory()
        jobActionRunner.runJobs()
    }

    override suspend fun removeTask(taskId: Int) = update {
        removeTask(taskId)
    }

    override suspend fun getTodoLists(): List<ToDoListDto> = localDataSource.getTodoLists()

    override suspend fun removeList(id: Int) = update {
        removeList(id)
    }

    override suspend fun createToDoList(title: String) = update {
        createToDoList(title)
    }

    override suspend fun addNewTask(listId: Int, createTaskDto: CreateTaskDto) = update {
        addNewTask(listId, createTaskDto)
    }

    override suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean
    ) = update {
        updateTask(taskId, description, parentTaskId, priority, isToDo)
    }

    private suspend fun update(action: suspend DataSource.() -> Unit) {
        localDataSource.action()
        updateMemory()
        jobActionRunner.addNewJob { apiDataSource.action() }
    }

    private suspend fun updateMemory() {
        memoryCache.put(Api.Todo.TODO_LISTS, localDataSource.getTodoLists().map { it.toDomain() })
    }
}

data class JobAction(
    val id: String,
    val action: suspend () -> Unit
)

class JobActionRunner(
    private val logger: Logger,
    private val scope: CoroutineScope
) {

    var onAllJobsComplete: suspend () -> Unit = { }

    private val jobs = mutableListOf<JobAction>()
    private val jobsLock = Lock()

    private var pending: Boolean = false
    private val pendingLock = Lock()

    fun addNewJob(action: suspend () -> Unit) {
        synchronized(jobsLock) {
            jobs.add(JobAction(id = currentMillis().toString(), action))
        }
        scope.async { runJobs() }
    }

    suspend fun runJobs() {
        val isPending = compareAndSet(expected = false, newValue = true)
        if (isPending) {
            return
        }
        try {
            while (synchronized(jobsLock) { jobs.isNotEmpty() }) {
                val job = synchronized(jobsLock) { jobs.first() }
                job.action()
                synchronized(jobsLock) { jobs.removeFirst() }
            }
            onAllJobsComplete()
        } catch (expected: Exception) {
            logger.log(this::class.simpleName.orEmpty(), expected.message.orEmpty())
        } finally {
            synchronized(pendingLock) { pending = false }
        }
    }

    private fun compareAndSet(
        expected: Boolean,
        newValue: Boolean
    ): Boolean {
        return synchronized(pendingLock) {
            if (pending == expected) {
                val oldValue = pending
                pending = newValue
                oldValue
            } else {
                pending
            }
        }
    }

    private fun <R> synchronized(lock: Lock, action: () -> R): R = try {
        lock.lock()
        action()
    } finally {
        lock.unlock()
    }
}

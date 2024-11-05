package com.marzec.todo.network

import com.marzec.cache.FileCache
import com.marzec.cache.getTyped
import com.marzec.cache.putTyped
import com.marzec.dto.NullableFieldDto
import com.marzec.extensions.replaceIf
import com.marzec.locker.Locker
import com.marzec.time.currentTime
import com.marzec.time.formatDate
import com.marzec.time.withStartOfDay
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.MarkAsToDoDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.UpdateTaskDto
import com.marzec.todo.extensions.flatMapTaskDto
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.highestPriorityAsDefault
import com.marzec.todo.model.toDomain
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.YearMonth
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
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

    override suspend fun removeTask(taskId: Int, removeSubtasks: Boolean): Unit = update {
        if (removeSubtasks) {
            val tasksTree = getTasksTree()
            val taskDto = tasksTree.firstInTreeOrNull { task -> task.id == taskId }
            taskDto?.let { removeTaskWithSubtasksInternal(it) }
            taskDto!!
        } else {
            removeTaskInternal(taskId)
        }
    }

    private fun removeTaskWithSubtasksInternal(taskDto: TaskDto) {
        removeTaskInternal(taskDto.id)
        taskDto.subTasks.forEach { removeTaskWithSubtasksInternal(it) }
    }

    private fun removeTaskInternal(taskId: Int): TaskDto {
        val taskToRemove = localData.tasks.first { it.id == taskId }
        localData = localData.copy(
            tasks = localData.tasks.toMutableList()
                .apply { removeIf { it.id == taskId } }
                .replaceIf(
                    condition = { task -> task.parentTaskId == taskId },
                    replace = { it.copy(parentTaskId = taskToRemove.parentTaskId) }
                )
        )
        return taskToRemove
    }

    override suspend fun getAll(): List<TaskDto> {
        SchedulerDispatcher().dispatchScheduled()
        return try {
            lock.lock()
            getTasksTree()
        } finally {
            lock.unlock()
        }
    }

    override suspend fun getById(id: Int): TaskDto = synchronized {
        getTasksTree().firstInTreeOrNull { it.id == id }!!
    }

    private fun getTasksTree(): List<TaskDto> {
        val rootTasks = localData.tasks.filter { it.parentTaskId == null }
        return getTasksTree(rootTasks)
    }

    private fun getTasksTree(rootTasks: List<TaskDto>): List<TaskDto> {
        val subTasks = localData.tasks.filter { it.parentTaskId != null }.toMutableList()

        return rootTasks.fillSubTasks(subTasks)
    }

    private fun List<TaskDto>.firstInTreeOrNull(condition: (TaskDto) -> Boolean): TaskDto? {
        var result: TaskDto? = null
        forEach { task ->
            result = when {
                condition(task) -> return task
                task.subTasks.isNotEmpty() -> task.subTasks.firstInTreeOrNull(condition)
                else -> null
            }
            if (result != null) {
                return result
            }
        }
        return result
    }

    private fun List<TaskDto>.fillSubTasks(subTasks: List<TaskDto>): List<TaskDto> = map { root ->
        root.copy(subTasks = subTasks
            .filter { it.parentTaskId == root.id }
            .sortedWith(
                compareByDescending(TaskDto::isToDo)
                    .thenByDescending(TaskDto::priority)
                    .thenBy(TaskDto::modifiedTime)
            )
            .fillSubTasks(subTasks))
    }


    override suspend fun copyTask(taskId: Int) {
        var taskToCopy: TaskDto? = null
        update {
            taskToCopy = localData.tasks.firstOrNull { it.id == taskId }
        }
        taskToCopy?.let {
            create(it.toCreateTask())
        }
    }

    override suspend fun create(createTaskDto: CreateTaskDto): TaskDto = update {
        addNewTaskInternal(createTaskDto)
    }

    private fun addNewTaskInternal(createTaskDto: CreateTaskDto): TaskDto {
        val tasks = localData.tasks
        val newTaskId = createNewId(tasks)
        val newTask = createNewTask(newTaskId, createTaskDto, tasks)
        localData = localData.copy(
            tasks = tasks.toMutableList() + newTask,
        )
        return newTask
    }

    private fun createNewId(tasks: List<TaskDto>) =
        (tasks.maxOfOrNull { it.id } ?: 0).inc()

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
            getTasksTree(tasks.filter { parentTask == it.id }).firstOrNull()?.subTasks
        } ?: tasks)

    override suspend fun update(
        taskId: Int,
        task: UpdateTaskDto
    ): TaskDto = update {
        updateTaskInternal(taskId, task)
    }

    override suspend fun remove(id: Int) = removeTask(id)

    private fun updateTaskInternal(
        taskId: Int,
        task: UpdateTaskDto
    ): TaskDto {
        var updated: TaskDto? = null
        localData = localData.copy(
            tasks = localData.tasks.replaceIf(
                condition = { it.id == taskId },
                replace = { changedTask ->
                    updated = changedTask.copy(
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
                    updated!!
                }
            )
        )
        return updated!!
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

    private suspend fun <T> update(action: suspend () -> T): T =
        synchronized(finally = { updateStorage() }) {
            action()
        }

    private suspend fun <T> synchronized(finally: suspend () -> Unit = { }, action: suspend () -> T): T =
        try {
            lock.lock()
            action()
        } finally {
            finally()
            lock.unlock()
        }

    private suspend fun updateStorage() {
        fileCache.putTyped(cacheKey, localData)
    }

    private inner class SchedulerDispatcher {

        private val schedulerChecker = SchedulerChecker(
            isInStartWindow = { creationTime: LocalDateTime, today: LocalDateTime ->
                lastDateIsNotToday() && creationTime <= today
            },
            creationTimeFeatureEnabled = false
        )

        suspend fun dispatchScheduled() = update {
            localData.tasks.filter { it.scheduler != null }.forEach { task ->
                val scheduler = task.scheduler?.toDomain()
                val today = currentTime().toJavaLocalDateTime()
                if (scheduler != null) {
                    var highestLastDate: LocalDateTime? = findFirstHighestLastDate(today, scheduler)

                    if (highestLastDate != null) {
                        copyTask(task)

                        if ((scheduler as? Scheduler.OneShot)?.removeScheduled == true) {
                            val taskDtoToRemove = getTasksTree().firstInTreeOrNull { it.id == task.id }
                            taskDtoToRemove?.let { removeTaskWithSubtasksInternal(it) }
                        } else {
                            updateLastDate(task, highestLastDate)
                        }
                    }
                }
            }
        }

        private fun findFirstHighestLastDate(
            today: LocalDateTime,
            scheduler: Scheduler
        ): LocalDateTime? {
            val pastDate: LocalDateTime = today.plusWeeks(-2)
            val minDateForSchedulerCheck = listOf(
                pastDate,
                scheduler.lastDate?.toJavaLocalDateTime() ?: LocalDateTime.MIN,
                scheduler.startDate.toJavaLocalDateTime()
            ).max()

            var iterator = today
            var highestLastDate: LocalDateTime? = null
            while (iterator >= minDateForSchedulerCheck) {
                if (schedulerChecker.shouldBeCreated(scheduler, iterator)) {
                    highestLastDate = iterator
                    break
                }
                iterator = iterator.plusDays(-1)
            }
            return highestLastDate
        }

        private suspend fun copyTask(task: TaskDto, ignorePriority: Boolean = true): TaskDto {
            val createNewTask = task.toCreateTask(
                ignorePriority = ignorePriority,
                ignoreScheduler = true
            ).copy(
                highestPriorityAsDefault = task.scheduler?.highestPriorityAsDefault ?: false
            )

            val subTasks = localData.tasks
                .filter { it.parentTaskId == task.id }
                .map { copyTask(it, ignorePriority = false) }

            val newTask = addNewTaskInternal(createNewTask)
            update(newTask.id, UpdateTaskDto(isToDo = task.isToDo))

            subTasks.forEach {
                updateTaskInternal(
                    it.id,
                    UpdateTaskDto(parentTaskId = NullableFieldDto(newTask.id))
                )
            }
            return newTask
        }

        private fun Scheduler.lastDateIsNotToday(): Boolean {
            val todayLocalDate = currentTime().toJavaLocalDateTime().toLocalDate()
            return lastDate?.withStartOfDay()
                ?.let { it.toJavaLocalDateTime().toLocalDate() != todayLocalDate }
                ?: true
        }

        private fun updateLastDate(task: TaskDto, today: LocalDateTime) {
            localData = localData.copy(
                tasks = localData.tasks.replaceIf(
                    condition = { item -> item.id == task.id },
                    replace = {
                        task.copy(scheduler = task.scheduler?.copy(lastDate = today.toKotlinLocalDateTime().formatDate()))
                    }
                )
            )
        }
    }
}

private fun TaskDto.toCreateTask(
    ignorePriority: Boolean = false,
    ignoreScheduler: Boolean = false,
) = CreateTaskDto(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority.takeIf { !ignorePriority },
    highestPriorityAsDefault = false,
    scheduler = scheduler.takeIf { !ignoreScheduler }
)

private fun LocalDate.lastDayOfTheMonth() = YearMonth.of(year, month).atEndOfMonth().dayOfMonth

private fun LocalDateTime.targetDayOfMonth(targetDayOfMonth: Int): Int {
    val lastDayOfTheMonth = this.toLocalDate().lastDayOfTheMonth()
    return if (targetDayOfMonth > lastDayOfTheMonth) lastDayOfTheMonth else targetDayOfMonth
}

private fun LocalDate.findFirstDate(
    maxDate: LocalDate = currentTime().toJavaLocalDateTime().toLocalDate().plusMonths(3),
    mutate: (LocalDate) -> LocalDate = { it.plusDays(1) },
    predicate: (LocalDate) -> Boolean
): LocalDate? =
    if (predicate(this)) {
        this
    } else {
        val nextDate = mutate(this)
        if (nextDate <= maxDate) {
            nextDate.findFirstDate(maxDate, mutate, predicate)
        } else {
            null
        }
    }

private class SchedulerChecker(
    private val isInStartWindow: Scheduler.(creationTime: LocalDateTime, today: LocalDateTime) -> Boolean,
    private val creationTimeFeatureEnabled: Boolean
) {
    fun shouldBeCreated(scheduler: Scheduler, today: LocalDateTime): Boolean = with(scheduler) {
        return when (this) {
            is Scheduler.OneShot -> shouldBeCreated(today)
            is Scheduler.Monthly -> shouldBeCreated(today)
            is Scheduler.Weekly -> shouldBeCreated(today)
        }
    }

    private fun Scheduler.OneShot.shouldBeCreated(today: LocalDateTime): Boolean {
        val creationTime = startDate.toJavaLocalDateTime()
            .withHour(hour)
            .withMinute(minute)
        return lastDate == null && isInStartWindow(creationTime, today)
    }

    private fun Scheduler.Monthly.shouldBeCreated(today: LocalDateTime): Boolean {
        if (today.targetDayOfMonth(dayOfMonth) != today.dayOfMonth) {
            return false
        }

        return shouldCreate(
            today,
            calcFirstPeriodDate = { startDate: LocalDate ->
                val realDayOfMonth =
                    if (dayOfMonth > startDate.lastDayOfTheMonth()) startDate.lastDayOfTheMonth() else dayOfMonth
                val startedInNextMonth = startDate.dayOfMonth > realDayOfMonth
                startDate.withDayOfMonth(realDayOfMonth)
                    .let { if (startedInNextMonth) it.plusMonths(1) else it }
            },
            calcPeriodNumber = { firstPeriodDate, todayLocalDate ->
                Period.between(firstPeriodDate.plusDays(-1), todayLocalDate).months.inc()
            }
        )
    }

    private fun Scheduler.Weekly.shouldBeCreated(today: LocalDateTime): Boolean {
        if (daysOfWeek.isNotEmpty() && today.dayOfWeek !in daysOfWeek) {
            return false
        }

        return shouldCreate(
            today,
            calcFirstPeriodDate = { startDate: LocalDate ->
                startDate.findFirstDate { it.dayOfWeek in daysOfWeek }
                    ?.findFirstDate(
                        mutate = { it.plusDays(-1) },
                        predicate = { it.dayOfWeek == DayOfWeek.MONDAY }
                    )
            },
            calcPeriodNumber = { firstPeriodDate, todayLocalDate ->
                val firstPeriodDayOfToday = todayLocalDate.findFirstDate(
                    mutate = { it.plusDays(-1) },
                    predicate = { it.dayOfWeek == DayOfWeek.MONDAY }
                )
                val daysBetween = Period.between(firstPeriodDate, firstPeriodDayOfToday).days
                daysBetween / WEEK_DAYS_COUNT + 1
            }
        )
    }

    private fun Scheduler.shouldCreate(
        today: LocalDateTime,
        calcFirstPeriodDate: (startDate: LocalDate) -> LocalDate?,
        calcPeriodNumber: (firstPeriodDate: LocalDate, todayLocalDate: LocalDate) -> Int
    ): Boolean {
        val creationDate =
            creationDate?.toJavaLocalDateTime()?.takeIf { creationTimeFeatureEnabled } ?: LocalDateTime.MIN
        val startDateWithHour = startDate.toJavaLocalDateTime().withHour(hour).withMinute(minute)
        val startDate = if (creationDate > startDateWithHour) {
            if (creationDate.toLocalDate() == startDateWithHour.toLocalDate()) {
                creationDate.plusDays(1)
            } else {
                creationDate
            }
        } else {
            startDateWithHour
        }.toLocalDate()
        val todayLocalDate = today.toLocalDate()

        val firstPeriodDate = calcFirstPeriodDate(startDate) ?: return false

        if (firstPeriodDate <= todayLocalDate) {
            val periodNumber = calcPeriodNumber(firstPeriodDate, todayLocalDate)

            val isRightPeriod = (periodNumber - 1).mod(repeatInEveryPeriod) == 0
            val isInCountLimit = repeatCount.takeIf { it > 0 }?.let { maxCount ->
                (periodNumber - 1) / repeatInEveryPeriod.toFloat() + 1 <= maxCount
            } ?: true

            if (isRightPeriod && isInCountLimit) {
                val creationTime = today.withHour(hour).withMinute(minute)
                return isInStartWindow(creationTime, today)
            }
        }
        return false
    }

    companion object {
        private const val WEEK_DAYS_COUNT = 7
    }
}
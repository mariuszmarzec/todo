package com.marzec.todo.model

import com.marzec.time.formatDate
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.SchedulerDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.UpdateTaskDto
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

data class Task(
    val id: Int,
    val description: String,
    val addedTime: LocalDateTime,
    val modifiedTime: LocalDateTime,
    val parentTaskId: Int?,
    val subTasks: List<Task>,
    val isToDo: Boolean,
    val priority: Int,
    val scheduler: Scheduler?
)

fun SchedulerDto.toDomain(): Scheduler = when (type) {
    Scheduler.OneShot::class.simpleName -> Scheduler.OneShot(
        hour = hour,
        minute = minute,
        startDate = startDate.toLocalDateTime(),
        lastDate = lastDate.toLocalDateTime(),
    )
    Scheduler.Weekly::class.simpleName -> Scheduler.Weekly(
        hour = hour,
        minute = minute,
        startDate = startDate.toLocalDateTime(),
        lastDate = lastDate.toLocalDateTime(),
        daysOfWeek = daysOfWeek.map { DayOfWeek(it) },
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
    )
    Scheduler.Monthly::class.simpleName -> Scheduler.Monthly(
        hour = hour, minute = minute, startDate = startDate.toLocalDateTime(),
        lastDate = lastDate.toLocalDateTime(),
        dayOfMonth = dayOfMonth,
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
    )
    else -> throw IllegalArgumentException("Unknown type of scheduler")
}

sealed class Scheduler(
    open val hour: Int,
    open val minute: Int,
    open val startDate: LocalDateTime,
    open val lastDate: LocalDateTime,
    open val repeatCount: Int = DEFAULT_REPEAT_COUNT,
    open val repeatInEveryPeriod: Int = DEFAULT_REPEAT_IN_EVERY_PERIOD
) {
    data class OneShot(
        override val hour: Int,
        override val minute: Int,
        override val startDate: LocalDateTime,
        override val lastDate: LocalDateTime,
        override val repeatCount: Int = DEFAULT_REPEAT_COUNT,
        override val repeatInEveryPeriod: Int = DEFAULT_REPEAT_IN_EVERY_PERIOD
    ) : Scheduler(
        hour, minute, startDate, lastDate, repeatCount, repeatInEveryPeriod
    )

    data class Weekly(
        override val hour: Int,
        override val minute: Int,
        override val startDate: LocalDateTime,
        val daysOfWeek: List<DayOfWeek>,
        override val lastDate: LocalDateTime,
        override val repeatCount: Int = DEFAULT_REPEAT_COUNT,
        override val repeatInEveryPeriod: Int = DEFAULT_REPEAT_IN_EVERY_PERIOD
    ) : Scheduler(
        hour, minute, startDate, lastDate, repeatCount, repeatInEveryPeriod
    )

    data class Monthly(
        override val hour: Int,
        override val minute: Int,
        override val startDate: LocalDateTime,
        val dayOfMonth: Int,
        override val lastDate: LocalDateTime,
        override val repeatCount: Int = DEFAULT_REPEAT_COUNT,
        override val repeatInEveryPeriod: Int = DEFAULT_REPEAT_IN_EVERY_PERIOD
    ) : Scheduler(
        hour, minute, startDate, lastDate, repeatCount, repeatInEveryPeriod
    )

    companion object {
        val DEFAULT_REPEAT_COUNT = -1
        val DEFAULT_REPEAT_IN_EVERY_PERIOD = 1
    }
}

fun Scheduler.toDto(): SchedulerDto = when (this) {
    is Scheduler.OneShot -> SchedulerDto(
        hour = hour,
        minute = minute,
        startDate = startDate.formatDate(),
        lastDate = lastDate.formatDate(),
        daysOfWeek = emptyList(),
        dayOfMonth = 0,
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
        type = this::class.simpleName.orEmpty()
    )
    is Scheduler.Weekly -> SchedulerDto(
        hour = hour,
        minute = minute,
        startDate = startDate.formatDate(),
        lastDate = lastDate.formatDate(),
        daysOfWeek = daysOfWeek.map { it.isoDayNumber },
        dayOfMonth = 0,
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
        type = this::class.simpleName.orEmpty()
    )
    is Scheduler.Monthly -> SchedulerDto(
        hour = hour,
        minute = minute,
        startDate = startDate.formatDate(),
        lastDate = lastDate.formatDate(),
        daysOfWeek = emptyList(),
        dayOfMonth = dayOfMonth,
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
        type = this::class.simpleName.orEmpty()
    )
}

fun Task.toDto(): TaskDto = TaskDto(
    id = id,
    description = description,
    addedTime = addedTime.toString(),
    modifiedTime = modifiedTime.toString(),
    parentTaskId = parentTaskId,
    subTasks = subTasks.map { it.toDto() },
    isToDo = isToDo,
    priority = priority,
    scheduler = scheduler?.toDto()
)

fun TaskDto.toDomain(): Task = Task(
    id = this.id,
    description = description,
    addedTime = LocalDateTime.parse(addedTime),
    modifiedTime = LocalDateTime.parse(modifiedTime),
    parentTaskId = parentTaskId,
    subTasks = subTasks.map { it.toDomain() },
    isToDo = isToDo,
    priority = priority,
    scheduler = scheduler?.toDomain()
)

data class CreateTask(
    val description: String,
    val parentTaskId: Int?,
    val priority: Int? = null,
    val highestPriorityAsDefault: Boolean? = null,
    val scheduler: Scheduler? = null
)

fun CreateTaskDto.toDomain() = CreateTask(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
    scheduler = scheduler?.toDomain()
)

fun CreateTask.toDto() = CreateTaskDto(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
    highestPriorityAsDefault = highestPriorityAsDefault,
    scheduler = scheduler?.toDto()
)

data class UpdateTask(
    val description: String,
    val parentTaskId: Int?,
    val priority: Int,
    val isToDo: Boolean,
    val scheduler: Scheduler? = null
)

fun UpdateTaskDto.toDomain() = UpdateTask(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
    isToDo = isToDo,
    scheduler = scheduler?.toDomain()
)

fun UpdateTask.toDto() = UpdateTaskDto(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
    isToDo = isToDo,
    scheduler = scheduler?.toDto()
)

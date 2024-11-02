package com.marzec.todo.model

import com.marzec.model.NullableField
import com.marzec.model.toDto
import com.marzec.time.formatDate
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.SchedulerDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.UpdateTaskDto
import kotlin.reflect.KProperty1
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime

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
        creationDate = creationDate?.toLocalDateTime(),
        startDate = startDate.toLocalDateTime(),
        lastDate = lastDate?.toLocalDateTime(),
        highestPriorityAsDefault = highestPriorityAsDefault,
        removeScheduled = removeScheduled
    )
    Scheduler.Weekly::class.simpleName -> Scheduler.Weekly(
        hour = hour,
        minute = minute,
        creationDate = creationDate?.toLocalDateTime(),
        startDate = startDate.toLocalDateTime(),
        lastDate = lastDate?.toLocalDateTime(),
        daysOfWeek = daysOfWeek.map { DayOfWeek(it) },
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
        highestPriorityAsDefault = highestPriorityAsDefault
    )
    Scheduler.Monthly::class.simpleName -> Scheduler.Monthly(
        hour = hour,
        minute = minute,
        creationDate = creationDate?.toLocalDateTime(),
        startDate = startDate.toLocalDateTime(),
        lastDate = lastDate?.toLocalDateTime(),
        dayOfMonth = dayOfMonth,
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
        highestPriorityAsDefault = highestPriorityAsDefault
    )
    else -> throw IllegalArgumentException("Unknown type of scheduler")
}

val SchedulerDto.highestPriorityAsDefault: Boolean
    get() = getHighestPriorityAsDefault(options)

val SchedulerDto.removeScheduled: Boolean
    get() = getRemoveScheduled(options)

fun getHighestPriorityAsDefault(options: Map<String, String>?) =
    options?.get(Scheduler::highestPriorityAsDefault.name)?.toBooleanStrictOrNull()
        ?: Scheduler.HIGHEST_PRIORITY_AS_DEFAULT

fun getRemoveScheduled(options: Map<String, String>?) =
    options?.get(Scheduler.OneShot::removeScheduled.name)?.toBooleanStrictOrNull()
        ?: Scheduler.REMOVE_SCHEDULED

sealed class Scheduler(
    open val hour: Int,
    open val minute: Int,
    open val creationDate: LocalDateTime?,
    open val startDate: LocalDateTime,
    open val lastDate: LocalDateTime?,
    open val repeatCount: Int = DEFAULT_REPEAT_COUNT,
    open val repeatInEveryPeriod: Int = DEFAULT_REPEAT_IN_EVERY_PERIOD,
    open val highestPriorityAsDefault: Boolean
) {
    data class OneShot(
        override val hour: Int,
        override val minute: Int,
        override val creationDate: LocalDateTime?,
        override val startDate: LocalDateTime,
        override val lastDate: LocalDateTime?,
        override val repeatCount: Int = DEFAULT_REPEAT_COUNT,
        override val repeatInEveryPeriod: Int = DEFAULT_REPEAT_IN_EVERY_PERIOD,
        override val highestPriorityAsDefault: Boolean = HIGHEST_PRIORITY_AS_DEFAULT,
        val removeScheduled: Boolean = REMOVE_SCHEDULED
    ) : Scheduler(
        hour, minute, creationDate, startDate, lastDate, repeatCount, repeatInEveryPeriod, highestPriorityAsDefault
    )

    data class Weekly(
        override val hour: Int,
        override val minute: Int,
        override val creationDate: LocalDateTime?,
        override val startDate: LocalDateTime,
        val daysOfWeek: List<DayOfWeek>,
        override val lastDate: LocalDateTime?,
        override val repeatCount: Int = DEFAULT_REPEAT_COUNT,
        override val repeatInEveryPeriod: Int = DEFAULT_REPEAT_IN_EVERY_PERIOD,
        override val highestPriorityAsDefault: Boolean = HIGHEST_PRIORITY_AS_DEFAULT
    ) : Scheduler(
        hour, minute, creationDate, startDate, lastDate, repeatCount, repeatInEveryPeriod, highestPriorityAsDefault
    )

    data class Monthly(
        override val hour: Int,
        override val minute: Int,
        override val creationDate: LocalDateTime?,
        override val startDate: LocalDateTime,
        val dayOfMonth: Int,
        override val lastDate: LocalDateTime?,
        override val repeatCount: Int = DEFAULT_REPEAT_COUNT,
        override val repeatInEveryPeriod: Int = DEFAULT_REPEAT_IN_EVERY_PERIOD,
        override val highestPriorityAsDefault: Boolean = HIGHEST_PRIORITY_AS_DEFAULT
    ) : Scheduler(
        hour, minute, creationDate, startDate, lastDate, repeatCount, repeatInEveryPeriod, highestPriorityAsDefault
    )

    companion object {
        const val DEFAULT_REPEAT_COUNT = -1
        const val DEFAULT_REPEAT_IN_EVERY_PERIOD = 1
        const val HIGHEST_PRIORITY_AS_DEFAULT = false
        const val REMOVE_SCHEDULED = false
    }
}

private fun Scheduler.optionsToMap(): Map<String, String>? =
    listOfNotNull(
        takeIfNotDefault(Scheduler::highestPriorityAsDefault, Scheduler.HIGHEST_PRIORITY_AS_DEFAULT)
    ).toMap()
        .takeIf { it.isNotEmpty() }

private fun Scheduler.OneShot.optionsToMap(): Map<String, String>? =
    listOfNotNull(
        takeIfNotDefault(Scheduler::highestPriorityAsDefault, Scheduler.HIGHEST_PRIORITY_AS_DEFAULT),
        takeIfNotDefault(Scheduler.OneShot::removeScheduled, Scheduler.REMOVE_SCHEDULED),
    ).toMap()
        .takeIf { it.isNotEmpty() }

private fun <RECEIVER, VALUE> RECEIVER.takeIfNotDefault(
    kProperty: KProperty1<RECEIVER, VALUE>,
    defaultValue: VALUE
): Pair<String, String>? {
    return kProperty.get(this).takeIf { it != defaultValue }?.let { kProperty.name to it.toString() }
}

fun Scheduler.toDto(): SchedulerDto = when (this) {
    is Scheduler.OneShot -> SchedulerDto(
        hour = hour,
        minute = minute,
        startDate = startDate.formatDate(),
        lastDate = lastDate?.formatDate(),
        daysOfWeek = emptyList(),
        dayOfMonth = 0,
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
        type = this::class.simpleName.orEmpty(),
        options = optionsToMap()
    )
    is Scheduler.Weekly -> SchedulerDto(
        hour = hour,
        minute = minute,
        startDate = startDate.formatDate(),
        lastDate = lastDate?.formatDate(),
        daysOfWeek = daysOfWeek.map { it.isoDayNumber },
        dayOfMonth = 0,
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
        type = this::class.simpleName.orEmpty(),
        options = optionsToMap()
    )
    is Scheduler.Monthly -> SchedulerDto(
        hour = hour,
        minute = minute,
        startDate = startDate.formatDate(),
        lastDate = lastDate?.formatDate(),
        daysOfWeek = emptyList(),
        dayOfMonth = dayOfMonth,
        repeatInEveryPeriod = repeatInEveryPeriod,
        repeatCount = repeatCount,
        type = this::class.simpleName.orEmpty(),
        options = optionsToMap()
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
    val description: String? = null,
    val parentTaskId: NullableField<Int>? = null,
    val priority: Int? = null,
    val isToDo: Boolean? = null,
    val scheduler: NullableField<Scheduler>? = null
)

fun UpdateTask.toDto() = UpdateTaskDto(
    description = description,
    parentTaskId = parentTaskId?.toDto(),
    priority = priority,
    isToDo = isToDo,
    scheduler = scheduler?.toDto{ it?.toDto() }
)

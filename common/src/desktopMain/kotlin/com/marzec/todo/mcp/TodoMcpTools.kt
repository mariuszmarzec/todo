package com.marzec.todo.mcp

import com.marzec.content.Content
import com.marzec.model.NullableField
import com.marzec.todo.BuildKonfig
import com.marzec.todo.DI
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task
import com.marzec.todo.model.TaskShare
import com.marzec.todo.model.UpdateTask
import com.marzec.todo.repository.TodoRepository
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

internal class TodoMcpTools(
    private val repository: TodoRepository,
    private val isLocalStorageMode: Boolean = BuildKonfig.ENVIRONMENT == "m"
) {
    fun register(server: Server) {
        registerTool(
            server,
            "list_tasks",
            "List active tasks with their subtasks. Scheduled tasks are excluded.",
            properties = buildJsonObject {}
        ) {
            repository.observeTasks().awaitResult().toJson()
        }

        registerTool(
            server,
            "list_scheduled_tasks",
            "List scheduled tasks with their subtasks.",
            properties = buildJsonObject {}
        ) {
            repository.observeScheduledTasks().awaitResult().toJson()
        }

        registerTool(
            server,
            "get_task",
            "Return a task and its complete subtask tree by ID.",
            required = listOf("taskId"),
            properties = buildJsonObject {
                put("taskId", intProperty("Task ID", minimum = 1))
            }
        ) { args ->
            repository.observeTask(args.requiredInt("taskId")).awaitResult().let(::taskToJson)
        }

        registerTool(
            server,
            "create_task",
            "Create one task. If parentTaskId is provided, create it as a subtask. Returns the exact created taskId. Optional shares require remote mode and the task-sharing feature.",
            required = listOf("description"),
            properties = buildJsonObject {
                put("description", stringProperty("Non-empty task description"))
                put("parentTaskId", intProperty("Optional parent task ID", minimum = 1))
                put("highestPriorityAsDefault", booleanProperty("Whether to use the app's highest-priority default"))
                put("expirationDate", dateProperty("Optional local date-time in ISO format"))
                put("scheduler", schedulerProperty())
                put("shares", sharesProperty())
            }
        ) { args ->
            val shares = args.optionalShares()
            if (args.containsKey("shares")) validateSharingAvailable()
            val taskId = repository.addNewTaskAndGetId(
                description = args.requiredString("description"),
                parentTaskId = args.optionalInt("parentTaskId"),
                highestPriorityAsDefault = args.optionalBoolean("highestPriorityAsDefault") ?: false,
                scheduler = args.optionalScheduler(),
                expirationDate = args.optionalDate("expirationDate"),
                shares = shares
            ).awaitResult()
            successJson("taskId", taskId)
        }

        registerTool(
            server,
            "create_tasks",
            "Create multiple sibling tasks with the same optional parent, schedule and expiration date.",
            required = listOf("descriptions"),
            properties = buildJsonObject {
                put("descriptions", arrayProperty(stringProperty("Task description"), "At least one non-empty task description"))
                put("parentTaskId", intProperty("Optional parent task ID", minimum = 1))
                put("highestPriorityAsDefault", booleanProperty("Whether to use the app's highest-priority default"))
                put("expirationDate", dateProperty("Optional local date-time in ISO format"))
                put("scheduler", schedulerProperty())
            }
        ) { args ->
            val descriptions = args.requiredStringList("descriptions")
            if (descriptions.isEmpty()) throw IllegalArgumentException("descriptions must contain at least one task")
            repository.addNewTasks(
                highestPriorityAsDefault = args.optionalBoolean("highestPriorityAsDefault") ?: false,
                parentTaskId = args.optionalInt("parentTaskId"),
                descriptions = descriptions,
                scheduler = args.optionalScheduler(),
                expirationDate = args.optionalDate("expirationDate")
            ).awaitResult()
            successJson()
        }

        registerTool(
            server,
            "create_task_tree",
            "Create a task tree from newline-separated descriptions. Use an unprefixed line for a root and add one leading dash per nesting level, for example: root, - child, -- grandchild.",
            required = listOf("tree"),
            properties = buildJsonObject {
                put("tree", stringProperty("Newline-separated task tree"))
                put("parentTaskId", intProperty("Optional parent ID for the root task", minimum = 1))
                put("highestPriorityAsDefault", booleanProperty("Whether to use the app's highest-priority default"))
                put("expirationDate", dateProperty("Optional local date-time in ISO format"))
                put("scheduler", schedulerProperty())
            }
        ) { args ->
            val tree = args.requiredString("tree")
            if (tree.lines().none { it.isNotBlank() }) throw IllegalArgumentException("tree must contain at least one task")
            repository.createTaskTree(
                description = tree,
                parentTaskId = args.optionalInt("parentTaskId"),
                highestPriorityAsDefault = args.optionalBoolean("highestPriorityAsDefault") ?: false,
                schedulerWithOptions = args.optionalScheduler(),
                expirationDate = args.optionalDate("expirationDate")
            ).awaitResult()
            successJson()
        }

        registerTool(
            server,
            "update_task",
            "Update provided fields of a task. Omitted fields remain unchanged; parentTaskId, scheduler and expirationDate may be explicitly set to null to clear them.",
            required = listOf("taskId"),
            properties = buildJsonObject {
                put("taskId", intProperty("Task ID", minimum = 1))
                put("description", stringProperty("Replacement task description"))
                put("parentTaskId", nullableIntProperty("New parent task ID, or null to make this a root task"))
                put("priority", intProperty("Priority value"))
                put("isToDo", booleanProperty("Whether the task is still to-do"))
                put("scheduler", nullableSchedulerProperty())
                put("expirationDate", nullableDateProperty("ISO local date-time, or null to clear"))
                put("shares", sharesProperty())
            }
        ) { args ->
            val updateKeys = setOf("description", "parentTaskId", "priority", "isToDo", "scheduler", "expirationDate", "shares")
            if (updateKeys.none(args::containsKey)) throw IllegalArgumentException("At least one task field must be provided")
            val shares = if (args.containsKey("shares")) args.optionalShares() ?: emptyList() else null
            if (args.containsKey("shares")) validateSharingAvailable()
            val update = UpdateTask(
                description = args.optionalString("description"),
                parentTaskId = if (args.containsKey("parentTaskId")) NullableField(args.optionalInt("parentTaskId")) else null,
                priority = args.optionalSignedInt("priority"),
                isToDo = args.optionalBoolean("isToDo"),
                scheduler = if (args.containsKey("scheduler")) NullableField(args.optionalScheduler()) else null,
                expirationDate = if (args.containsKey("expirationDate")) NullableField(args.optionalDate("expirationDate")) else null,
                shares = shares
            )
            repository.updateTask(args.requiredInt("taskId"), update).awaitResult()
            successJson()
        }

        registerTool(
            server,
            "mark_tasks_done",
            "Mark one or more tasks as done.",
            required = listOf("taskIds"),
            properties = buildJsonObject {
                put("taskIds", arrayProperty(intProperty("Positive task ID", minimum = 1), "Task IDs to mark done"))
            }
        ) { args ->
            val ids = args.requiredIdList("taskIds")
            repository.markAsDone(ids).awaitResult()
            successJson("updated", ids.size)
        }

        registerTool(
            server,
            "mark_tasks_to_do",
            "Mark one or more tasks as to-do again.",
            required = listOf("taskIds"),
            properties = buildJsonObject {
                put("taskIds", arrayProperty(intProperty("Positive task ID", minimum = 1), "Task IDs to mark to-do"))
            }
        ) { args ->
            val ids = args.requiredIdList("taskIds")
            repository.markAsToDo(ids).awaitResult()
            successJson("updated", ids.size)
        }

        registerTool(
            server,
            "reorder_tasks",
            "Reorder tasks by priority using the supplied task ID order. This changes task priorities.",
            required = listOf("taskIds"),
            properties = buildJsonObject {
                put("taskIds", arrayProperty(intProperty("Positive task ID", minimum = 1), "Task IDs in the intended display order"))
            }
        ) { args ->
            val ids = args.requiredIdList("taskIds")
            val tasks = ids.map { repository.observeTask(it).awaitResult() }
            repository.reorderByPriority(tasks).awaitResult()
            successJson("reordered", ids.size)
        }

        registerTool(
            server,
            "schedule_tasks",
            "Apply a schedule to one or more tasks. This changes their scheduling configuration.",
            required = listOf("taskIds", "scheduler"),
            properties = buildJsonObject {
                put("taskIds", arrayProperty(intProperty("Positive task ID", minimum = 1), "Task IDs to schedule"))
                put("scheduler", schedulerProperty())
            }
        ) { args ->
            val ids = args.requiredIdList("taskIds")
            val scheduler = args.requiredScheduler("scheduler")
            val tasks = ids.map { repository.observeTask(it).awaitResult() }
            repository.schedule(tasks, scheduler).awaitResult()
            successJson("scheduled", ids.size)
        }

        registerTool(
            server,
            "copy_task",
            "Copy a task, including the subtask tree where supported by the selected data source.",
            required = listOf("taskId"),
            properties = buildJsonObject {
                put("taskId", intProperty("Task ID to copy", minimum = 1))
            }
        ) { args ->
            repository.copyTask(args.requiredInt("taskId")).awaitResult()
            successJson()
        }

        registerTool(
            server,
            "remove_task",
            "Remove a task. This is destructive. With removeSubtasks=false, the app's repository behavior for preserving/reparenting subtasks applies; with true, the whole subtask tree is removed when permissions allow.",
            required = listOf("taskId"),
            properties = buildJsonObject {
                put("taskId", intProperty("Task ID to remove", minimum = 1))
                put("removeSubtasks", booleanProperty("Whether to remove all descendants too; defaults to false"))
            }
        ) { args ->
            val taskId = args.requiredInt("taskId")
            val task = repository.observeTask(taskId).awaitResult()
            if (args.optionalBoolean("removeSubtasks") == true) {
                repository.removeTaskWithSubtasks(task).awaitResult()
            } else {
                repository.removeTask(taskId).awaitResult()
            }
            successJson()
        }

        registerTool(
            server,
            "leave_shared_task",
            "Leave a task shared with the current user. This is unsupported in local-storage/mock mode.",
            required = listOf("taskId"),
            properties = buildJsonObject {
                put("taskId", intProperty("Task ID of the shared task", minimum = 1))
            }
        ) { args ->
            requireRemoteSharing("Leaving shared tasks")
            repository.leaveShare(args.requiredInt("taskId")).awaitResult()
            successJson()
        }

        registerTool(
            server,
            "list_shareable_users",
            "List users that can be selected for task sharing. This is unsupported in local-storage/mock mode.",
            properties = buildJsonObject {}
        ) {
            validateSharingAvailable()
            JsonArray(repository.getUsers().awaitResult().map { user ->
                buildJsonObject {
                    put("id", user.id)
                    put("email", user.email)
                }
            })
        }
    }

    private fun validateSharingAvailable() {
        requireRemoteSharing("Task sharing")
        if (!DI.featureTogglesManager.get("todo.taskSharing")) {
            throw UnsupportedOperationException("Task sharing is disabled by the todo.taskSharing feature toggle")
        }
    }

    private fun requireRemoteSharing(operation: String) {
        if (isLocalStorageMode) {
            throw UnsupportedOperationException("$operation is not supported in local-storage/mock mode")
        }
    }

    private fun registerTool(
        server: Server,
        name: String,
        description: String,
        required: List<String> = emptyList(),
        properties: JsonObject,
        action: suspend (JsonObject) -> JsonElement
    ) {
        server.addTool(
            name = name,
            description = description,
            inputSchema = ToolSchema(properties = properties, required = required)
        ) { request ->
            try {
                val result = action(request.arguments ?: buildJsonObject {})
                CallToolResult(content = listOf(TextContent(result.toString())))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                CallToolResult(
                    content = listOf(TextContent(error.message ?: "Todo MCP operation failed")),
                    isError = true
                )
            }
        }
    }
}

private suspend fun <T> Flow<Content<T>>.awaitResult(): T {
    val completed = first { it !is Content.Loading }
    return when (completed) {
        is Content.Data -> completed.data
        is Content.Error -> throw IllegalStateException(completed.exception.message ?: "Todo operation failed", completed.exception)
        is Content.Loading -> throw IllegalStateException("Todo operation did not finish")
    }
}

private fun JsonObject.requiredString(name: String): String {
    val value = this[name] ?: throw IllegalArgumentException("$name is required")
    val primitive = value as? JsonPrimitive ?: throw IllegalArgumentException("$name must be a string")
    if (!primitive.isString) throw IllegalArgumentException("$name must be a string")
    return primitive.content.trim().takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("$name must not be blank")
}

private fun JsonObject.optionalString(name: String): String? {
    val value = this[name] ?: return null
    if (value == JsonNull) return null
    val primitive = value as? JsonPrimitive ?: throw IllegalArgumentException("$name must be a string or null")
    if (!primitive.isString) throw IllegalArgumentException("$name must be a string or null")
    return primitive.content.trim().takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("$name must not be blank")
}

private fun JsonObject.requiredInt(name: String): Int {
    val value = this[name] as? JsonPrimitive ?: throw IllegalArgumentException("$name must be an integer")
    if (value.isString) throw IllegalArgumentException("$name must be an integer")
    val intValue = value.intOrNull ?: throw IllegalArgumentException("$name must be an integer")
    if (intValue < 1) throw IllegalArgumentException("$name must be a positive integer")
    return intValue
}

private fun JsonObject.optionalInt(name: String): Int? {
    val value = this[name] ?: return null
    if (value == JsonNull) return null
    val primitive = value as? JsonPrimitive ?: throw IllegalArgumentException("$name must be an integer or null")
    if (primitive.isString) throw IllegalArgumentException("$name must be an integer or null")
    val intValue = primitive.intOrNull ?: throw IllegalArgumentException("$name must be an integer or null")
    if (intValue < 1) throw IllegalArgumentException("$name must be a positive integer")
    return intValue
}

private fun JsonObject.optionalSignedInt(name: String): Int? {
    val value = this[name] ?: return null
    if (value == JsonNull) return null
    val primitive = value as? JsonPrimitive ?: throw IllegalArgumentException("$name must be an integer")
    if (primitive.isString) throw IllegalArgumentException("$name must be an integer")
    return primitive.intOrNull ?: throw IllegalArgumentException("$name must be an integer")
}

private fun JsonObject.optionalBoolean(name: String): Boolean? {
    val value = this[name] ?: return null
    if (value == JsonNull) return null
    val primitive = value as? JsonPrimitive ?: throw IllegalArgumentException("$name must be a boolean")
    if (primitive.isString) throw IllegalArgumentException("$name must be a boolean")
    return primitive.booleanOrNull ?: throw IllegalArgumentException("$name must be a boolean")
}

private fun JsonObject.requiredStringList(name: String): List<String> {
    val list = this[name] as? JsonArray ?: throw IllegalArgumentException("$name must be an array")
    return list.mapIndexed { index, value ->
        val primitive = value as? JsonPrimitive ?: throw IllegalArgumentException("$name[$index] must be a string")
        if (!primitive.isString) throw IllegalArgumentException("$name[$index] must be a string")
        primitive.content.trim().takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("$name[$index] must not be blank")
    }
}

private fun JsonObject.requiredIdList(name: String): List<Int> {
    val list = this[name] as? JsonArray ?: throw IllegalArgumentException("$name must be an array")
    if (list.isEmpty()) throw IllegalArgumentException("$name must contain at least one ID")
    val ids = list.mapIndexed { index, value ->
        val primitive = value as? JsonPrimitive ?: throw IllegalArgumentException("$name[$index] must be an integer")
        if (primitive.isString) throw IllegalArgumentException("$name[$index] must be an integer")
        val id = primitive.intOrNull ?: throw IllegalArgumentException("$name[$index] must be an integer")
        if (id < 1) throw IllegalArgumentException("$name[$index] must be a positive integer")
        id
    }
    if (ids.distinct().size != ids.size) throw IllegalArgumentException("$name must not contain duplicate IDs")
    return ids
}

private fun JsonObject.optionalDate(name: String): LocalDateTime? {
    val value = this[name] ?: return null
    if (value == JsonNull) return null
    val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: throw IllegalArgumentException("$name must be an ISO local date-time string or null")
    return try {
        LocalDateTime.parse(text)
    } catch (error: IllegalArgumentException) {
        throw IllegalArgumentException("$name must be a valid ISO local date-time")
    }
}

private fun JsonObject.optionalScheduler(): Scheduler? = when (val element = this["scheduler"]) {
    null, JsonNull -> null
    else -> parseScheduler(element)
}

private fun JsonObject.requiredScheduler(name: String): Scheduler {
    val value = this[name] ?: throw IllegalArgumentException("$name is required")
    return parseScheduler(value) ?: throw IllegalArgumentException("$name must be a scheduler object")
}

private fun parseScheduler(element: JsonElement): Scheduler {
    val args = element as? JsonObject ?: throw IllegalArgumentException("scheduler must be an object or null")
    val type = args.requiredString("type").lowercase()
    val hour = args.requiredIntAllowZero("hour")
    val minute = args.requiredIntAllowZero("minute")
    if (hour !in 0..23) throw IllegalArgumentException("scheduler.hour must be between 0 and 23")
    if (minute !in 0..59) throw IllegalArgumentException("scheduler.minute must be between 0 and 59")
    val startDate = args.optionalDate("startDate")
        ?: throw IllegalArgumentException("scheduler.startDate is required")
    val lastDate = args.optionalDate("lastDate")
    val repeatCount = args.optionalIntAllowSentinel("repeatCount") ?: Scheduler.DEFAULT_REPEAT_COUNT
    if (repeatCount == 0 || repeatCount < -1) throw IllegalArgumentException("scheduler.repeatCount must be -1 or positive")
    val repeatEvery = args.optionalIntAllowZero("repeatInEveryPeriod") ?: Scheduler.DEFAULT_REPEAT_IN_EVERY_PERIOD
    if (repeatEvery < 1) throw IllegalArgumentException("scheduler.repeatInEveryPeriod must be positive")
    val highestPriority = args.optionalBoolean("highestPriorityAsDefault") ?: Scheduler.HIGHEST_PRIORITY_AS_DEFAULT
    val showNotification = args.optionalBoolean("showNotification") ?: Scheduler.SHOW_NOTIFICATION

    return when (type) {
        "oneshot", "one_shot" -> Scheduler.OneShot(
            hour = hour,
            minute = minute,
            creationDate = null,
            startDate = startDate,
            lastDate = lastDate,
            repeatCount = repeatCount,
            repeatInEveryPeriod = repeatEvery,
            highestPriorityAsDefault = highestPriority,
            removeScheduled = args.optionalBoolean("removeScheduled") ?: Scheduler.REMOVE_SCHEDULED,
            showNotification = showNotification
        )
        "weekly" -> {
            val days = args.requiredIdList("daysOfWeek")
            if (days.any { it !in 1..7 }) throw IllegalArgumentException("scheduler.daysOfWeek values must be ISO weekday numbers from 1 to 7")
            Scheduler.Weekly(
                hour = hour,
                minute = minute,
                creationDate = null,
                startDate = startDate,
                daysOfWeek = days.map(::DayOfWeek),
                lastDate = lastDate,
                repeatCount = repeatCount,
                repeatInEveryPeriod = repeatEvery,
                highestPriorityAsDefault = highestPriority,
                showNotification = showNotification
            )
        }
        "monthly" -> {
            val day = args.requiredIntAllowZero("dayOfMonth")
            if (day !in 1..31) throw IllegalArgumentException("scheduler.dayOfMonth must be between 1 and 31")
            Scheduler.Monthly(
                hour = hour,
                minute = minute,
                creationDate = null,
                startDate = startDate,
                dayOfMonth = day,
                lastDate = lastDate,
                repeatCount = repeatCount,
                repeatInEveryPeriod = repeatEvery,
                highestPriorityAsDefault = highestPriority,
                showNotification = showNotification
            )
        }
        else -> throw IllegalArgumentException("scheduler.type must be OneShot, Weekly or Monthly")
    }
}

private fun JsonObject.requiredIntAllowZero(name: String): Int {
    val primitive = this[name] as? JsonPrimitive ?: throw IllegalArgumentException("$name must be an integer")
    if (primitive.isString) throw IllegalArgumentException("$name must be an integer")
    return primitive.intOrNull ?: throw IllegalArgumentException("$name must be an integer")
}

private fun JsonObject.optionalIntAllowZero(name: String): Int? {
    val value = this[name] ?: return null
    if (value == JsonNull) return null
    val primitive = value as? JsonPrimitive ?: throw IllegalArgumentException("$name must be an integer")
    if (primitive.isString) throw IllegalArgumentException("$name must be an integer")
    return primitive.intOrNull ?: throw IllegalArgumentException("$name must be an integer")
}

private fun JsonObject.optionalIntAllowSentinel(name: String): Int? {
    val value = this[name] ?: return null
    if (value == JsonNull) return null
    val primitive = value as? JsonPrimitive ?: throw IllegalArgumentException("$name must be an integer")
    if (primitive.isString) throw IllegalArgumentException("$name must be an integer")
    return primitive.intOrNull ?: throw IllegalArgumentException("$name must be an integer")
}

private fun JsonObject.optionalShares(): List<TaskShare>? {
    val value = this["shares"] ?: return null
    if (value == JsonNull) return emptyList()
    val items = value as? JsonArray ?: throw IllegalArgumentException("shares must be an array")
    return items.mapIndexed { index, item ->
        val share = item as? JsonObject ?: throw IllegalArgumentException("shares[$index] must be an object")
        val userId = share.requiredString("userId")
        val permission = share.requiredString("permission")
        if (permission !in setOf("VIEWER", "EDITOR_AND_VIEWER")) {
            throw IllegalArgumentException("shares[$index].permission must be VIEWER or EDITOR_AND_VIEWER")
        }
        TaskShare(userId = userId, permission = permission)
    }
}

private fun taskToJson(task: Task): JsonObject = buildJsonObject {
    put("id", task.id)
    put("description", task.description)
    put("addedTime", task.addedTime.toString())
    put("modifiedTime", task.modifiedTime.toString())
    put("parentTaskId", task.parentTaskId?.let(::JsonPrimitive) ?: JsonNull)
    put("isToDo", task.isToDo)
    put("priority", task.priority)
    put("ownerId", task.ownerId?.let(::JsonPrimitive) ?: JsonNull)
    put("expirationDate", task.expirationDate?.toString()?.let(::JsonPrimitive) ?: JsonNull)
    put("scheduler", task.scheduler?.let(::schedulerToJson) ?: JsonNull)
    put("shares", JsonArray(task.shares.map { share ->
        buildJsonObject {
            put("userId", share.userId)
            put("permission", share.permission)
        }
    }))
    put("subTasks", JsonArray(task.subTasks.map(::taskToJson)))
}

private fun schedulerToJson(scheduler: Scheduler): JsonObject = buildJsonObject {
    put("hour", scheduler.hour)
    put("minute", scheduler.minute)
    put("creationDate", scheduler.creationDate?.toString()?.let(::JsonPrimitive) ?: JsonNull)
    put("startDate", scheduler.startDate.toString())
    put("lastDate", scheduler.lastDate?.toString()?.let(::JsonPrimitive) ?: JsonNull)
    put("repeatCount", scheduler.repeatCount)
    put("repeatInEveryPeriod", scheduler.repeatInEveryPeriod)
    put("highestPriorityAsDefault", scheduler.highestPriorityAsDefault)
    put("showNotification", scheduler.showNotification)
    when (scheduler) {
        is Scheduler.OneShot -> {
            put("type", "OneShot")
            put("removeScheduled", scheduler.removeScheduled)
        }
        is Scheduler.Weekly -> {
            put("type", "Weekly")
            put("daysOfWeek", JsonArray(scheduler.daysOfWeek.map { JsonPrimitive(it.isoDayNumber) }))
        }
        is Scheduler.Monthly -> {
            put("type", "Monthly")
            put("dayOfMonth", scheduler.dayOfMonth)
        }
    }
}

private fun List<Task>.toJson(): JsonArray = JsonArray(map(::taskToJson))

private fun successJson(): JsonObject = buildJsonObject {
    put("success", true)
}

private fun successJson(key: String, count: Int): JsonObject = buildJsonObject {
    put("success", true)
    put(key, count)
}

private fun stringProperty(description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("description", description)
}

private fun dateProperty(description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("format", "date-time")
    put("description", description)
}

private fun intProperty(description: String, minimum: Int? = null): JsonObject = buildJsonObject {
    put("type", "integer")
    put("description", description)
    minimum?.let { put("minimum", it) }
}

private fun nullableIntProperty(description: String): JsonObject = buildJsonObject {
    put("type", JsonArray(listOf(JsonPrimitive("integer"), JsonPrimitive("null"))))
    put("description", description)
    put("minimum", 1)
}

private fun nullableDateProperty(description: String): JsonObject = buildJsonObject {
    put("type", JsonArray(listOf(JsonPrimitive("string"), JsonPrimitive("null"))))
    put("description", description)
    put("format", "date-time")
}

private fun booleanProperty(description: String): JsonObject = buildJsonObject {
    put("type", "boolean")
    put("description", description)
}

private fun arrayProperty(items: JsonObject, description: String): JsonObject = buildJsonObject {
    put("type", "array")
    put("items", items)
    put("description", description)
    put("minItems", 1)
}

private fun sharesProperty(): JsonObject = buildJsonObject {
    put("type", "array")
    put("description", "Optional task shares")
    put("items", buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("userId", stringProperty("User ID to share with"))
            put("permission", buildJsonObject {
                put("type", "string")
                put("enum", JsonArray(listOf(JsonPrimitive("VIEWER"), JsonPrimitive("EDITOR_AND_VIEWER"))))
            })
        })
        put("required", JsonArray(listOf(JsonPrimitive("userId"), JsonPrimitive("permission"))))
    })
}

private fun schedulerProperty(): JsonObject = buildJsonObject {
    put("type", "object")
    put("description", "Schedule object. Use type OneShot, Weekly or Monthly.")
    put("properties", buildJsonObject {
        put("type", buildJsonObject {
            put("type", "string")
            put("enum", JsonArray(listOf(JsonPrimitive("OneShot"), JsonPrimitive("Weekly"), JsonPrimitive("Monthly"))))
        })
        put("hour", intProperty("Hour of day, 0..23", minimum = 0))
        put("minute", intProperty("Minute, 0..59", minimum = 0))
        put("startDate", dateProperty("Schedule start date-time"))
        put("lastDate", dateProperty("Optional last date-time"))
        put("daysOfWeek", arrayProperty(intProperty("ISO weekday: Monday=1, Sunday=7", minimum = 1), "Required for Weekly"))
        put("dayOfMonth", intProperty("Required for Monthly, 1..31", minimum = 1))
        put("repeatCount", intProperty("Repeat count or -1 for unlimited", minimum = -1))
        put("repeatInEveryPeriod", intProperty("Repeat every N periods", minimum = 1))
        put("highestPriorityAsDefault", booleanProperty("Priority default for generated tasks"))
        put("showNotification", booleanProperty("Whether generated tasks should show notifications"))
        put("removeScheduled", booleanProperty("Whether a OneShot schedule removes its source task"))
    })
    put("required", JsonArray(listOf(JsonPrimitive("type"), JsonPrimitive("hour"), JsonPrimitive("minute"), JsonPrimitive("startDate"))))
}

private fun nullableSchedulerProperty(): JsonObject = buildJsonObject {
    put("type", JsonArray(listOf(JsonPrimitive("object"), JsonPrimitive("null"))))
    put("description", "Replacement schedule object, or null to remove scheduling")
    put("properties", schedulerProperty()["properties"] ?: buildJsonObject {})
}

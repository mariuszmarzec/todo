package com.marzec.todo.extensions

import com.marzec.todo.model.Task

val Task.descriptionWithProgress: String
    get() {
        val description = description.toMarkdownPlainTextFirstLine()
        val subTaskDone = subTasks.count { !it.isToDo }
        val progress = if (subTaskDone > 0) {
            " - $subTaskDone/${subTasks.size}"
        } else {
            ""
        }
        return description + progress
    }

val Task.subDescription: String
    get() = subTasks
        .firstOrNull { it.isToDo }
        ?.description
        ?.lines()
        ?.first()?.takeIf { it != it.urls().firstOrNull() } ?: ""
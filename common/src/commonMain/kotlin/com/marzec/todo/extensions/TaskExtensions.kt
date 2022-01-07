package com.marzec.todo.extensions

import com.marzec.todo.model.Task

val Task.descriptionWithProgress: String
    get() {
        val description = description.lines().first()
        val subTaskDone = subTasks.count { !it.isToDo }
        val progress = if (subTaskDone > 0) {
            " - $subTaskDone/${subTasks.size}"
        } else {
            ""
        }
        return description + progress
    }
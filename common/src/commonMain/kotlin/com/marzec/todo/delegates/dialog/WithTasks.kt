package com.marzec.todo.delegates.dialog

import com.marzec.todo.model.Task

interface WithTasks<T> : WithDialog<T> {

    fun taskById(taskId: Int): Task
}
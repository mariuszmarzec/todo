package com.marzec.todo.delegates.dialog

import com.marzec.delegate.WithDialog
import com.marzec.todo.model.Task

interface WithTasks<T> : WithDialog<Int, T> {

    fun taskById(taskId: Int): Task
}

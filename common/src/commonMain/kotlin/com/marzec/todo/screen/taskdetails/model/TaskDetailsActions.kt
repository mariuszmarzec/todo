package com.marzec.todo.screen.taskdetails.model

sealed class TaskDetailsActions {
    object InitialLoad: TaskDetailsActions()
    object Edit : TaskDetailsActions()
}

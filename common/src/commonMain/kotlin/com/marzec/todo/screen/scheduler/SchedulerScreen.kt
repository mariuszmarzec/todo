package com.marzec.todo.screen.scheduler

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.marzec.mvi.collectState
import com.marzec.view.ActionBarProvider

@Composable
fun SchedulerScreen(
    store: SchedulerStore,
    actionBarProvider: ActionBarProvider
) {

    val state by store.collectState()

    Column {
        actionBarProvider.provide("Schedule")

        Button({ store.onSaveButtonClick() }) {
            Text("Save")
        }
    }
}
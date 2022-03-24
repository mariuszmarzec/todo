package com.marzec.todo.screen.scheduler

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.view.ActionBarProvider
import com.marzec.view.SpinnerView

@Composable
fun SchedulerScreen(
    store: SchedulerStore,
    actionBarProvider: ActionBarProvider
) {

    val state by store.collectState()

    Column {
        actionBarProvider.provide("Schedule")

        when (val state = state) {
            is State.Data -> {
                Row {
                    val hours = (0..23).toList()
                    val minutes = (0..59).toList()
                    SpinnerView(
                        label = "Hour",
                        items = hours.map { it.toString() },
                        selectedItemIndex = hours.indexOf(state.data.scheduler?.hour ?: 0)
                    ) {
                        store.onHourChanged(hours[it])
                    }
                    SpinnerView(
                        label = "Minute",
                        items = minutes.map { it.toString() },
                        selectedItemIndex = minutes.indexOf(state.data.scheduler?.minute ?: 0)
                    ) {
                        store.onMinuteChanged(minutes[it])
                    }
                }

                Button({ store.onSaveButtonClick() }) {
                    Text("Save")
                }
            }
            is State.Loading -> {
                Text("Pending")
            }
            is State.Error -> {

            }
        }
    }
}
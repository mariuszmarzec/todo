package com.marzec.todo.screen.scheduler

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import com.marzec.view.TextFieldStateful
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marzec.mvi.collectState
import com.marzec.view.ActionBarProvider
import com.marzec.view.DatePickerView
import com.marzec.view.SelectableRow
import com.marzec.view.SpinnerView
import kotlinx.datetime.DayOfWeek

@Composable
fun SchedulerScreen(
    store: SchedulerStore, actionBarProvider: ActionBarProvider
) {

    val state by store.collectState {
        store.onDatePickerResult()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        actionBarProvider.provide("Schedule")

        Row {
            val hours = (0..23).toList()
            val minutes = (0..59).toList()
            SpinnerView(
                label = "Hour",
                items = hours.map { it.toString() },
                selectedItemIndex = hours.indexOf(state.hour)
            ) {
                store.onHourChanged(hours[it])
            }
            SpinnerView(
                label = "Minute",
                items = minutes.map { it.toString() },
                selectedItemIndex = minutes.indexOf(state.minute)
            ) {
                store.onMinuteChanged(minutes[it])
            }
        }

        Spacer(Modifier.height(16.dp))

        val label = "start date"

        DatePickerView(label, state.date) {
            store.onDatePickerViewClick()
        }

        Spacer(Modifier.height(16.dp))

        Row {
            SchedulerTypeView(store, SchedulerType.OneShot, state)
            SchedulerTypeView(store, SchedulerType.Weekly, state)
            SchedulerTypeView(store, SchedulerType.Monthly, state)
        }
        if (state.type in listOf(SchedulerType.Weekly, SchedulerType.Monthly)) {
            SelectableRow(Color.White,
                selected = state.repeatTimes,
                selectable = true,
                onSelectedChange = { store.onRepeatTimesChanged() }) {
                Text("Repeat times?")
            }
            if (state.repeatTimes) {
                TextFieldStateful(label = { Text("Repeat count") },
                    value = state.repeatCount.toString(),
                    onValueChange = { store.onRepeatCountChanged(it) })
            }
            Spacer(Modifier.height(16.dp))
            TextFieldStateful(label = { Text("Repeat in every period") },
                value = state.repeatInEveryPeriod.toString(),
                onValueChange = { store.onRepeatInEveryPeriodChanged(it) })
        }
        if (state.type == SchedulerType.Weekly) {
            Row {
                DayOfWeek.values().forEach {
                    Box(Modifier.weight(1 / 7f)) {
                        SelectableRow(Color.White,
                            selected = it in state.daysOfWeek,
                            selectable = true,
                            checkboxMargin = 0.dp,
                            onSelectedChange = { store.onDaysOfWeekChanged(it) }) {
                            Text(it.toString().substring(0, 3))
                        }
                    }
                }
            }
        }
        if (state.type == SchedulerType.Monthly) {
            val daysInMonth = (1..31).toList()
            SpinnerView(
                label = "Day in month",
                items = daysInMonth.map { it.toString() },
                selectedItemIndex = daysInMonth.indexOf(state.dayOfMonth)
            ) {
                store.onDayOfMonthChanged(daysInMonth[it])
            }
        }
        Spacer(Modifier.height(16.dp))
        Button({ store.onSaveButtonClick() }) {
            Text("Save")
        }
    }
}

@Composable
fun SchedulerTypeView(
    store: SchedulerStore, schedulerType: SchedulerType, state: SchedulerState
) {
    Row(
        modifier = Modifier.clickable { store.type(schedulerType) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = state.type == schedulerType, onClick = { store.type(schedulerType) })
        Text(schedulerType.toString())
    }
}

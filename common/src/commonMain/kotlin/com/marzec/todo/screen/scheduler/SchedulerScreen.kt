package com.marzec.todo.screen.scheduler

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marzec.mvi.Store3
import com.marzec.mvi.collectState
import com.marzec.time.currentTime
import com.marzec.view.ActionBarProvider
import com.marzec.view.SelectableRow
import com.marzec.view.SpinnerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime

@Composable
fun SchedulerScreen(
    store: SchedulerStore,
    actionBarProvider: ActionBarProvider
) {

    val state by store.collectState()

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
        TextField(
            trailingIcon = {
                IconButton(
                    onClick = {

                    }
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Date pick")
                }
            },
            label = { Text("start date (yyyy-MM-dd)") },
            value = state.startDate,
            onValueChange = { store.onStartDateChanged(it) }
        )

        DatePickerView()

        Spacer(Modifier.height(16.dp))

        Row {
            SchedulerTypeView(store, SchedulerType.OneShot, state)
            SchedulerTypeView(store, SchedulerType.Weekly, state)
            SchedulerTypeView(store, SchedulerType.Monthly, state)
        }
        if (state.type in listOf(SchedulerType.Weekly, SchedulerType.Monthly)) {
            SelectableRow(
                Color.White,
                selected = state.repeatTimes,
                selectable = true,
                onSelectedChange = { store.onRepeatTimesChanged() }) {
                Text("Repeat times?")
            }
            if (state.repeatTimes) {
                TextField(
                    label = { Text("Repeat count") },
                    value = state.repeatCount.toString(),
                    onValueChange = { store.onRepeatCountChanged(it) }
                )
            }
            Spacer(Modifier.height(16.dp))
            TextField(
                label = { Text("Repeat in every period") },
                value = state.repeatInEveryPeriod.toString(),
                onValueChange = { store.onRepeatInEveryPeriodChanged(it) }
            )
        }
        if (state.type == SchedulerType.Weekly) {
            Row {
                DayOfWeek.values().forEach {
                    Box(Modifier.weight(1 / 7f)) {
                        SelectableRow(
                            Color.White,
                            selected = it in state.daysOfWeek,
                            selectable = true,
                            checkboxMargin = 0.dp,
                            onSelectedChange = { store.onDaysOfWeekChanged(it) }
                        ) {
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
    store: SchedulerStore,
    schedulerType: SchedulerType,
    state: SchedulerState
) {
    Row(
        modifier = Modifier.clickable { store.type(schedulerType) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = state.type == schedulerType,
            onClick = { store.type(schedulerType) }
        )
        Text(schedulerType.toString())
    }
}

data class DatePickerState(
    val year: Int = currentTime().year,
    val monthInYear: Int = currentTime().monthNumber,
    val dayInMonth: Int = currentTime().dayOfMonth
)

class DatePickerStore(
    scope: CoroutineScope,
    initialState: DatePickerState
) : Store3<DatePickerState>(scope, initialState) {

    fun onYearChange(year: Int) = reducerIntent {
        state.copy(year = year)
    }

    fun onMonthChange(month: Int) = reducerIntent {
        state.copy(monthInYear = month)
    }

    fun onDayChange(day: Int) = reducerIntent {
        state.copy(dayInMonth = day)
    }
}

@Composable
fun DatePickerView(
    year: Int = currentTime().year,
    monthInYear: Int = currentTime().monthNumber,
    dayInMonth: Int = currentTime().dayOfMonth,
    store: DatePickerStore = DatePickerStore(
        scope = rememberCoroutineScope(),
        initialState = DatePickerState(
            year, monthInYear, dayInMonth
        )
    )
) {
    val state by store.collectState()

    val years = (1920..currentTime().year).reversed()

    Column {
        SpinnerView(
            items = years.map { it.toString() },
            selectedItemIndex = years.indexOf(year),
            onValueChanged = {
                store.onYearChange(it)
            }
        )
        Row {
            DayOfWeek.values().map { it.toString().substring(0, 3) }.forEach { dayShortName ->
                Box(Modifier.weight(1/7f)) {
                    Text(dayShortName)
                }
            }
        }

    }
}
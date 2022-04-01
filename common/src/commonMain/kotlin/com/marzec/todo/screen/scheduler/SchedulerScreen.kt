package com.marzec.todo.screen.scheduler

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marzec.mvi.Store3
import com.marzec.mvi.collectState
import com.marzec.time.currentTime
import com.marzec.time.formatDate
import com.marzec.time.lengthOfMonth
import com.marzec.time.plusDays
import com.marzec.time.time
import com.marzec.view.ActionBarProvider
import com.marzec.view.SelectableRow
import com.marzec.view.SpinnerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber

@Composable
fun SchedulerScreen(
    store: SchedulerStore, actionBarProvider: ActionBarProvider
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
        TextField(trailingIcon = {
            IconButton(onClick = {

            }) {
                Icon(Icons.Default.DateRange, contentDescription = "Date pick")
            }
        },
            label = { Text("start date (yyyy-MM-dd)") },
            value = state.startDate,
            onValueChange = { store.onStartDateChanged(it) })

        DatePickerView()

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
                TextField(label = { Text("Repeat count") },
                    value = state.repeatCount.toString(),
                    onValueChange = { store.onRepeatCountChanged(it) })
            }
            Spacer(Modifier.height(16.dp))
            TextField(label = { Text("Repeat in every period") },
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

data class DatePickerState(
    val year: Int = currentTime().year,
    val monthInYear: Int = currentTime().monthNumber,
    val dayInMonth: Int = currentTime().dayOfMonth
) {
    fun toLocalDateTime() = time(dayInMonth, monthInYear, year)
}

class DatePickerStore(
    scope: CoroutineScope, initialState: DatePickerState
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
    store: DatePickerStore = DatePickerStore(
        scope = rememberCoroutineScope(), initialState = DatePickerState(
        )
    )
) {
    val state by store.collectState()

    val years = (1920..currentTime().year).reversed().toList()
    val months = (1..12).toList()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.Center) {
            IconButton(
                onClick = { store.onYearChange(state.year.dec()) }
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous year")
            }

            SpinnerView(items = years.map { it.toString() },
                selectedItemIndex = years.indexOf(state.year),
                onValueChanged = {
                    store.onYearChange(years[it])
                })
            IconButton(
                onClick = { store.onYearChange(state.year.inc()) }
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next year")
            }
        }
        Row(horizontalArrangement = Arrangement.Center) {
            IconButton(
                onClick = { store.onMonthChange(state.monthInYear.dec()) }
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            SpinnerView(items = months.map {
                time(
                    state.dayInMonth,
                    it,
                    state.year
                ).formatDate("MMM")
            },
                selectedItemIndex = months.indexOf(state.monthInYear),
                onValueChanged = {
                    store.onMonthChange(months[it])
                })
            IconButton(
                onClick = { store.onMonthChange(state.monthInYear.inc()) }
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
            }
        }
        Row {
            DayOfWeek.values().map { it.toString().substring(0, 3) }.forEach { dayShortName ->
                Box(
                    modifier = Modifier.weight(1 / 7f), contentAlignment = Alignment.Center,
                ) {
                    Text(dayShortName)
                }
            }
        }
        val currentSelectedDay = state.toLocalDateTime()
        val currentMonthStartDate = state.copy(dayInMonth = 1).toLocalDateTime()

        val startOffset = 1 - currentMonthStartDate.dayOfWeek.isoDayNumber
        val monthDates = (startOffset until startOffset + 6 * 7).map { offset: Int ->
            currentMonthStartDate.plusDays(offset)
        }.chunked(7)

        monthDates.forEach { rowDates: List<LocalDateTime> ->
            Row {
                rowDates.forEach { day ->
                    ClickableText(
                        modifier = Modifier
                            .padding(8.dp, 8.dp, 8.dp, 8.dp)
                            .background(
                                if (day == currentSelectedDay) {
                                    Color.Gray
                                } else {
                                    Color.Transparent
                                }
                            ).weight(1 / 7f),
                        text = AnnotatedString(day.formatDate("d")),
                        style = TextStyle.Default.copy(
                            color = if (day.monthNumber != state.monthInYear) {
                                Color.Gray
                            } else {
                                Color.Black
                            },
                            textAlign = TextAlign.Center
                        ),
                        onClick = {
                            store.onDayChange(day.dayOfMonth)
                        }
                    )
                }
            }
        }
    }
}
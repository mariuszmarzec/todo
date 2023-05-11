package com.marzec.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marzec.delegate.StoreDelegate
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.mvi.collectState
import com.marzec.mvi.reduceData
import com.marzec.navigation.Destination
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.Preferences
import com.marzec.time.SHORT_DATE_FORMAT
import com.marzec.time.currentTime
import com.marzec.time.formatDate
import com.marzec.time.plusDays
import com.marzec.time.time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber

const val REQUEST_DATE_PICKER = 142323

@Composable
fun DatePickerView(
    label: String = "",
    date: LocalDateTime,
    onDatePickerClick: () -> Unit
) {
    Column(modifier = Modifier.clickable { onDatePickerClick() }) {
        if (label.isNotEmpty()) {
            Text(label)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.defaultMinSize(100.dp)
        ) {

            Text(modifier = Modifier.padding(8.dp), text = date.formatDate(SHORT_DATE_FORMAT))

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = {
                onDatePickerClick()
            }) {
                Icon(Icons.Default.DateRange, contentDescription = "Date pick")
            }
        }
    }
}

data class DatePickerState(
    val year: Int = currentTime().year,
    val monthInYear: Int = currentTime().monthNumber,
    val dayInMonth: Int = currentTime().dayOfMonth,
    val currentSelectedDay: LocalDateTime? = null
) {
    fun toLocalDateTime() = time(dayInMonth, monthInYear, year)

    companion object {
        fun from(dateTime: LocalDateTime?) = dateTime?.let {
            DatePickerState(
                dateTime.year,
                dateTime.monthNumber,
                dateTime.dayOfMonth,
                dateTime
            )
        } ?: DatePickerState()
    }
}

class DatePickerStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    initialState: DatePickerState,
    private val cacheKey: String,
    private val stateCache: Preferences
) : Store3<DatePickerState>(
    scope, stateCache.get(cacheKey) ?: initialState
) {

    fun onYearChange(year: Int) = reducerIntent {
        state.copy(year = year)
    }

    fun onMonthChange(month: Int) = reducerIntent {
        state.copy(monthInYear = month)
    }

    fun onDayClick(day: Int) = intent<Unit> {
        reducer {
            state.copy(dayInMonth = day)
        }
        sideEffect {
            navigationStore.goBack(state.toLocalDateTime())
        }
    }

    override suspend fun onNewState(newState: DatePickerState) {
        super.onNewState(newState)
        stateCache.set(cacheKey, newState)
    }
}

@Composable
fun DatePickerScreen(
    store: DatePickerStore,
    actionBarProvider: ActionBarProvider
) {
    val state by store.collectState()

    val years = (1920..currentTime().year+4).reversed().toList()
    val months = (1..12).toList()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        actionBarProvider.provide { }
        val yearIndex = years.indexOfFirst { it == state.year }
        Row(horizontalArrangement = Arrangement.Center) {
            if (yearIndex < years.lastIndex) {
                IconButton(
                    onClick = { store.onYearChange(state.year.dec()) }
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous year")
                }
            }

            SpinnerView(items = years.map { it.toString() },
                selectedItemIndex = yearIndex,
                onValueChanged = {
                    store.onYearChange(years[it])
                })

            if (yearIndex > 0) {
                IconButton(
                    onClick = { store.onYearChange(state.year.inc()) }
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next year")
                }
            }
        }
        val monthIndex = months.indexOfFirst { it == state.monthInYear }
        Row(horizontalArrangement = Arrangement.Center) {
            if (monthIndex > 0) {
                IconButton(
                    onClick = { store.onMonthChange(state.monthInYear.dec()) }
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
                }
            }
            SpinnerView(items = months.map {
                time(
                    1,
                    it,
                    state.year
                ).formatDate("MMM")
            },
                selectedItemIndex = monthIndex,
                onValueChanged = {
                    store.onMonthChange(months[it])
                })
            if (monthIndex < months.lastIndex) {
                IconButton(
                    onClick = { store.onMonthChange(state.monthInYear.inc()) }
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
                }
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
        val currentSelectedDay = state.currentSelectedDay

        val currentMonthStartDate = state.copy(dayInMonth = 1).toLocalDateTime()

        val startOffset = 1 - currentMonthStartDate.dayOfWeek.isoDayNumber
        val monthDates = (startOffset until startOffset + 6 * 7).map { offset: Int ->
            currentMonthStartDate.plusDays(offset)
        }.chunked(7)

        monthDates.forEach { rowDates: List<LocalDateTime> ->
            Row {
                rowDates.forEach { day ->
                    val isSelected =
                        currentSelectedDay?.let {
                            it.year == day.year &&
                                    it.monthNumber == day.monthNumber &&
                                    it.dayOfMonth == day.dayOfMonth
                        }
                            ?: false

                    ClickableText(
                        modifier = Modifier
                            .padding(8.dp, 8.dp, 8.dp, 8.dp)
                            .background(
                                if (isSelected) {
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
                            store.onDayClick(day.dayOfMonth)
                        }
                    )
                }
            }
        }
    }
}

interface WithDate<DATA> {
    val date: LocalDateTime

    fun copyWithDate(date: LocalDateTime): DATA
}

interface DateDelegate {

    fun onDatePickerResult()
    fun onDatePickerViewClick()
}

class DateDelegateImpl<DATA : WithDate<DATA>>(
    private val navigationStore: NavigationStore,
    private val datePickerDestinationFactory: (date: LocalDateTime) -> Destination
) : StoreDelegate<DATA>(), DateDelegate {

    override fun onDatePickerResult() = intent<LocalDateTime> {
        onTrigger {
            navigationStore.observe<LocalDateTime>(REQUEST_DATE_PICKER)?.filterNotNull()
        }

        reducer {
            result?.let { state.copyWithDate(it) } ?: state
        }
    }

    override fun onDatePickerViewClick() = sideEffect {
        navigationStore.next(
            NavigationAction(datePickerDestinationFactory(state.date)),
            requestId = REQUEST_DATE_PICKER
        )
    }
}

class DateDelegateStateImpl<DATA : WithDate<DATA>>(
    private val navigationStore: NavigationStore,
    private val datePickerDestinationFactory: (date: LocalDateTime) -> Destination
) : StoreDelegate<State<DATA>>(), DateDelegate {

    override fun onDatePickerResult() = intent<LocalDateTime> {
        onTrigger {
            navigationStore.observe<LocalDateTime>(REQUEST_DATE_PICKER)?.filterNotNull()
        }

        reducer {
            result?.let { state.reduceData { copyWithDate(it) } } ?: state
        }
    }

    override fun onDatePickerViewClick() = sideEffect {
        state.ifDataAvailable {
            navigationStore.next(
                NavigationAction(datePickerDestinationFactory(date)),
                requestId = REQUEST_DATE_PICKER
            )
        }
    }
}

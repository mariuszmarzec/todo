package com.marzec.todo.screen.scheduler

import com.marzec.extensions.toggle
import com.marzec.mvi.Store3
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.Preferences
import com.marzec.time.currentTime
import com.marzec.time.formatDate
import com.marzec.time.shortDateToLocalDateTime
import com.marzec.todo.model.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.toLocalDateTime

data class SchedulerState(
    val hour: Int = 7,
    val minute: Int = 0,
    val startDate: String = currentTime().formatDate("yyyy-MM-dd"),
    val repeatTimes: Boolean = false,
    val repeatCount: Int = 1,
    val repeatInEveryPeriod: Int = 1,
    val type: SchedulerType = SchedulerType.OneShot,
    val daysOfWeek: List<DayOfWeek> = emptyList(),
    val dayOfMonth: Int = 1
) {
    companion object {
        val INITIAL = SchedulerState()

        fun from(scheduler: Scheduler?) = scheduler?.let {
            SchedulerState(
                hour = scheduler.hour,
                minute = scheduler.minute,
                startDate = scheduler.startDate.formatDate("yyyy-MM-dd"),
                repeatCount = scheduler.repeatCount,
                repeatInEveryPeriod = scheduler.repeatInEveryPeriod,
                type = when (scheduler) {
                    is Scheduler.Monthly -> SchedulerType.Monthly
                    is Scheduler.OneShot -> SchedulerType.OneShot
                    is Scheduler.Weekly -> SchedulerType.Weekly
                },
                daysOfWeek = (scheduler as? Scheduler.Weekly)?.daysOfWeek ?: emptyList(),
                dayOfMonth = (scheduler as? Scheduler.Monthly)?.dayOfMonth ?: 1
            )
        } ?: INITIAL
    }
}

enum class SchedulerType {
    OneShot, Weekly, Monthly
}

const val RESULT_KEY_SCHEDULER = "REQUEST_KEY_SCHEDULER"

class SchedulerStore(
    scope: CoroutineScope,
    private val cacheKey: String,
    private val stateCache: Preferences,
    private val navigationStore: NavigationStore,
    initialState: SchedulerState
) : Store3<SchedulerState>(
    scope, stateCache.get(cacheKey) ?: initialState
) {

    fun onSaveButtonClick() = sideEffect {
        val scheduler = when (state.type) {
            SchedulerType.OneShot -> Scheduler.OneShot(
                state.hour,
                state.minute,
                state.startDate.shortDateToLocalDateTime(),
                lastDate = null
            )
            SchedulerType.Weekly -> Scheduler.Weekly(
                state.hour,
                state.minute,
                state.startDate.shortDateToLocalDateTime(),
                lastDate = null,
                daysOfWeek = state.daysOfWeek,
                repeatCount = state.repeatCount,
                repeatInEveryPeriod = state.repeatInEveryPeriod
            )
            SchedulerType.Monthly -> Scheduler.Monthly(
                state.hour,
                state.minute,
                state.startDate.shortDateToLocalDateTime(),
                lastDate = null,
                dayOfMonth = state.dayOfMonth,
                repeatCount = state.repeatCount,
                repeatInEveryPeriod = state.repeatInEveryPeriod
            )
        }
        navigationStore.goBack(RESULT_KEY_SCHEDULER to scheduler)
    }

    override suspend fun onNewState(newState: SchedulerState) {
        stateCache.set(cacheKey, newState)
    }

    fun onHourChanged(hour: Int) = intent<Unit> {
        reducer { state.copy(hour = hour) }
    }

    fun onMinuteChanged(minute: Int) = reducerIntent {
        state.copy(minute = minute)
    }

    fun onStartDateChanged(startDate: String) = reducerIntent {
        state.copy(startDate = startDate)
    }

    fun onRepeatTimesChanged() = reducerIntent {
        state.copy(repeatTimes = !state.repeatTimes)
    }

    fun onRepeatCountChanged(repeatCount: String) = reducerIntent {
        repeatCount.toIntOrNull()?.let { state.copy(repeatCount = it) } ?: state
    }

    fun onRepeatInEveryPeriodChanged(repeatInEveryPeriod: String) = reducerIntent {
        repeatInEveryPeriod.toIntOrNull()?.let { state.copy(repeatInEveryPeriod = it) } ?: state
    }

    fun type(type: SchedulerType) = reducerIntent {
        state.copy(type = type)
    }

    fun onDaysOfWeekChanged(day: DayOfWeek) = reducerIntent {
        state.copy(daysOfWeek = state.daysOfWeek.toSet().toggle(day).toList())
    }

    fun onDayOfMonthChanged(dayOfMonth: Int) = reducerIntent {
        state.copy(dayOfMonth = dayOfMonth)
    }
}
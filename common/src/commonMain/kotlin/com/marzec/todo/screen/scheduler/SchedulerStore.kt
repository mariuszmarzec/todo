package com.marzec.todo.screen.scheduler

import com.marzec.mvi.delegates
import com.marzec.extensions.toggle
import com.marzec.mvi.Store4Impl
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.StateCache
import com.marzec.time.currentTime
import com.marzec.todo.model.Scheduler
import com.marzec.view.DateDelegate
import com.marzec.view.WithDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime

data class SchedulerState(
    val hour: Int = 7,
    val minute: Int = 0,
    val creationDate: LocalDateTime? = currentTime(),
    override val date: LocalDateTime = currentTime(),
    val repeatTimes: Boolean = false,
    val repeatCount: Int = 1,
    val repeatInEveryPeriod: Int = 1,
    val type: SchedulerType = SchedulerType.OneShot,
    val daysOfWeek: List<DayOfWeek> = emptyList(),
    val dayOfMonth: Int = 1,
    val additionalOptionsAvailable: Boolean = false,
    val removeAfterSchedule: Boolean = false,
    val highestPriorityAsDefault: Boolean = false
) : WithDate<SchedulerState> {

    override fun copyWithDate(date: LocalDateTime): SchedulerState = copy(date = date)

    companion object {
        val INITIAL = SchedulerState()

        fun from(scheduler: Scheduler?, additionalOptionsAvailable: Boolean) = scheduler?.let {
            SchedulerState(
                hour = scheduler.hour,
                minute = scheduler.minute,
                creationDate = scheduler.creationDate ?: currentTime(),
                date = scheduler.startDate,
                repeatCount = scheduler.repeatCount,
                repeatInEveryPeriod = scheduler.repeatInEveryPeriod,
                type = when (scheduler) {
                    is Scheduler.Monthly -> SchedulerType.Monthly
                    is Scheduler.OneShot -> SchedulerType.OneShot
                    is Scheduler.Weekly -> SchedulerType.Weekly
                },
                daysOfWeek = (scheduler as? Scheduler.Weekly)?.daysOfWeek ?: emptyList(),
                dayOfMonth = (scheduler as? Scheduler.Monthly)?.dayOfMonth ?: 1,
                additionalOptionsAvailable = additionalOptionsAvailable,
                removeAfterSchedule = (scheduler as? Scheduler.OneShot)?.removeScheduled ?: false,
                highestPriorityAsDefault = scheduler.highestPriorityAsDefault
            )
        } ?: INITIAL.copy(additionalOptionsAvailable = additionalOptionsAvailable)
    }
}

enum class SchedulerType {
    OneShot, Weekly, Monthly
}

class SchedulerStore(
    scope: CoroutineScope,
    private val cacheKey: String,
    private val stateCache: StateCache,
    private val navigationStore: NavigationStore,
    initialState: SchedulerState,
    private val dateDelegate: DateDelegate
) : Store4Impl<SchedulerState>(
    scope, stateCache.get(cacheKey) ?: initialState
), DateDelegate by dateDelegate {

    init {
        delegates(dateDelegate)
    }

    fun onSaveButtonClick() = sideEffectIntent {
        val scheduler = when (state.type) {
            SchedulerType.OneShot -> Scheduler.OneShot(
                hour = state.hour,
                minute = state.minute,
                creationDate = state.creationDate,
                startDate = state.date,
                lastDate = null,
                highestPriorityAsDefault = state.highestPriorityAsDefault,
                removeScheduled = state.removeAfterSchedule
            )

            SchedulerType.Weekly -> Scheduler.Weekly(
                hour = state.hour,
                minute = state.minute,
                creationDate = state.creationDate,
                startDate = state.date,
                lastDate = null,
                daysOfWeek = state.daysOfWeek,
                repeatCount = state.repeatCount.takeIf { state.repeatTimes } ?: -1,
                repeatInEveryPeriod = state.repeatInEveryPeriod,
                highestPriorityAsDefault = state.highestPriorityAsDefault
            )

            SchedulerType.Monthly -> Scheduler.Monthly(
                hour = state.hour,
                minute = state.minute,
                creationDate = state.creationDate,
                startDate = state.date,
                lastDate = null,
                dayOfMonth = state.dayOfMonth,
                repeatCount = state.repeatCount.takeIf { state.repeatTimes } ?: -1,
                repeatInEveryPeriod = state.repeatInEveryPeriod,
                highestPriorityAsDefault = state.highestPriorityAsDefault
            )
        }
        navigationStore.goBack(scheduler)
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

    fun toggleHighestPriority() =
        reducerIntent { state.copy(highestPriorityAsDefault = !state.highestPriorityAsDefault) }

    fun toggleRemoveAfterSchedule() =
        reducerIntent { state.copy(removeAfterSchedule = !state.removeAfterSchedule) }
}
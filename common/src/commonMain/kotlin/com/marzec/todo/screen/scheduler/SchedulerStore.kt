package com.marzec.todo.screen.scheduler

import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.Preferences
import com.marzec.time.currentTime
import com.marzec.todo.model.Scheduler
import kotlinx.coroutines.CoroutineScope

data class SchedulerData(
    val scheduler: Scheduler?
) {
    companion object {
        val INITIAL = State.Data(SchedulerData(null))
    }
}

const val RESULT_KEY_SCHEDULER = "REQUEST_KEY_SCHEDULER"

class SchedulerStore(
    scope: CoroutineScope,
    private val cacheKey: String,
    private val stateCache: Preferences,
    private val navigationStore: NavigationStore,
    initialState: State<SchedulerData>
) : Store3<State<SchedulerData>>(
    scope, stateCache.get(cacheKey) ?: initialState
) {

    fun onSaveButtonClick() = sideEffect {
        val scheduler = Scheduler.OneShot(
            10, 5, currentTime(), currentTime()
        )

        navigationStore.goBack(RESULT_KEY_SCHEDULER to scheduler)
    }

    override suspend fun onNewState(newState: State<SchedulerData>) {
        stateCache.set(cacheKey, newState)
    }

    fun onHourChanged(hour: Int) {
        TODO("Not yet implemented")
    }
}
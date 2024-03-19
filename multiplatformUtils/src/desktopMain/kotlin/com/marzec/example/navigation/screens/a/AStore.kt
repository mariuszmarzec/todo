package com.marzec.example.navigation.screens.a

import com.marzec.example.navigation.NavigationExampleDestination
import com.marzec.mvi.Store3
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.next
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AStore(
    private val navigationStore: NavigationStore,
    private val message: StateFlow<String>,
    scope: CoroutineScope
) : Store3<String>(scope, message.value) {

    fun load() = intent<String> {
        onTrigger { message }

        reducer { resultNonNull() }
    }

    fun onGoToBClick() = sideEffectIntent {
        navigationStore.next(NavigationExampleDestination.B)
    }
}

package com.marzec.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.marzec.example.navigation.NavigationExampleDestination
import com.marzec.example.navigation.screens.a.AStore
import com.marzec.example.navigation.screens.a.ScreenA
import com.marzec.example.navigation.screens.b.BStore
import com.marzec.example.navigation.screens.b.ScreenB
import com.marzec.example.navigation.screens.home.HomeScreen
import com.marzec.example.navigation.screens.home.HomeStore
import com.marzec.logger.Logger
import com.marzec.navigation.Destination
import com.marzec.navigation.NavigationState
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.MemoryStateCache
import com.marzec.view.ActionBarProvider
import com.marzec.view.NavigationHost
import com.marzec.view.navigationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

// not working because of that shit
// https://youtrack.jetbrains.com/issue/KTIJ-13908/Its-impossible-to-run-main-method-from-Java-sources-of-a-multiplatform-project
fun main() {
    Logger.logger = object : Logger {
        override fun log(tag: String, message: String) {
            println("$tag: $message")
        }

        override fun log(tag: String, message: String, t: Throwable) {
            println("$tag: $message")
            t.printStackTrace()
        }
    }

    application {
        Window(
            ::exitApplication,
            title = "Utils",
            state = rememberWindowState(
                width = 700.dp, height = 768.dp, position = WindowPosition.Aligned(
                    Alignment.Center
                )
            )
        ) {
            DI.navigationStore = DI.provideNavigationStore(rememberCoroutineScope()) {
                exitApplication()
                this
            }

            Column(modifier = Modifier.fillMaxSize()) {
                ActionBarProvider(DI.navigationStore).provide(
                    title = "Navigation example",
                    backButtonShow = { true }
                )

                NavigationHost(DI.navigationStore, DI::router)
            }
        }
    }
}

object DI {
    val stateCache = MemoryStateCache()

    val cacheKeyProvider by lazy {
        { Random.nextInt(Int.MAX_VALUE).toString() }
    }

    lateinit var navigationStore: NavigationStore

    fun provideNavigationStore(
        scope: CoroutineScope,
        overrideLastClose: (NavigationState.() -> NavigationState)? = null
    ) = navigationStore(
        scope = scope,
        stateCache = stateCache,
        cacheKeyProvider = cacheKeyProvider,
        defaultDestination = NavigationExampleDestination.HomeScreen,
        overrideLastClose = overrideLastClose,
        onNewStateCallback = ::onNewStateCallback
    )

    val flowsScope = mutableMapOf<String, MutableStateFlow<String>>()

    private var currentFlows = setOf<String>()
    fun onNewStateCallback(state: NavigationState) {
        val newFlowsList = state.flows()

        val flowsScopesToRemove = currentFlows - newFlowsList
        val flowsScopeToCreate = newFlowsList - currentFlows

        currentFlows = newFlowsList

        flowsScopesToRemove.forEach { flowsScope.remove(it) }
        flowsScopeToCreate.forEach {
            flowsScope[it] = MutableStateFlow("default value")
        }
    }

    fun router(
        destination: Destination,
        flowId: String
    ): @Composable (destination: Destination, cacheKey: String) -> Unit {
        return when (destination as NavigationExampleDestination) {
            NavigationExampleDestination.HomeScreen -> @Composable { _, cacheKey ->
                val store = HomeStore(rememberCoroutineScope(), navigationStore)
                HomeScreen(store)
            }

            is NavigationExampleDestination.A -> @Composable { _, cacheKey ->
                val store = AStore(navigationStore, flowsScope.getValue(flowId), rememberCoroutineScope())
                ScreenA(store)
            }

            is NavigationExampleDestination.B -> @Composable { _, cacheKey ->
                val store = BStore(navigationStore, flowsScope.getValue(flowId), rememberCoroutineScope())
                ScreenB(store)
            }
        }
    }
}

private fun NavigationState.flows(): Set<String> = backStack.fold(mutableSetOf()) { acc, entry ->
    entry.subFlow?.let {
        acc.add(it.id)
        acc.addAll(it.flows())
    }
    acc
}
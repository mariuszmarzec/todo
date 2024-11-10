package com.marzec.screen.featuretoggle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.content.mapData
import com.marzec.model.FeatureToggle
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.navigation.Destination
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.StateCache
import com.marzec.repository.FeatureToggleRepository
import com.marzec.screen.pickitemscreen.PickItemOptions
import com.marzec.screen.pickitemscreen.PickItemScreen
import com.marzec.screen.pickitemscreen.providePickItemStore
import com.marzec.view.ActionBarProvider
import com.marzec.view.TextFieldStateful
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.mapLatest

data class FeatureToggles(val options: PickItemOptions<FeatureToggle>) : Destination

data class FeatureToggleDetails(
    val id: Int? = null,
    val name: String = "",
    val value: String = ""
) : Destination

@Composable
fun provideFeatureTogglesScreen(
    actionBarProvider: ActionBarProvider,
    destination: FeatureToggles,
    cacheKey: String,
    navigationStore: NavigationStore,
    stateCache: StateCache,
) =
    PickItemScreen(
        options = destination.options,
        store = providePickItemStore(
            options = destination.options,
            scope = rememberCoroutineScope(),
            cacheKey = cacheKey,
            navigationStore = navigationStore,
            stateCache = stateCache
        ),
        actionBarProvider = actionBarProvider
    )

fun featureTogglePickOptions(
    featureToggleRepository: FeatureToggleRepository,
    navigationStore: NavigationStore
) = PickItemOptions(
    loadData = {
        featureToggleRepository.observeAll()
            .mapLatest { content ->
                content.mapData { featureToggles -> featureToggles.sortedBy { it.name } }
            }
    },
    mapItemToId = { it.id.toString() },
    itemRow = { item: FeatureToggle, _: (FeatureToggle) -> Unit ->
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable {
                    navigationStore.next(NavigationAction(FeatureToggleDetails(item.id)))
                }
                .padding(16.dp)
        ) {
            Text(item.name)
            Spacer(Modifier.weight(1f))
            Text(item.value)
        }

    },
    onAddNavigationAction = { NavigationAction(FeatureToggleDetails()) },
    stringsToCompare = { listOf(it.name) },
    groupByHeader = { featureToggle ->
        featureToggle.name.split(".").takeIf { it.size >= 2 }?.get(0) ?: ""
    }
)

@Composable
fun FeatureToggleScreen(
    store: FeatureToggleStore,
    actionBarProvider: ActionBarProvider
) {

    val state: State<FeatureToggleState> by store.collectState {
        store.initialLoad()
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide()
        }
    ) {
        when (val state = state) {
            is State.Data -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextFieldStateful(state.data.name, {
                            store.onNameChanged(it)
                        })
                    }
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextFieldStateful(state.data.value, {
                            store.onValueChanged(it)
                        })
                    }
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextButton({ store.save() }) {
                            Text(
                                if (state.data.id != null) {
                                    "Update"
                                } else {
                                    "Create"
                                }
                            )
                        }
                    }
                }
            }

            is State.Loading -> {
                Text(text = "Loading")
            }

            is State.Error -> {
                Text(text = state.message)
            }
        }
    }
}

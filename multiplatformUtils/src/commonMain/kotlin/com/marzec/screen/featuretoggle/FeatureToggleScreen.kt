package com.marzec.screen.featuretoggle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.model.FeatureToggle
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.navigation.Destination
import com.marzec.view.ActionBarProvider
import com.marzec.view.TextFieldStateful

data class FeatureToggleDetails(
    val id: Int? = null,
    val name: String = "",
    val value: String = ""
) : Destination

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

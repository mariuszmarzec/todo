package com.marzec.todo.mcp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.view.ActionBarProvider

@Composable
fun LocalMcpTokenSettingsScreen(
    actionBarProvider: ActionBarProvider,
    store: LocalMcpTokenSettingsStore
) {
    val state: State<LocalMcpTokenSettingsState> by store.collectState {
        store.loadToken()
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide("Local MCP token")
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("This token is used to authorize clients connecting to the local Todo MCP server.")
            when (val currentState = state) {
                is State.Data -> {
                    val data = currentState.data
                    if (data.saving) {
                        CircularProgressIndicator()
                    } else {
                        Text("Current token", style = MaterialTheme.typography.subtitle1)
                        OutlinedTextField(
                            value = data.currentToken,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = { store.copyToken() },
                            enabled = data.currentToken.isNotEmpty() && !data.saving
                        ) {
                            Text("Copy token")
                        }
                        OutlinedTextField(
                            value = data.draftToken,
                            onValueChange = { store.onDraftTokenChanged(it) },
                            label = { Text("New token") },
                            singleLine = true,
                            isError = data.error != null,
                            enabled = !data.saving,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Use 32-256 URL-safe characters: letters, digits, dot, underscore, tilde or hyphen.",
                            style = MaterialTheme.typography.caption
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { store.saveToken() },
                                enabled = !data.saving
                            ) {
                                Text("Save token")
                            }
                            TextButton(
                                onClick = { store.regenerateToken() },
                                enabled = !data.saving
                            ) {
                                Text("Regenerate")
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        data.error?.let { Text(it, color = MaterialTheme.colors.error) }
                        data.status?.let { Text(it, color = MaterialTheme.colors.primary) }
                    }
                }

                is State.Loading -> {
                    CircularProgressIndicator()
                }

                is State.Error -> {
                    Text(text = currentState.message)
                }
            }
        }
    }
}

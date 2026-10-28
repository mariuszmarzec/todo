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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.common.CopyToClipBoardHelper
import com.marzec.view.ActionBarProvider
import kotlinx.coroutines.launch

@Composable
fun LocalMcpTokenSettingsScreen(
    actionBarProvider: ActionBarProvider,
    copyToClipBoardHelper: CopyToClipBoardHelper,
    store: McpTokenStore?
) {
    if (store == null) return

    val scope = rememberCoroutineScope()
    var currentToken by remember { mutableStateOf("") }
    var draftToken by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(store) {
        try {
            val token = store.getOrCreateToken()
            currentToken = token
            draftToken = token
        } catch (exception: Exception) {
            error = exception.message ?: "Could not load the local MCP token"
        } finally {
            loading = false
        }
    }

    fun updateToken(regenerate: Boolean) {
        scope.launch {
            saving = true
            error = null
            status = null
            try {
                val token = if (regenerate) {
                    store.regenerateToken()
                } else {
                    require(isValidLocalMcpToken(draftToken)) {
                        "Token must be 32-256 URL-safe characters using letters, digits, dot, underscore, tilde or hyphen"
                    }
                    store.saveToken(draftToken)
                    draftToken
                }
                currentToken = token
                draftToken = token
                status = "Token updated. The previous stored token has been replaced."
            } catch (exception: Exception) {
                error = exception.message ?: "Could not update the local MCP token"
            } finally {
                saving = false
            }
        }
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
            if (loading) {
                CircularProgressIndicator()
            } else {
                Text("Current token", style = MaterialTheme.typography.subtitle1)
                OutlinedTextField(
                    value = currentToken,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { copyToClipBoardHelper.copy(currentToken) },
                    enabled = currentToken.isNotEmpty() && !saving
                ) {
                    Text("Copy token")
                }
                OutlinedTextField(
                    value = draftToken,
                    onValueChange = {
                        draftToken = it
                        error = null
                        status = null
                    },
                    label = { Text("New token") },
                    singleLine = true,
                    isError = error != null,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Use 32-256 URL-safe characters: letters, digits, dot, underscore, tilde or hyphen.",
                    style = MaterialTheme.typography.caption
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { updateToken(regenerate = false) },
                        enabled = !saving
                    ) {
                        Text("Save token")
                    }
                    TextButton(
                        onClick = { updateToken(regenerate = true) },
                        enabled = !saving
                    ) {
                        Text("Regenerate")
                    }
                }
                Spacer(Modifier.height(2.dp))
                error?.let { Text(it, color = MaterialTheme.colors.error) }
                status?.let { Text(it, color = MaterialTheme.colors.primary) }
            }
        }
    }
}

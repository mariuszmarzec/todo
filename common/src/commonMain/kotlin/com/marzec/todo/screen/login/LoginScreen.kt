package com.marzec.todo.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marzec.todo.navigation.model.NavigationActions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.screen.login.model.LoginActions
import com.marzec.todo.screen.login.model.LoginStore
import com.marzec.todo.screen.login.model.LoginViewState
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(loginStore: LoginStore) {
    val scope = rememberCoroutineScope()

    val state: LoginViewState by loginStore.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(modifier = Modifier.padding(16.dp)) {
            TextField(state.loginData.login, {
                scope.launch { loginStore.sendAction(LoginActions.LoginChanged(it)) }
            })
        }
        Box(modifier = Modifier.padding(16.dp)) {
            TextField(state.loginData.password, {
                scope.launch { loginStore.sendAction(LoginActions.PasswordChanged(it)) }
            })
        }
        Box(modifier = Modifier.padding(16.dp)) {
            TextButton({ scope.launch { loginStore.sendAction(LoginActions.LoginButtonClick) } }) {
                Text("Login")
            }
        }
    }
}

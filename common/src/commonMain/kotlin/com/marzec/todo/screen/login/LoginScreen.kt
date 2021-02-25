package com.marzec.todo.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.todo.screen.login.model.LoginActions
import com.marzec.todo.screen.login.model.LoginViewState
import com.marzec.todo.screen.login.model.loginStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.compose.runtime.getValue

@ExperimentalCoroutinesApi
@Composable
fun LoginScreen() {
    val scope = rememberCoroutineScope()

    val state: LoginViewState by loginStore.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(modifier = Modifier.padding(16.dp)) {
            TextField(state.loginData.login, { loginStore.sendAction(LoginActions.LoginChanged(it), scope) })
        }
        Box(modifier = Modifier.padding(16.dp)) {
            TextField(state.loginData.password, { loginStore.sendAction(LoginActions.PasswordChanged(it), scope) })
        }
        Box(modifier = Modifier.padding(16.dp)) {
            TextButton({}) {
                Text("Login")
            }
        }
    }
}
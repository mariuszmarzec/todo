package com.marzec.todo.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.mvi.State
import com.marzec.todo.screen.login.model.LoginData
import com.marzec.todo.screen.login.model.LoginStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(loginStore: LoginStore) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loginStore.init(scope)
    }

    val state: State<LoginData> by loginStore.state.collectAsState()

    when (val state = state) {
        is State.Data -> {
            LoginScreen(
                login = state.data.login,
                scope = scope,
                loginStore = loginStore,
                password = state.data.password,
                error = ""
            )
        }
        is State.Loading -> {
            Text(text = "Pending...", fontSize = 16.sp)

        }
        is State.Error -> {
            LoginScreen(
                login = state.data?.login.orEmpty(),
                scope = scope,
                loginStore = loginStore,
                password = state.data?.password.orEmpty(),
                error = state.message
            )
        }
    }
}

@Composable
private fun LoginScreen(
    login: String,
    scope: CoroutineScope,
    loginStore: LoginStore,
    password: String,
    error: String
) {
    Column(
        modifier = Modifier.background(Color.White).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(modifier = Modifier.padding(16.dp)) {
            TextField(login, {
                loginStore.onLoginChanged(it)
            })
        }
        Box(modifier = Modifier.padding(16.dp)) {
            TextField(password, {
                loginStore.onPasswordChanged(it)
            })
        }
        Row(horizontalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.padding(16.dp)) {
                TextButton({ loginStore.login() }) {
                    Text("Login")
                }
            }
        }
        Spacer(Modifier.size(16.dp))
        if (error.isNotEmpty()) {
            Text(text = error, fontSize = 16.sp)
        }
    }
}

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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.todo.screen.login.model.LoginData
import com.marzec.todo.screen.login.model.LoginStore
import com.marzec.view.ActionBarProvider
import com.marzec.view.TextFieldStateful

@Composable
fun LoginScreen(
    actionBarProvider: ActionBarProvider,
    store: LoginStore
) {

    val state: State<LoginData> by store.collectState()

    when (val state = state) {
        is State.Data -> {
            LoginScreen(
                actionBarProvider = actionBarProvider,
                login = state.data.login,
                store = store,
                password = state.data.password,
                error = ""
            )
        }
        is State.Loading -> {
            Text(text = "Pending...", fontSize = 16.sp)

        }
        is State.Error -> {
            LoginScreen(
                actionBarProvider = actionBarProvider,
                login = state.data?.login.orEmpty(),
                store = store,
                password = state.data?.password.orEmpty(),
                error = state.message
            )
        }
    }
}

@Composable
private fun LoginScreen(
    actionBarProvider: ActionBarProvider,
    login: String,
    store: LoginStore,
    password: String,
    error: String
) {
    Scaffold(
        topBar = {
            actionBarProvider.provide {
                IconButton({
                    store.onFeatureFlagClicked()
                }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Feature Toggle")
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(modifier = Modifier.padding(16.dp)) {
                TextFieldStateful(login, {
                    store.onLoginChanged(it)
                })
            }
            Box(modifier = Modifier.padding(16.dp)) {
                TextFieldStateful(password, {
                    store.onPasswordChanged(it)
                })
            }
            Row(horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.padding(16.dp)) {
                    TextButton({ store.login() }) {
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
}

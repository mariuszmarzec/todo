package com.marzec.todo.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.mvi.Store
import com.marzec.todo.DI
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationActions
import com.marzec.todo.navigation.model.NavigationOptions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.repository.LoginRepository
import kotlinx.coroutines.launch

@Composable
fun ActionBar(store: NavigationStore, title: String) {
    val scope = rememberCoroutineScope()

    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton({
            scope.launch { store.sendAction(NavigationActions.Back) }
        }) {
            Text(text = "Back")
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = title)
        Spacer(modifier = Modifier.weight(1f))
        TextButton({
            scope.launch { topBarStore.logout() }
        }) {
            Text(text = "Logout")
        }
    }
}

val topBarStore by lazy {
    TopBarStore(DI.navigationStore)
}

class TopBarStore(
    private val store: NavigationStore
) : Store<Unit, TopBarStore.Logout>(Unit) {

    init {
        addIntent<Logout> {
            onTrigger {
                LoginRepository(DI.client).logout()
            }
            sideEffect {
                store.sendAction(
                    NavigationActions.Next(
                        destination = Destination.Login,
                        NavigationOptions(
                            Destination.Login,
                            popToInclusive = true
                        )
                    )
                )
            }
        }
    }

    suspend fun logout() {
        sendAction(Logout)
    }

    object Logout
}
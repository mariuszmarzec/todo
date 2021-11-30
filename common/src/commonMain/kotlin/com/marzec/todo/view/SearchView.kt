package com.marzec.todo.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

data class SearchState(
    val value: String,
    val focused: Boolean
)

data class SearchViewState(
    val value: String,
    val focused: Boolean,
    val onFocusChanged: (Boolean) -> Unit,
    val onSearchQueryChanged: (String) -> Unit,
    val onClearButtonClick: () -> Unit,
    val onActivateSearchButtonClick: () -> Unit
)

@Composable
fun SearchView(state: SearchViewState) {
    Row(
        Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val searchInUse = state.focused || state.value.isNotEmpty()
        BasicTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    state.onFocusChanged.invoke(it.isFocused)
                }
                .widthIn(min = 200.dp, max = 300.dp)
                .padding(0.dp),
            singleLine = true,
            value = if (searchInUse) state.value else "Search",
            onValueChange = { state.onSearchQueryChanged(it) }
        )
        LaunchedEffect(key1 = state.focused) {
            if (state.focused) {
                focusRequester.requestFocus()
            } else {
                focusManager.clearFocus()

            }
        }
        if (searchInUse) {
            IconButton({
                state.onClearButtonClick()
            }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search"
                )
            }
        } else {
            IconButton({
                state.onActivateSearchButtonClick()
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Activate search"
                )
            }
        }
    }
}

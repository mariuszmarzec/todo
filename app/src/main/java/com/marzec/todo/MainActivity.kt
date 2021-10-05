package com.marzec.todo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.todo.screen.main.HomeScreen
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import com.marzec.todo.common.OpenUrlHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DI.openUrlHelper = object : OpenUrlHelper {
            override fun open(url: String) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    )
                )
            }
        }

        setContent {
            MaterialTheme(colors = lightColors()) {
                DI.logger.log("Navigation RECR", DI.navigationStore.state.value.backStack.map { it.destination }.toString())


                DI.navigationScope = rememberCoroutineScope()

                DI.logger.log("navigationStore", "content")
                HomeScreen(DI.navigationStore)
            }
        }
    }

    override fun onBackPressed() {
        if (DI.navigationStore.state.value.backStack.size > 1) {
            DI.logger.log("Navigation BACK", DI.navigationStore.state.value.backStack.map { it.destination }.toString())
            DI.navigationScope.launch {
                DI.navigationStore.goBack()
            }
        } else {
            super.onBackPressed()
        }
    }
}
package com.marzec.todo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.todo.screen.main.HomeScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colors = lightColors()) {

                DI.navigationScope = rememberCoroutineScope()

                DI.logger.log("navigationStore", "content")
                HomeScreen(DI.navigationStore)
            }
        }
    }
}
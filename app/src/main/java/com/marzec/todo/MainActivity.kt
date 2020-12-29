package com.marzec.todo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.setContent

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Greeting(name = "compose")
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text (text = "Hello $name!")
}

package com.marzec.todo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.common.OpenUrlHelper
import com.marzec.todo.screen.main.HomeScreenSaveable
import kotlinx.coroutines.launch

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

                DI.navigationScope = rememberCoroutineScope()

                HomeScreenSaveable()
            }
        }
    }

    override fun onBackPressed() {
        if (DI.navigationStore.state.value.backStack.size > 1) {
            DI.navigationScope.launch {
                DI.navigationStore.goBack()
            }
        } else {
            super.onBackPressed()
        }
    }
}

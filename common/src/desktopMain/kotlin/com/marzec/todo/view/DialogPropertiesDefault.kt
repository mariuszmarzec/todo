package com.marzec.todo.view

import androidx.compose.ui.window.DesktopDialogProperties
import androidx.compose.ui.window.DialogProperties

actual object DialogPropertiesDefault {
    actual val properties: DialogProperties = DesktopDialogProperties(resizable = false)
}
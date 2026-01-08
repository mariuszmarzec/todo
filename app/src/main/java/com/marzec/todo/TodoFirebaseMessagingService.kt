package com.marzec.todo

import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.marzec.todo.api.TaskDto
import android.content.Intent
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class TodoFirebaseMessagingService : FirebaseMessagingService() {

    private val coroutineScope = CoroutineScope(DI.ioDispatcher)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 1. Check if the message contains data
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val taskJson = remoteMessage.data["data"]

            if (type == "TASK_SCHEDULED" && taskJson != null) {
                // 2. Parse TaskDto (using Gson or Kotlin Serialization)
                val taskDto = Json.decodeFromString<TaskDto>(taskJson)

                // 3. Show the notification
                showNotification(taskDto)
            }
        }
    }

    private fun showNotification(task: TaskDto) {
        val channelId = "task_channel"

        // 4. Intent to open the app when clicked
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("task_id", task.id) // Optional: pass data to the activity
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("New Task Scheduled")
            .setContentText(task.description) // Showing the description field
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Open app on click
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "Tasks", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(task.id.hashCode(), builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        coroutineScope.launch {
            DI.deviceTokenRepository.saveToken(token)
        }
    }


}
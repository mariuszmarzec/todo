package com.marzec.todo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.marzec.cache.getTyped
import com.marzec.logger.Logger
import com.marzec.todo.api.TaskDto
import com.marzec.todo.repository.DeviceTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoFirebaseMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(DI.ioDispatcher + job)

    private val deviceTokenRepository: DeviceTokenRepository by lazy {
        DI.deviceTokenRepository
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 1. Check if the message contains data
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val taskJson = remoteMessage.data["data"]

            if (taskJson != null) {
                // 2. Parse TaskDto
                val taskDto = DI.json.decodeFromString<TaskDto>(taskJson)

                when (type) {
                    "TASK_SCHEDULED" -> {
                        // 3. Show the notification
                        showNotification(taskDto)
                    }
                    "TASK_REMOVED" -> {
                        // 4. Cancel the notification
                        cancelNotification(taskDto)
                    }
                }
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
            .setContentTitle("New Task:")
            .setContentText(task.description) // Showing the description field
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Open app on click
            .setAutoCancel(true)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel =
            NotificationChannel(channelId, "Tasks", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(task.id.hashCode(), builder.build())
    }

    private fun cancelNotification(task: TaskDto) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(task.id.hashCode())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Logger.logger.log("TodoFirebaseMessagingService", token)
        coroutineScope.launch {
            deviceTokenRepository.updateToken(
                token = token,
                isLogged = DI.fileCache.getTyped<String>(PreferencesKeys.AUTHORIZATION) != null
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

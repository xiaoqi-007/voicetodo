package com.example.voicetodo.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.voicetodo.MainActivity
import com.example.voicetodo.R

object NotificationHelper {

    const val CHANNEL_ID = "todo_reminder_channel"
    private const val CHANNEL_NAME = "待办提醒"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "待办事项提醒通知"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun showReminderNotification(
        context: Context,
        todoId: Int,
        task: String,
        isAlarm: Boolean
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            todoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (isAlarm) "闹钟提醒" else "待办提醒")
            .setContentText(task)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        if (isAlarm) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(todoId, builder.build())
    }
}

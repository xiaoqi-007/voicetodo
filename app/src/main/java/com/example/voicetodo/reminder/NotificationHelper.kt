package com.example.voicetodo.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.voicetodo.MainActivity

object NotificationHelper {

    const val CHANNEL_ID_REMINDER = "todo_reminder_channel"
    const val CHANNEL_ID_ALARM = "todo_alarm_channel"
    private const val REMINDER_NAME = "待办提醒"
    private const val ALARM_NAME = "闹钟提醒"

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val reminderChannel = NotificationChannel(
            CHANNEL_ID_REMINDER, REMINDER_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "待办事项提醒通知"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
        }

        val alarmChannel = NotificationChannel(
            CHANNEL_ID_ALARM, ALARM_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "闹钟提醒（全屏弹窗）"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(soundUri, audioAttr)
        }

        manager.createNotificationChannel(reminderChannel)
        manager.createNotificationChannel(alarmChannel)
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
            context, todoId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isAlarm) CHANNEL_ID_ALARM else CHANNEL_ID_REMINDER
        val defaultSound = if (isAlarm) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (isAlarm) "闹钟提醒" else "待办提醒")
            .setContentText(task)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setVibrate(if (isAlarm) longArrayOf(0, 500, 200, 500, 200, 500) else longArrayOf(0, 300, 200, 300))
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        // 闹钟模式：全屏弹窗（锁屏也能看到）
        if (isAlarm) {
            builder.setFullScreenIntent(pendingIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(todoId, builder.build())
    }
}

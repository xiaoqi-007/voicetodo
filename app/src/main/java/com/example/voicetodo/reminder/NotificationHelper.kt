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

    const val CHANNEL_ID_REMINDER = "todo_reminder"
    const val CHANNEL_ID_ALARM = "todo_alarm"

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        // 普通提醒通道
        val reminder = NotificationChannel(
            CHANNEL_ID_REMINDER, "待办提醒", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "待办事项提醒"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
        }

        // 闹钟通道（更高优先级）
        val alarm = NotificationChannel(
            CHANNEL_ID_ALARM, "闹钟提醒", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "闹钟级别提醒"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            try {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(soundUri, attr)
            } catch (_: Exception) { }
        }

        manager.createNotificationChannel(reminder)
        manager.createNotificationChannel(alarm)
    }

    fun showReminderNotification(
        context: Context,
        todoId: Int,
        task: String,
        isAlarm: Boolean
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, todoId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isAlarm) CHANNEL_ID_ALARM else CHANNEL_ID_REMINDER
        val soundUri = if (isAlarm) {
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
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setVibrate(if (isAlarm) longArrayOf(0, 500, 200, 500, 200, 500) else longArrayOf(0, 300, 200, 300))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        // 闹钟模式：全屏弹窗
        if (isAlarm) {
            try {
                builder.setFullScreenIntent(pendingIntent, true)
            } catch (_: Exception) {
                // 部分设备不支持全屏 intent，忽略
            }
        }

        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.notify(todoId, builder.build())
        } catch (e: Exception) {
            // 最终兜底：用最基础的通知
            try {
                val nm = context.getSystemService(NotificationManager::class.java)
                val n = android.app.Notification.Builder(context, CHANNEL_ID_REMINDER)
                    .setContentTitle("待办提醒")
                    .setContentText(task)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setAutoCancel(true)
                    .build()
                nm.notify(todoId, n)
            } catch (_: Exception) { }
        }
    }
}

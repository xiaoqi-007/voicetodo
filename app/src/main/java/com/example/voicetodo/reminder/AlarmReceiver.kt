package com.example.voicetodo.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getIntExtra("todo_id", 0)
        val todoTask = intent.getStringExtra("todo_task") ?: "待办提醒"
        val isAlarm = intent.getBooleanExtra("is_alarm", false)

        // 唤醒锁，防止系统休眠
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "voicetodo:alarm_$todoId"
        )
        try {
            wakeLock.acquire(15_000L)

            NotificationHelper.showReminderNotification(
                context = context,
                todoId = todoId,
                task = todoTask,
                isAlarm = isAlarm
            )
        } catch (e: Exception) {
            // 降级：用最简单的通知
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val n = android.app.Notification.Builder(context, NotificationHelper.CHANNEL_ID_REMINDER)
                    .setContentTitle("待办提醒")
                    .setContentText(todoTask)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setAutoCancel(true)
                    .build()
                nm.notify(todoId, n)
            } catch (_: Exception) { }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}

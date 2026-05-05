package com.example.voicetodo.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getIntExtra("todo_id", 0)
        val todoTask = intent.getStringExtra("todo_task") ?: "待办提醒"
        val isAlarm = intent.getBooleanExtra("is_alarm", false)

        // 获取唤醒锁，确保通知能显示
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "voicetodo:alarm_wakeup"
        )
        wakeLock.acquire(10_000L) // 最多持有 10 秒

        try {
            NotificationHelper.showReminderNotification(
                context = context,
                todoId = todoId,
                task = todoTask,
                isAlarm = isAlarm
            )
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}

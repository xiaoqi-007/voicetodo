package com.example.voicetodo.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getIntExtra("todo_id", 0)
        val todoTask = intent.getStringExtra("todo_task") ?: "待办提醒"
        val isAlarm = intent.getBooleanExtra("is_alarm", false)

        NotificationHelper.showReminderNotification(
            context = context,
            todoId = todoId,
            task = todoTask,
            isAlarm = isAlarm
        )
    }
}

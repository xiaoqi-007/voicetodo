package com.example.voicetodo.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.voicetodo.data.TodoItem

object AlarmScheduler {

    fun schedule(context: Context, todo: TodoItem) {
        val remindTime = todo.remindTime ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("todo_id", todo.id)
            putExtra("todo_task", todo.task)
            putExtra("is_alarm", todo.isAlarm)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todo.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        remindTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    remindTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // 降级为非精确闹钟
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                remindTime,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, todoId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}

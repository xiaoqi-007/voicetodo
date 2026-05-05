package com.example.voicetodo.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.voicetodo.MainActivity
import com.example.voicetodo.data.TodoItem

object AlarmScheduler {

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true
    }

    fun getExactAlarmSettingsIntent(context: Context): Intent {
        return Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun schedule(context: Context, todo: TodoItem) {
        val remindTime = todo.remindTime ?: return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("todo_id", todo.id)
            putExtra("todo_task", todo.task)
            putExtra("is_alarm", todo.isAlarm)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, todo.id, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 统一使用 setAlarmClock：系统级闹钟，国产手机也不会杀
        val showIntent = PendingIntent.getActivity(
            context, todo.id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            am.setAlarmClock(
                AlarmManager.AlarmClockInfo(remindTime, showIntent),
                pendingIntent
            )
        } catch (e: Exception) {
            // 极端情况兜底
            try {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, remindTime, pendingIntent
                )
            } catch (_: Exception) { }
        }
    }

    fun cancel(context: Context, todoId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, todoId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pendingIntent)
    }
}

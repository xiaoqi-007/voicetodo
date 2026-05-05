package com.example.voicetodo.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.voicetodo.data.TodoItem

object AlarmScheduler {

    /** 检查是否有精确闹钟权限 */
    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /** 获取跳转到精确闹钟设置页的 Intent */
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
            if (todo.isAlarm) {
                // 闹钟模式：使用 setAlarmClock，系统级优先级，不会被电池优化杀掉
                val showIntent = PendingIntent.getActivity(
                    context, todo.id,
                    Intent(context, com.example.voicetodo.MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(remindTime, showIntent),
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+：检查精确闹钟权限
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, remindTime, pendingIntent
                    )
                } else {
                    // 没权限，用 setAlarmClock（优先级最高，不受限制）
                    val showIntent = PendingIntent.getActivity(
                        context, todo.id,
                        Intent(context, com.example.voicetodo.MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(remindTime, showIntent),
                        pendingIntent
                    )
                }
            } else {
                // Android 12 以下
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, remindTime, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // 最终兜底：setAlarmClock 不需要 SCHEDULE_EXACT_ALARM 权限
            try {
                val showIntent = PendingIntent.getActivity(
                    context, todo.id,
                    Intent(context, com.example.voicetodo.MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(remindTime, showIntent),
                    pendingIntent
                )
            } catch (_: Exception) {
                // 彻底失败
            }
        }
    }

    fun cancel(context: Context, todoId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, todoId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}

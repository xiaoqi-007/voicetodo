package com.example.voicetodo.reminder

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.util.Calendar

object SystemAlarmHelper {

    /**
     * 直接调用系统闹钟 App 设置闹钟
     * 这是最可靠的方式：系统闹钟不会被杀，锁屏也能响
     */
    fun setSystemAlarm(context: Context, hour: Int, minute: Int, label: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false) // 显示确认界面让用户看到
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 根据时间戳设置系统闹钟
     */
    fun setSystemAlarmFromTimestamp(context: Context, timeMillis: Long, label: String): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return setSystemAlarm(context, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), label)
    }

    /**
     * 设置系统计时器（倒计时提醒）
     */
    fun setSystemTimer(context: Context, seconds: Int, label: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

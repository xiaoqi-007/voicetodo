package com.example.voicetodo.reminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log
import android.widget.Toast
import java.util.Calendar

object SystemAlarmHelper {

    private const val TAG = "SystemAlarm"

    /**
     * 直接调用系统闹钟 App 设置闹钟
     */
    fun setSystemAlarm(context: Context, hour: Int, minute: Int, label: String): Boolean {
        return try {
            // 方式1：标准 AlarmClock Intent
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Log.d(TAG, "尝试设置闹钟: ${hour}:${minute} - $label")

            // 直接 startActivity，不检查 resolveActivity（Android 11+ 包可见性会导致误判）
            context.startActivity(intent)
            Toast.makeText(context, "已打开系统闹钟: ${hour}:${String.format("%02d", minute)}", Toast.LENGTH_SHORT).show()
            true

        } catch (e: Exception) {
            Log.e(TAG, "标准 Intent 失败: ${e.message}")

            // 方式2：某些国产手机用自定义 URI
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("alarm://create?hour=$hour&minute=$minute&message=$label")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                Toast.makeText(context, "已打开系统闹钟: ${hour}:${String.format("%02d", minute)}", Toast.LENGTH_SHORT).show()
                true
            } catch (e2: Exception) {
                Log.e(TAG, "备选 Intent 也失败: ${e2.message}")

                // 方式3：打开系统闹钟 App 让用户手动设置
                try {
                    val openClockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(openClockIntent)
                    Toast.makeText(context, "已打开闹钟，请手动添加", Toast.LENGTH_LONG).show()
                    true
                } catch (e3: Exception) {
                    Log.e(TAG, "所有方式均失败: ${e3.message}")
                    Toast.makeText(context, "无法打开系统闹钟，请手动设置", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }
    }

    /**
     * 根据时间戳设置系统闹钟
     */
    fun setSystemAlarmFromTimestamp(context: Context, timeMillis: Long, label: String): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        Log.d(TAG, "从时间戳设置闹钟: $timeMillis -> ${hour}:${minute}")
        return setSystemAlarm(context, hour, minute, label)
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
            context.startActivity(intent)
            Toast.makeText(context, "已打开系统计时器", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Log.e(TAG, "设置计时器失败: ${e.message}")
            false
        }
    }
}

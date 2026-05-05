package com.example.voicetodo.voice

import java.util.Calendar
import java.util.regex.Pattern

data class ParsedTodo(
    val task: String,
    val remindTime: Long? = null,
    val isAlarm: Boolean = false
)

class TextParser {

    private val numberMap = mapOf(
        "一" to 1, "二" to 2, "两" to 2, "三" to 3, "四" to 4,
        "五" to 5, "六" to 6, "七" to 7, "八" to 8, "九" to 9,
        "十" to 10, "十一" to 11, "十二" to 12, "十三" to 13,
        "十四" to 14, "十五" to 15, "十六" to 16, "十七" to 17,
        "十八" to 18, "十九" to 19, "二十" to 20, "二十一" to 21,
        "二十二" to 22, "二十三" to 23, "二十四" to 24, "二十五" to 25,
        "二十六" to 26, "二十七" to 27, "二十八" to 28, "二十九" to 29,
        "三十" to 30, "三十一" to 31, "半" to 30
    )

    private val chineseNumPattern = "[一二两三四五六七八九十]+"

    // 相对时间模式: "X分钟/小时/天后"
    private val relativeTimePattern = Pattern.compile(
        "(\\d+|$chineseNumPattern)(分钟|小时|天|秒钟?)后"
    )

    // 绝对时间模式: "上午/下午 X点X分"
    private val absoluteTimePattern = Pattern.compile(
        "(今天|明天|后天|大后天)?(上午|下午|晚上|早上|早晨|中午)?(\\d+|$chineseNumPattern)点(?:(\\d+|$chineseNumPattern)分?)?"
    )

    // 闹钟关键词
    private val alarmKeywords = listOf("闹钟", "闹铃", "定时")

    // 提醒关键词
    private val reminderKeywords = listOf("提醒", "记得", "别忘", "通知")

    fun parse(input: String): ParsedTodo {
        var text = input.trim()
        var remindTime: Long? = null
        var isAlarm = false

        // 检测闹钟模式
        for (keyword in alarmKeywords) {
            if (text.contains(keyword)) {
                isAlarm = true
                text = text.replace(keyword, "").trim()
                break
            }
        }

        // 尝试解析相对时间
        val relativeMatch = relativeTimePattern.matcher(text)
        if (relativeMatch.find()) {
            val numberStr = relativeMatch.group(1)!!
            val unit = relativeMatch.group(2)!!
            val number = parseNumber(numberStr)

            val calendar = Calendar.getInstance()
            when (unit) {
                "秒钟", "秒" -> calendar.add(Calendar.SECOND, number)
                "分钟" -> calendar.add(Calendar.MINUTE, number)
                "小时" -> calendar.add(Calendar.HOUR, number)
                "天" -> calendar.add(Calendar.DAY_OF_MONTH, number)
            }
            remindTime = calendar.timeInMillis

            // 移除时间部分，提取任务
            text = text.substring(0, relativeMatch.start()) +
                    text.substring(relativeMatch.end())
        } else {
            // 尝试解析绝对时间
            val absoluteMatch = absoluteTimePattern.matcher(text)
            if (absoluteMatch.find()) {
                val dayOffset = parseDayOffset(absoluteMatch.group(1))
                val period = absoluteMatch.group(2)
                val hourStr = absoluteMatch.group(3)!!
                val minuteStr = absoluteMatch.group(4)

                var hour = parseNumber(hourStr)
                val minute = if (minuteStr != null) parseNumber(minuteStr) else 0

                // 处理上午/下午
                if (period != null) {
                    when (period) {
                        "下午", "晚上" -> if (hour < 12) hour += 12
                        "上午", "早上", "早晨" -> if (hour == 12) hour = 0
                        "中午" -> if (hour < 12) hour = 12
                    }
                }

                val calendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, dayOffset)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // 如果计算出的时间已经过去，推到明天
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                remindTime = calendar.timeInMillis

                // 移除时间部分，提取任务
                text = text.substring(0, absoluteMatch.start()) +
                        text.substring(absoluteMatch.end())
            }
        }

        // 清理任务文本
        text = cleanTaskText(text)

        return ParsedTodo(
            task = text,
            remindTime = remindTime,
            isAlarm = isAlarm
        )
    }

    private fun parseNumber(str: String): Int {
        return str.toIntOrNull() ?: numberMap[str] ?: 0
    }

    private fun parseDayOffset(dayStr: String?): Int {
        return when (dayStr) {
            "明天" -> 1
            "后天" -> 2
            "大后天" -> 3
            else -> 0
        }
    }

    private fun cleanTaskText(text: String): String {
        var result = text
        // 移除常见无意义词汇
        val removeWords = listOf("帮我", "我要", "我需要", "请", "一下", "吧")
        for (word in removeWords) {
            result = result.replace(word, "")
        }
        // 移除多余标点和空格
        result = result.replace(Regex("[，。、！？,\\s]+"), " ").trim()
        return result
    }
}

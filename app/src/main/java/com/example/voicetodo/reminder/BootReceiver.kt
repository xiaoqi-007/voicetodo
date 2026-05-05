package com.example.voicetodo.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicetodo.data.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = TodoDatabase.getDatabase(context)
                    val reminders = db.todoDao().getPendingReminders()
                    reminders.forEach { todo ->
                        AlarmScheduler.schedule(context, todo)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

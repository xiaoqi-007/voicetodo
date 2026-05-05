package com.example.voicetodo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicetodo.data.TodoDatabase
import com.example.voicetodo.data.TodoItem
import com.example.voicetodo.reminder.AlarmScheduler
import com.example.voicetodo.reminder.SystemAlarmHelper
import com.example.voicetodo.voice.ParsedTodo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TodoDatabase.getDatabase(application)
    private val todoDao = database.todoDao()

    val todos: StateFlow<List<TodoItem>> = todoDao.getAllTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTodo(parsed: ParsedTodo) {
        viewModelScope.launch {
            val todo = TodoItem(
                task = parsed.task,
                remindTime = parsed.remindTime,
                isAlarm = parsed.isAlarm
            )
            val id = todoDao.insert(todo).toInt()

            if (parsed.remindTime != null) {
                val savedTodo = todo.copy(id = id)

                if (parsed.isAlarm) {
                    // 闹钟模式：直接调用系统闹钟 App
                    val success = SystemAlarmHelper.setSystemAlarmFromTimestamp(
                        getApplication(), parsed.remindTime, parsed.task
                    )
                    if (!success) {
                        // 系统闹钟不可用，兜底用 AlarmManager
                        AlarmScheduler.schedule(getApplication(), savedTodo)
                    }
                } else {
                    // 提醒模式：用 AlarmManager + 通知
                    AlarmScheduler.schedule(getApplication(), savedTodo)
                }
            }
        }
    }

    fun toggleComplete(todo: TodoItem) {
        viewModelScope.launch {
            val updated = todo.copy(isCompleted = !todo.isCompleted)
            todoDao.update(updated)
            if (updated.isCompleted && updated.remindTime != null) {
                AlarmScheduler.cancel(getApplication(), updated.id)
            }
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            todoDao.delete(todo)
            if (todo.remindTime != null) {
                AlarmScheduler.cancel(getApplication(), todo.id)
            }
        }
    }

    fun deleteCompleted() {
        viewModelScope.launch {
            todoDao.deleteCompleted()
        }
    }

    /**
     * 手动触发：把待办设为系统闹钟
     */
    fun setAsSystemAlarm(todo: TodoItem) {
        val time = todo.remindTime ?: return
        SystemAlarmHelper.setSystemAlarmFromTimestamp(getApplication(), time, todo.task)
    }
}

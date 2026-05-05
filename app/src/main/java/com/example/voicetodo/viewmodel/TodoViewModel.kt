package com.example.voicetodo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicetodo.data.TodoDatabase
import com.example.voicetodo.data.TodoItem
import com.example.voicetodo.reminder.AlarmScheduler
import com.example.voicetodo.voice.ParsedTodo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

            // 如果有提醒时间，设置闹钟
            if (parsed.remindTime != null) {
                val savedTodo = todo.copy(id = id)
                AlarmScheduler.schedule(getApplication(), savedTodo)
            }
        }
    }

    fun toggleComplete(todo: TodoItem) {
        viewModelScope.launch {
            val updated = todo.copy(isCompleted = !todo.isCompleted)
            todoDao.update(updated)

            // 如果标记完成，取消闹钟
            if (updated.isCompleted && updated.remindTime != null) {
                AlarmScheduler.cancel(getApplication(), updated.id)
            }
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            todoDao.delete(todo)
            // 取消闹钟
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
}

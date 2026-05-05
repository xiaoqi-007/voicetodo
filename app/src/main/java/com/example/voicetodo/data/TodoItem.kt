package com.example.voicetodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val task: String,
    val createdAt: Long = System.currentTimeMillis(),
    val remindTime: Long? = null,
    val isCompleted: Boolean = false,
    val isAlarm: Boolean = false
)

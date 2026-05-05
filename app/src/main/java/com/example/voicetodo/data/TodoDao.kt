package com.example.voicetodo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Int): TodoItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoItem): Long

    @Update
    suspend fun update(todo: TodoItem)

    @Delete
    suspend fun delete(todo: TodoItem)

    @Query("UPDATE todos SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Int)

    @Query("DELETE FROM todos WHERE isCompleted = 1")
    suspend fun deleteCompleted()

    @Query("SELECT * FROM todos WHERE remindTime IS NOT NULL AND remindTime > :now AND isCompleted = 0")
    suspend fun getPendingReminders(now: Long = System.currentTimeMillis()): List<TodoItem>
}

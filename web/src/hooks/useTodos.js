import { useState, useEffect, useCallback } from 'react'
import { scheduleReminder, cancelReminder, restoreReminders } from '../services/reminder'

const STORAGE_KEY = 'voice_todos'

function loadTodos() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : []
  } catch {
    return []
  }
}

function saveTodos(todos) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(todos))
}

export function useTodos() {
  const [todos, setTodos] = useState(loadTodos)

  // 保存到localStorage
  useEffect(() => {
    saveTodos(todos)
  }, [todos])

  // 恢复提醒
  useEffect(() => {
    restoreReminders(todos)
  }, [])

  const addTodos = useCallback((parsedTodos) => {
    const newTodos = parsedTodos.map(parsed => ({
      id: Date.now() + Math.random(),
      task: parsed.task,
      createdAt: Date.now(),
      remindTime: parsed.remindTime,
      isAlarm: parsed.isAlarm,
      isCompleted: false,
    }))

    setTodos(prev => {
      const updated = [...newTodos, ...prev]
      // 为有提醒时间的任务设置定时器
      newTodos.forEach(todo => {
        if (todo.remindTime) {
          scheduleReminder(todo)
        }
      })
      return updated
    })

    return newTodos
  }, [])

  const toggleComplete = useCallback((id) => {
    setTodos(prev => {
      const updated = prev.map(todo => {
        if (todo.id === id) {
          const newTodo = { ...todo, isCompleted: !todo.isCompleted }
          if (newTodo.isCompleted && newTodo.remindTime) {
            cancelReminder(newTodo.id)
          }
          return newTodo
        }
        return todo
      })
      return updated
    })
  }, [])

  const deleteTodo = useCallback((id) => {
    cancelReminder(id)
    setTodos(prev => prev.filter(todo => todo.id !== id))
  }, [])

  const clearCompleted = useCallback(() => {
    setTodos(prev => {
      prev.filter(t => t.isCompleted).forEach(t => cancelReminder(t.id))
      return prev.filter(todo => !todo.isCompleted)
    })
  }, [])

  return {
    todos,
    addTodos,
    toggleComplete,
    deleteTodo,
    clearCompleted,
  }
}

const activeTimers = new Map()

export async function requestNotificationPermission() {
  if (!('Notification' in window)) {
    console.warn('浏览器不支持通知')
    return false
  }

  if (Notification.permission === 'granted') {
    return true
  }

  if (Notification.permission !== 'denied') {
    const permission = await Notification.requestPermission()
    return permission === 'granted'
  }

  return false
}

export function scheduleReminder(todo) {
  if (!todo.remindTime) return

  // 清除已有的定时器
  cancelReminder(todo.id)

  const now = Date.now()
  const delay = todo.remindTime - now

  if (delay <= 0) return

  const timer = setTimeout(() => {
    showNotification(todo)
    activeTimers.delete(todo.id)
  }, delay)

  activeTimers.set(todo.id, timer)
}

export function cancelReminder(todoId) {
  const timer = activeTimers.get(todoId)
  if (timer) {
    clearTimeout(timer)
    activeTimers.delete(todoId)
  }
}

function showNotification(todo) {
  if (Notification.permission !== 'granted') return

  const notification = new Notification(
    todo.isAlarm ? '⏰ 闹钟提醒' : '📋 待办提醒',
    {
      body: todo.task,
      icon: '/vite.svg',
      tag: `todo-${todo.id}`,
      requireInteraction: todo.isAlarm,
    }
  )

  notification.onclick = () => {
    window.focus()
    notification.close()
  }

  // 播放提示音
  playNotificationSound(todo.isAlarm)
}

function playNotificationSound(isAlarm) {
  try {
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()

    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)

    oscillator.frequency.value = isAlarm ? 800 : 520
    oscillator.type = 'sine'
    gainNode.gain.value = 0.3

    oscillator.start()

    if (isAlarm) {
      // 闹钟模式：响3次
      let count = 0
      const interval = setInterval(() => {
        count++
        oscillator.frequency.value = count % 2 === 0 ? 800 : 600
        if (count >= 5) {
          clearInterval(interval)
          oscillator.stop()
        }
      }, 300)
    } else {
      setTimeout(() => oscillator.stop(), 200)
    }
  } catch (e) {
    // 音频播放失败忽略
  }
}

// 页面加载时恢复所有未触发的提醒
export function restoreReminders(todos) {
  todos.forEach(todo => {
    if (todo.remindTime && todo.remindTime > Date.now() && !todo.isCompleted) {
      scheduleReminder(todo)
    }
  })
}

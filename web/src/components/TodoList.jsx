export function TodoList({ todos, onToggle, onDelete }) {
  if (todos.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">📋</div>
        <p>暂无待办事项</p>
        <p className="empty-hint">点击下方麦克风按钮开始语音输入</p>
      </div>
    )
  }

  return (
    <div className="todo-list">
      {todos.map(todo => (
        <TodoItem
          key={todo.id}
          todo={todo}
          onToggle={() => onToggle(todo.id)}
          onDelete={() => onDelete(todo.id)}
        />
      ))}
    </div>
  )
}

function TodoItem({ todo, onToggle, onDelete }) {
  const formatTime = (timestamp) => {
    if (!timestamp) return ''
    const now = Date.now()
    const diff = timestamp - now

    const date = new Date(timestamp)
    const exactTime = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

    if (diff < 0) return `已过期 (${exactTime})`
    if (diff < 60000) return `即将提醒 (${exactTime})`
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟后 (${exactTime})`

    const today = new Date()
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)

    if (date.toDateString() === today.toDateString()) {
      return `今天 ${exactTime}`
    }
    if (date.toDateString() === tomorrow.toDateString()) {
      return `明天 ${exactTime}`
    }
    return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  }

  return (
    <div className={`todo-item ${todo.isCompleted ? 'completed' : ''}`}>
      <label className="todo-checkbox">
        <input
          type="checkbox"
          checked={todo.isCompleted}
          onChange={onToggle}
        />
        <span className="checkmark" />
      </label>

      <div className="todo-content">
        <span className="todo-task">{todo.task}</span>
        {todo.remindTime && (
          <span className="todo-time">
            {todo.isAlarm ? '⏰' : '🔔'} {formatTime(todo.remindTime)}
          </span>
        )}
      </div>

      <button className="delete-btn" onClick={onDelete} title="删除">
        ✕
      </button>
    </div>
  )
}

import { useState, useEffect } from 'react'
import { VoiceButton } from './components/VoiceButton'
import { TodoList } from './components/TodoList'
import { UpdateDialog } from './components/UpdateDialog'
import { useVoiceRecognition } from './hooks/useVoiceRecognition'
import { useTodos } from './hooks/useTodos'
import { parseWithLLM } from './services/llm'
import { requestNotificationPermission } from './services/reminder'
import { checkUpdate, APP_VERSION } from './services/updater'
import './App.css'

function App() {
  const { state, transcript, partialText, startListening, stopListening, reset } = useVoiceRecognition()
  const { todos, addTodos, toggleComplete, deleteTodo, clearCompleted } = useTodos()
  const [isParsing, setIsParsing] = useState(false)
  const [statusText, setStatusText] = useState('')
  const [updateInfo, setUpdateInfo] = useState(null)

  useEffect(() => {
    requestNotificationPermission()
    // 启动时检查更新
    checkUpdate().then(info => {
      if (info) setUpdateInfo(info)
    })
  }, [])

  useEffect(() => {
    if (!transcript) return

    const processText = async () => {
      setIsParsing(true)
      setStatusText('正在用大模型解析...')

      try {
        const parsed = await parseWithLLM(transcript)
        if (parsed.length > 0) {
          addTodos(parsed)
          const tasks = parsed.map(p => p.task).join('、')
          setStatusText(`已添加: ${tasks}`)
        } else {
          setStatusText('未识别到待办内容')
        }
      } catch (err) {
        setStatusText(`解析失败: ${err.message}`)
      } finally {
        setIsParsing(false)
        setTimeout(() => {
          setStatusText('')
          reset()
        }, 3000)
      }
    }

    processText()
  }, [transcript, addTodos, reset])

  const activeCount = todos.filter(t => !t.isCompleted).length
  const completedCount = todos.filter(t => t.isCompleted).length

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-left">
          <h1>语音待办</h1>
          <span className="todo-count">{activeCount} 项待办</span>
        </div>
        <div className="header-right">
          {completedCount > 0 && (
            <button className="clear-btn" onClick={clearCompleted}>
              清除已完成 ({completedCount})
            </button>
          )}
          <span className="version-tag">v{APP_VERSION}</span>
        </div>
      </header>

      <main className="app-main">
        <TodoList
          todos={todos}
          onToggle={toggleComplete}
          onDelete={deleteTodo}
        />
      </main>

      <footer className="app-footer">
        {statusText && (
          <div className={`status-text ${isParsing ? 'parsing' : ''}`}>
            {statusText}
          </div>
        )}
        <VoiceButton
          state={state}
          partialText={partialText}
          onStart={startListening}
          onStop={stopListening}
        />
      </footer>

      {updateInfo && (
        <UpdateDialog
          version={updateInfo.version}
          url={updateInfo.url}
          onClose={() => setUpdateInfo(null)}
        />
      )}
    </div>
  )
}

export default App

import { useState, useEffect } from 'react'
import { getLLMConfig, saveLLMConfig } from '../services/llm'

export function Settings({ isOpen, onClose }) {
  const [config, setConfig] = useState({
    apiUrl: '',
    apiKey: '',
    model: '',
  })

  useEffect(() => {
    if (isOpen) {
      setConfig(getLLMConfig())
    }
  }, [isOpen])

  const handleSave = () => {
    saveLLMConfig(config)
    onClose()
  }

  if (!isOpen) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>大模型配置</h2>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>

        <div className="form-group">
          <label>API地址</label>
          <input
            type="text"
            placeholder="https://api.example.com/v1"
            value={config.apiUrl}
            onChange={e => setConfig({ ...config, apiUrl: e.target.value })}
          />
          <span className="hint">OpenAI兼容格式的基础地址，如 https://api.openai.com/v1</span>
        </div>

        <div className="form-group">
          <label>API Key</label>
          <input
            type="password"
            placeholder="sk-..."
            value={config.apiKey}
            onChange={e => setConfig({ ...config, apiKey: e.target.value })}
          />
          <span className="hint">你的API密钥</span>
        </div>

        <div className="form-group">
          <label>模型名称</label>
          <input
            type="text"
            placeholder="gpt-3.5-turbo"
            value={config.model}
            onChange={e => setConfig({ ...config, model: e.target.value })}
          />
          <span className="hint">如 gpt-3.5-turbo, gpt-4, qwen-turbo 等</span>
        </div>

        <div className="modal-actions">
          <button className="btn-secondary" onClick={onClose}>取消</button>
          <button className="btn-primary" onClick={handleSave}>保存</button>
        </div>
      </div>
    </div>
  )
}

import { useState, useEffect } from 'react'

export function VoiceButton({ state, partialText, onStart, onStop }) {
  const isListening = state === 'listening'

  return (
    <div className="voice-section">
      {partialText && (
        <div className="partial-text">{partialText}</div>
      )}

      <button
        className={`voice-btn ${isListening ? 'listening' : ''}`}
        onClick={isListening ? onStop : onStart}
      >
        <div className="voice-btn-inner">
          {isListening ? (
            <svg viewBox="0 0 24 24" fill="currentColor" width="32" height="32">
              <rect x="6" y="6" width="12" height="12" rx="2" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" fill="currentColor" width="32" height="32">
              <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" />
              <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
            </svg>
          )}
        </div>
        {isListening && <div className="pulse-ring" />}
        {isListening && <div className="pulse-ring delay" />}
      </button>

      <div className="voice-hint">
        {isListening ? '点击停止识别' : '点击开始语音输入'}
      </div>
    </div>
  )
}

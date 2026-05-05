import { useState, useCallback, useRef } from 'react'

export function useVoiceRecognition() {
  const [state, setState] = useState('idle') // idle | listening | error
  const [transcript, setTranscript] = useState('')
  const [partialText, setPartialText] = useState('')
  const recognitionRef = useRef(null)

  const startListening = useCallback(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition

    if (!SpeechRecognition) {
      setState('error')
      setTranscript('当前浏览器不支持语音识别，请使用Chrome')
      return
    }

    // 停止之前的识别
    if (recognitionRef.current) {
      recognitionRef.current.abort()
    }

    const recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'
    recognition.continuous = false
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    recognition.onstart = () => {
      setState('listening')
      setTranscript('')
      setPartialText('')
    }

    recognition.onresult = (event) => {
      let interimTranscript = ''
      let finalTranscript = ''

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        if (result.isFinal) {
          finalTranscript += result[0].transcript
        } else {
          interimTranscript += result[0].transcript
        }
      }

      if (finalTranscript) {
        setTranscript(finalTranscript)
        setPartialText('')
        setState('idle')
      } else if (interimTranscript) {
        setPartialText(interimTranscript)
      }
    }

    recognition.onerror = (event) => {
      setState('error')
      const messages = {
        'no-speech': '未检测到语音，请再试一次',
        'audio-capture': '未找到麦克风设备',
        'not-allowed': '麦克风权限被拒绝，请允许访问麦克风',
        'network': '网络错误，语音识别需要网络连接',
        'aborted': '识别已取消',
      }
      setTranscript(messages[event.error] || `识别错误: ${event.error}`)
    }

    recognition.onend = () => {
      if (state === 'listening') {
        setState('idle')
      }
    }

    recognitionRef.current = recognition
    recognition.start()
  }, [state])

  const stopListening = useCallback(() => {
    if (recognitionRef.current) {
      recognitionRef.current.stop()
    }
    setState('idle')
    setPartialText('')
  }, [])

  const reset = useCallback(() => {
    setState('idle')
    setTranscript('')
    setPartialText('')
  }, [])

  return {
    state,
    transcript,
    partialText,
    startListening,
    stopListening,
    reset,
  }
}

package com.example.voicetodo.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VoiceRecognizer(private val context: Context) {

    sealed class RecognitionState {
        data object Idle : RecognitionState()
        data object Listening : RecognitionState()
        data class PartialResult(val text: String) : RecognitionState()
        data class Result(val text: String) : RecognitionState()
        data class Error(val message: String) : RecognitionState()
        // 新增：需要通过 Intent 启动外部语音识别
        data object NeedExternalRecognition : RecognitionState()
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出待办事项")
    }

    /** 获取用于启动外部语音识别的 Intent */
    fun getExternalRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出待办事项")
        }
    }

    /** 处理外部语音识别返回的结果 */
    fun handleExternalResult(data: Intent?) {
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotEmpty()) {
            _state.value = RecognitionState.Result(text)
        } else {
            _state.value = RecognitionState.Error("未识别到内容")
        }
    }

    /** 外部语音识别取消 */
    fun handleExternalCancelled() {
        _state.value = RecognitionState.Idle
    }

    fun startListening() {
        // 先检查系统级 SpeechRecognizer 是否可用
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            // 不可用，提示使用外部 Intent 方式
            _state.value = RecognitionState.NeedExternalRecognition
            return
        }

        speechRecognizer?.destroy()
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = RecognitionState.Listening
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "录音错误"
                            SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限"
                            SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                            SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务忙"
                            SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音输入"
                            else -> "识别错误: $error"
                        }
                        _state.value = RecognitionState.Error(message)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            _state.value = RecognitionState.Result(text)
                        } else {
                            _state.value = RecognitionState.Error("未识别到内容")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            _state.value = RecognitionState.PartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                startListening(intent)
            }
        } catch (e: Exception) {
            _state.value = RecognitionState.NeedExternalRecognition
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        _state.value = RecognitionState.Idle
    }

    fun resetState() {
        _state.value = RecognitionState.Idle
    }
}

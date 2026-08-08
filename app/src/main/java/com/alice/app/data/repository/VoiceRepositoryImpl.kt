package com.alice.app.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.alice.app.domain.model.Message
import com.alice.app.domain.model.VoiceState
import com.alice.app.domain.repository.VoiceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID
import javax.inject.Inject

class VoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceRepository, RecognitionListener {
    
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    private val _voiceStateUpdates = MutableStateFlow(VoiceState.IDLE)
    override val voiceStateUpdates: Flow<VoiceState> = _voiceStateUpdates.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Message>()
    override val incomingMessages: Flow<Message> = _incomingMessages.asSharedFlow()

    init {
        // Initialize SpeechRecognizer on the main thread
        GlobalScope.launch(Dispatchers.Main) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(this@VoiceRepositoryImpl)
                
                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX") // Adjust language as needed
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
            }
        }
        
        // Ensure WebSocket is connected
        GlobalScope.launch { connectWebsocket() }
    }

    override fun startAudioStream() {
        if (_voiceStateUpdates.value != VoiceState.IDLE) return
        _voiceStateUpdates.value = VoiceState.LISTENING
        GlobalScope.launch(Dispatchers.Main) {
            speechRecognizer?.startListening(recognizerIntent)
        }
    }

    override fun stopAudioStream() {
        if (_voiceStateUpdates.value == VoiceState.LISTENING) {
            _voiceStateUpdates.value = VoiceState.THINKING
            GlobalScope.launch(Dispatchers.Main) {
                speechRecognizer?.stopListening()
            }
        }
    }

    override fun cancelCurrentRequest() {
        _voiceStateUpdates.value = VoiceState.IDLE
        GlobalScope.launch(Dispatchers.Main) {
            speechRecognizer?.cancel()
        }
        GlobalScope.launch {
            session?.close()
        }
    }

    private suspend fun processVoiceInput(text: String) {
        _voiceStateUpdates.value = VoiceState.THINKING
        _incomingMessages.emit(Message(UUID.randomUUID().toString(), text, true))
        
        try {
            if (session == null || !session!!.isActive) {
                connectWebsocket()
            }
            session?.send(Frame.Text(text))
        } catch (e: Exception) {
            e.printStackTrace()
            _voiceStateUpdates.value = VoiceState.IDLE
            _incomingMessages.emit(Message(UUID.randomUUID().toString(), "Error de conexión con Alice Control Center.", false))
        }
    }
    
    private suspend fun connectWebsocket() {
        try {
            session = client.webSocketSession(
                method = HttpMethod.Get,
                host = "alicev2.ronaldtellez.dev",
                port = 443,
                path = "/terminal"
            ) {
                url { protocol = URLProtocol.WSS }
            }
            
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    for (frame in session!!.incoming) {
                        if (frame is Frame.Text) {
                            val response = frame.readText()
                            _voiceStateUpdates.value = VoiceState.SPEAKING
                            _incomingMessages.emit(Message(UUID.randomUUID().toString(), response, false))
                            
                            delay((response.length * 60).toLong())
                            _voiceStateUpdates.value = VoiceState.IDLE
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _voiceStateUpdates.value = VoiceState.IDLE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        _voiceStateUpdates.value = VoiceState.THINKING
    }
    override fun onError(error: Int) {
        _voiceStateUpdates.value = VoiceState.IDLE
    }
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0]
            GlobalScope.launch(Dispatchers.IO) {
                processVoiceInput(spokenText)
            }
        } else {
            _voiceStateUpdates.value = VoiceState.IDLE
        }
    }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}

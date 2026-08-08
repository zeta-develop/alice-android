package com.alice.app.data.repository

import com.alice.app.domain.model.Message
import com.alice.app.domain.model.VoiceState
import com.alice.app.domain.repository.VoiceRepository
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

class VoiceRepositoryImpl : VoiceRepository {
    
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000
        }
    }

    private var session: DefaultClientWebSocketSession? = null

    private val _voiceStateUpdates = MutableStateFlow(VoiceState.IDLE)
    override val voiceStateUpdates: Flow<VoiceState> = _voiceStateUpdates.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Message>()
    override val incomingMessages: Flow<Message> = _incomingMessages.asSharedFlow()

    override fun startAudioStream() {
        _voiceStateUpdates.value = VoiceState.LISTENING
        // Logic to start native Android STT goes here
        // For now, simulate STT completion after 2 seconds
        GlobalScope.launch(Dispatchers.IO) {
            delay(2000)
            stopAudioStream()
            processVoiceInput("Hola Alice, haz una prueba del sistema.")
        }
    }

    override fun stopAudioStream() {
        _voiceStateUpdates.value = VoiceState.THINKING
    }

    override fun cancelCurrentRequest() {
        _voiceStateUpdates.value = VoiceState.IDLE
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
                            // When receiving response, switch to SPEAKING to trigger Piper TTS
                            _voiceStateUpdates.value = VoiceState.SPEAKING
                            _incomingMessages.emit(Message(UUID.randomUUID().toString(), response, false))
                            
                            // Simulate TTS duration
                            delay((response.length * 50).toLong())
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
}

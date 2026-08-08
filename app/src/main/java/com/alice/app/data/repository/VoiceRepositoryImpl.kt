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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
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

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: Flow<List<Message>> = _messages.asStateFlow()

    override suspend fun startListening() {
        _voiceStateUpdates.value = VoiceState.LISTENING
        // Logic to start native Android STT goes here
        // For now, simulate STT completion after 2 seconds
        GlobalScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(2000)
            stopListening()
            processVoiceInput("Hola Alice, haz una prueba del sistema.")
        }
    }

    override suspend fun stopListening() {
        _voiceStateUpdates.value = VoiceState.THINKING
    }

    override suspend fun processVoiceInput(text: String) {
        _voiceStateUpdates.value = VoiceState.THINKING
        addMessage(Message(UUID.randomUUID().toString(), text, true))
        
        try {
            if (session == null || !session!!.isActive) {
                connectWebsocket()
            }
            session?.send(Frame.Text(text))
        } catch (e: Exception) {
            e.printStackTrace()
            _voiceStateUpdates.value = VoiceState.IDLE
            addMessage(Message(UUID.randomUUID().toString(), "Error de conexión con Alice Control Center.", false))
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
                            addMessage(Message(UUID.randomUUID().toString(), response, false))
                            
                            // Simulate TTS duration
                            kotlinx.coroutines.delay((response.length * 50).toLong())
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

    private fun addMessage(message: Message) {
        val current = _messages.value.toMutableList()
        current.add(message)
        _messages.value = current
    }
}

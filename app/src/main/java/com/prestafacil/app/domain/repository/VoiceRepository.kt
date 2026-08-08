package com.prestafacil.app.domain.repository

import com.prestafacil.app.domain.model.Message
import com.prestafacil.app.domain.model.VoiceState
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    val incomingMessages: Flow<Message>
    val voiceStateUpdates: Flow<VoiceState>
    
    fun startAudioStream()
    fun stopAudioStream()
    fun cancelCurrentRequest()
}

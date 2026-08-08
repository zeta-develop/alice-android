package com.alice.app.domain.repository

import com.alice.app.domain.model.Message
import com.alice.app.domain.model.VoiceState
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    val incomingMessages: Flow<Message>
    val voiceStateUpdates: Flow<VoiceState>
    
    fun startAudioStream()
    fun stopAudioStream()
    fun cancelCurrentRequest()
}

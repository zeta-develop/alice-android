package com.alice.app.domain.model

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    EXECUTING,
    SPEAKING
}

data class Message(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val isStreaming: Boolean = false
)

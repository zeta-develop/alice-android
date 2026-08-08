package com.alice.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alice.app.domain.model.Message
import com.alice.app.domain.model.VoiceState
import com.alice.app.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val repository: VoiceRepository
) : ViewModel() {

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(
        listOf(Message(UUID.randomUUID().toString(), "¡Hola! Soy Alice, ¿en qué te puedo ayudar hoy?", false))
    )
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            repository.incomingMessages.collect { message ->
                updateMessage(message)
            }
        }
        viewModelScope.launch {
            repository.voiceStateUpdates.collect { state ->
                _voiceState.value = state
                // Auto-listen again when Alice finishes speaking for continuous conversation
                if (state == VoiceState.IDLE && _messages.value.isNotEmpty() && !_messages.value.last().isFromUser) {
                    // Logic to auto-resume listening can go here
                }
            }
        }
    }

    fun onMicButtonPressed() {
        when (_voiceState.value) {
            VoiceState.IDLE, VoiceState.SPEAKING, VoiceState.EXECUTING -> startListening()
            VoiceState.LISTENING -> stopListening()
            VoiceState.THINKING -> cancelAndStartListening()
        }
    }

    private fun startListening() {
        _voiceState.value = VoiceState.LISTENING
        repository.startAudioStream()
    }

    private fun stopListening() {
        _voiceState.value = VoiceState.THINKING
        repository.stopAudioStream()
    }

    private fun cancelAndStartListening() {
        repository.cancelCurrentRequest()
        startListening()
    }

    private fun updateMessage(newMessage: Message) {
        val currentList = _messages.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == newMessage.id }
        if (index != -1) {
            currentList[index] = newMessage
        } else {
            currentList.add(newMessage)
        }
        _messages.value = currentList
    }
}

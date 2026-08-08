package com.alice.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alice.app.domain.model.Message
import com.alice.app.domain.model.VoiceState
import com.alice.app.viewmodel.VoiceViewModel
import com.alice.app.ui.theme.*

@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val voiceState by viewModel.voiceState.collectAsState()
    val messages by viewModel.messages.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            VoiceControlBar(
                state = voiceState,
                onMicClick = { viewModel.onMicButtonPressed() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StateIndicatorHeader(state = voiceState)
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(message = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun StateIndicatorHeader(state: VoiceState) {
    val text = when(state) {
        VoiceState.IDLE -> "Alice"
        VoiceState.LISTENING -> "Escuchando..."
        VoiceState.THINKING -> "Pensando..."
        VoiceState.EXECUTING -> "Ejecutando..."
        VoiceState.SPEAKING -> "Respondiendo..."
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MessageBubble(message: Message) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isFromUser) {
        PrimaryChat
    } else {
        SecondaryChat
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            Text(
                text = message.text + if (message.isStreaming) "..." else "",
                color = TextPrimary
            )
        }
    }
}

@Composable
fun VoiceControlBar(state: VoiceState, onMicClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == VoiceState.LISTENING) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingActionButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(80.dp)
                .scale(scale),
            shape = CircleShape,
            containerColor = if (state == VoiceState.LISTENING) Color.Red else PrimaryChat
        ) {
            Text(text = if (state == VoiceState.LISTENING) "■" else "🎤", color = Color.White, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

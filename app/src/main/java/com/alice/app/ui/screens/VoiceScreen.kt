package com.alice.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alice.app.domain.model.Message
import com.alice.app.domain.model.VoiceState
import com.alice.app.viewmodel.VoiceViewModel
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val voiceState by viewModel.voiceState.collectAsState()
    val messages by viewModel.messages.collectAsState()

    // Premium dark gradient background
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Very dark slate
            Color(0xFF1E1B4B)  // Deep indigo
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Elegant Header
            HeaderPremium(state = voiceState)
            
            // Central visualization (Siri/ChatGPT style waves)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                SiriWaveAnimation(state = voiceState)
            }

            // Messages
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(bottom = 120.dp) // space for fab
            ) {
                items(messages.reversed()) { message ->
                    PremiumMessageBubble(message = message)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        // Continuous Listening Switch & Mic
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            PremiumVoiceControlBar(
                state = voiceState,
                onMicClick = { viewModel.onMicButtonPressed() }
            )
        }
    }
}

@Composable
fun HeaderPremium(state: VoiceState) {
    val text = when(state) {
        VoiceState.IDLE -> "Alice Assistant"
        VoiceState.LISTENING -> "Escuchando..."
        VoiceState.THINKING -> "Pensando..."
        VoiceState.EXECUTING -> "Ejecutando..."
        VoiceState.SPEAKING -> "Hablando..."
    }
    
    val animatedColor by animateColorAsState(
        targetValue = if (state == VoiceState.LISTENING) Color(0xFF10B981) else Color.White,
        animationSpec = tween(500)
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = animatedColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SiriWaveAnimation(state: VoiceState) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val amplitudeTarget = when (state) {
        VoiceState.LISTENING -> 40f
        VoiceState.SPEAKING -> 60f
        VoiceState.THINKING -> 15f
        else -> 5f
    }
    
    val amplitude by animateFloatAsState(
        targetValue = amplitudeTarget,
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val midY = height / 2

        val path = Path()
        
        // Draw 3 overlapping waves with gradients
        for (wave in 0..2) {
            path.reset()
            path.moveTo(0f, midY)
            
            val wavePhase = phase + (wave * Math.PI.toFloat() / 3)
            val currentAmplitude = amplitude * (1f - (wave * 0.2f))
            
            for (x in 0..width.toInt() step 5) {
                val normalizedX = x / width
                val y = midY + sin((normalizedX * 2 * Math.PI) + wavePhase).toFloat() * currentAmplitude * sin(normalizedX * Math.PI).toFloat()
                path.lineTo(x.toFloat(), y)
            }
            
            val color = when(wave) {
                0 -> Color(0xFF6366F1).copy(alpha = 0.8f) // Indigo
                1 -> Color(0xFFEC4899).copy(alpha = 0.6f) // Pink
                else -> Color(0xFF8B5CF6).copy(alpha = 0.4f) // Purple
            }
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 4.dp.toPx() * (1f - wave * 0.2f))
            )
        }
    }
}

@Composable
fun PremiumMessageBubble(message: Message) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val gradientColors = if (message.isFromUser) {
        listOf(Color(0xFF6366F1), Color(0xFF4F46E5)) // Indigo gradient
    } else {
        listOf(Color(0xFF334155), Color(0xFF1E293B)) // Slate gradient
    }
    
    val shape = if (message.isFromUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(8.dp, shape, spotColor = gradientColors.first())
                .clip(shape)
                .background(Brush.horizontalGradient(gradientColors))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = message.text + if (message.isStreaming) " ⬤" else "",
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun PremiumVoiceControlBar(state: VoiceState, onMicClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 4.dp.value,
        targetValue = if (state == VoiceState.LISTENING) 24.dp.value else 8.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Button(
        onClick = onMicClick,
        shape = CircleShape,
        modifier = Modifier
            .size(72.dp)
            .shadow(
                elevation = glowRadius.dp,
                shape = CircleShape,
                spotColor = if (state == VoiceState.LISTENING) Color(0xFFEC4899) else Color(0xFF6366F1)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = if (state == VoiceState.LISTENING) {
                            listOf(Color(0xFFF43F5E), Color(0xFFBE123C))
                        } else {
                            listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (state == VoiceState.LISTENING) "■" else "🎙",
                fontSize = 28.sp,
                color = Color.White
            )
        }
    }
}

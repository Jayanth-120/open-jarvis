package com.openjarvis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.openjarvis.agent.AgentState
import com.openjarvis.ui.theme.VoidColor
import com.openjarvis.voice.VoiceManager

@Composable
fun MahiruAssistantScreen(
    agentState: AgentState,
    voiceState: VoiceManager.VoiceState,
    onSend: (String) -> Unit,
    onMicClick: () -> Unit,
    onStopVoice: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var command by remember { mutableStateOf("") }
    val listening = voiceState is VoiceManager.VoiceState.Recording ||
        voiceState is VoiceManager.VoiceState.Transcribing
    val avatarAction = rememberUpdatedState(if (listening) onStopVoice else onMicClick)
    val status = when (agentState) {
        is AgentState.Idle -> when (voiceState) {
            is VoiceManager.VoiceState.Error -> voiceState.message
            is VoiceManager.VoiceState.Result -> "I heard: ${voiceState.text}"
            VoiceManager.VoiceState.Recording -> "I'm listening. Tap the mic when you're done."
            VoiceManager.VoiceState.Transcribing -> "Understanding your voice…"
            else -> "I'm here. What can I help you with?"
        }
        is AgentState.Running -> agentState.step
        is AgentState.Done -> "Done — anything else?"
        is AgentState.Error -> agentState.message
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05050B))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0x553E2AA5), Color.Transparent)),
                radius = size.width * 0.72f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.52f, size.height * 0.40f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0x2224B8D8), Color.Transparent)),
                radius = size.width * 0.55f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.62f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("MAHIRU SHIINA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                    Text("PERSONAL AI COMPANION", color = VoidColor.Cyan, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = VoidColor.TextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context -> MahiruAvatarView(context, onAvatarTapped = { avatarAction.value() }) },
                    onRelease = { it.destroy() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize()
                        .clickable { avatarAction.value() }
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("MAHIRU", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, letterSpacing = 5.sp)
                    Text("Tap to talk · or type below", color = VoidColor.TextSecondary, fontSize = 11.sp)
                }
            }

            Text(status, color = if (agentState is AgentState.Error || voiceState is VoiceManager.VoiceState.Error) VoidColor.Red else VoidColor.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xEE11111B),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoidColor.BorderGlow)
            ) {
                Row(
                    modifier = Modifier.padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = command,
                        onValueChange = { command = it },
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = Brush.verticalGradient(listOf(VoidColor.Cyan, VoidColor.Violet)),
                        decorationBox = { inner ->
                            if (command.isEmpty()) Text("Ask Mahiru to do something…", color = VoidColor.TextDisabled, fontSize = 14.sp)
                            inner()
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (command.isNotBlank()) {
                                onSend(command.trim())
                                command = ""
                            }
                        })
                    )
                    IconButton(onClick = { if (listening) onStopVoice() else onMicClick() }) {
                        Surface(shape = CircleShape, color = if (listening) VoidColor.Red else VoidColor.Violet) {
                            Icon(Icons.Default.Mic, contentDescription = if (listening) "Stop listening" else "Talk to Mahiru", tint = Color.White, modifier = Modifier.padding(10.dp).size(20.dp))
                        }
                    }
                    IconButton(
                        onClick = {
                            if (command.isNotBlank()) {
                                onSend(command.trim())
                                command = ""
                            }
                        },
                        enabled = command.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = if (command.isNotBlank()) VoidColor.Cyan else VoidColor.TextDisabled)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

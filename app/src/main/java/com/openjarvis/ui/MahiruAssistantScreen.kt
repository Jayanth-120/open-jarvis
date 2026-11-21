package com.openjarvis.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
    onOpenSettings: () -> Unit,
    alerts: List<com.openjarvis.intelligence.JarvisNotificationListener.JarvisNotification>,
    onNotificationAccess: () -> Unit,
    wakeWordEnabled: Boolean,
    onWakeWordToggle: (Boolean) -> Unit
) {
    var command by remember { mutableStateOf("") }
    val listening = voiceState is VoiceManager.VoiceState.Recording ||
        voiceState is VoiceManager.VoiceState.Transcribing
    val avatarAction = rememberUpdatedState(if (listening) onStopVoice else onMicClick)
    val orbit = rememberInfiniteTransition(label = "hologram-orbit")
    val orbitRotation by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit-rotation"
    )
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
                IconButton(onClick = onNotificationAccess) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Notification access", tint = VoidColor.Cyan)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (wakeWordEnabled) "WAKE WORD ON" else "TAP TO TALK", color = if (wakeWordEnabled) VoidColor.Cyan else VoidColor.TextSecondary, fontSize = 10.sp, letterSpacing = 1.2.sp)
                    Text(if (wakeWordEnabled) "Say “Mahiru” anytime" else "Background mic is off", color = VoidColor.TextDisabled, fontSize = 9.sp)
                }
                Spacer(Modifier.size(8.dp))
                Switch(checked = wakeWordEnabled, onCheckedChange = onWakeWordToggle)
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
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.minDimension * 0.43f
                    drawCircle(Color(0x335FEAFF), radius, androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(1.dp.toPx()))
                    drawArc(Color(0xCC7AEAFF), orbitRotation, 78f, false,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - radius * 1.12f, cy - radius * 1.12f),
                        size = androidx.compose.ui.geometry.Size(radius * 2.24f, radius * 2.24f),
                        style = Stroke(2.dp.toPx()))
                    drawArc(Color(0xAA9A78FF), -orbitRotation * 0.72f, 112f, false,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - radius * 1.27f, cy - radius * 0.80f),
                        size = androidx.compose.ui.geometry.Size(radius * 2.54f, radius * 1.60f),
                        style = Stroke(1.5.dp.toPx()))
                    drawCircle(Color(0xFF7AEAFF), 3.dp.toPx(), androidx.compose.ui.geometry.Offset(cx + radius * kotlin.math.cos(Math.toRadians(orbitRotation.toDouble())).toFloat(), cy + radius * kotlin.math.sin(Math.toRadians(orbitRotation.toDouble())).toFloat()))
                }
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
            if (alerts.isEmpty()) {
                Text("ALERT LINK  ·  enable notification access", color = VoidColor.Cyan.copy(alpha = 0.72f), fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.clickable(onClick = onNotificationAccess).padding(top = 7.dp))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 2.dp)) {
                    items(alerts.take(5)) { alert ->
                        Column(Modifier.border(1.dp, VoidColor.BorderGlow, RoundedCornerShape(12.dp)).background(Color(0xAA101522), RoundedCornerShape(12.dp)).padding(horizontal = 11.dp, vertical = 8.dp)) {
                            Text("${alert.appLabel.uppercase()}  ·  ${alert.title.ifBlank { "ALERT" }}", color = VoidColor.Cyan, fontSize = 10.sp, maxLines = 1)
                            Text(alert.body.ifBlank { "New activity" }, color = Color.White.copy(alpha = 0.84f), fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
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

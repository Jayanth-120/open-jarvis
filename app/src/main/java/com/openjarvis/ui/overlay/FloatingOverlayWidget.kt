package com.openjarvis.ui.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.openjarvis.agent.AgentState
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.ui.MahiruAvatarView
import com.openjarvis.ui.theme.VoidColor
import com.openjarvis.voice.VoiceManager
import com.openjarvis.voice.VoiceManager.VoiceState
import kotlinx.coroutines.delay

@Composable
fun FloatingOverlayWidget(
    agentState: AgentState,
    voiceManager: VoiceManager?,
    recentTasks: List<TaskNode>,
    suggestions: List<String> = emptyList(),
    onCommand: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var commandText by remember { mutableStateOf("") }
    var hasAnimatedOnce by remember { mutableStateOf(false) }
    var placeholderText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    val voiceState by voiceManager?.state?.collectAsState() ?: remember { mutableStateOf(VoiceState.Idle) }

    LaunchedEffect(isExpanded) {
        if (isExpanded && !hasAnimatedOnce) {
            hasAnimatedOnce = true
            val full = "command..."
            for (char in full) {
                placeholderText += char
                delay(30)
            }
        }
    }

    LaunchedEffect(voiceState) {
        isRecording = voiceState is VoiceState.Recording
        if (voiceState is VoiceState.Result) {
            commandText = (voiceState as VoiceState.Result).text
        }
    }

    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            if (targetState) {
                (expandHorizontally(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = 0.8f
                    )
                ) + expandVertically(
                    animationSpec = spring(
                        stiffness = 260f,
                        dampingRatio = 0.8f
                    )
                ) + fadeIn(animationSpec = tween(150, delayMillis = 240))) togetherWith
                    (shrinkHorizontally(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = 0.8f
                        )
                    ) + shrinkVertically(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = 0.8f
                        )
                    ) + fadeOut(animationSpec = tween(100)))
            } else {
                (expandHorizontally(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = 0.8f
                    )
                ) + expandVertically(
                    animationSpec = spring(
                        stiffness = 260f,
                        dampingRatio = 0.8f
                    )
                ) + fadeIn(animationSpec = tween(150))) togetherWith
                    (shrinkHorizontally(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = 0.8f
                        )
                    ) + shrinkVertically(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = 0.8f
                        )
                    ) + fadeOut(animationSpec = tween(100)))
            }
        },
        label = "overlay_expand"
    ) { expanded ->
        if (expanded) {
            ExpandedOverlay(
                agentState = agentState,
                voiceState = voiceState,
                voiceManager = voiceManager,
                commandText = commandText,
                onCommandTextChange = { commandText = it },
                placeholderText = placeholderText,
                recentTasks = recentTasks,
                suggestions = suggestions,
                onSend = {
                    if (commandText.isNotBlank()) {
                        onCommand(commandText)
                        commandText = ""
                    }
                },
                onSuggestionClick = { suggestion ->
                    commandText = suggestion
                },
                onVoicePressStart = {
                    voiceManager?.startListening()
                },
                onVoicePressEnd = {
                    val text = voiceManager?.stopListening()
                    if (!text.isNullOrBlank()) {
                        commandText = text
                    }
                },
                onCollapse = {
                    voiceManager?.stopSpeaking()
                    isExpanded = false
                    onCollapse()
                },
                modifier = modifier
            )
        } else {
            MahiruCollapsedAvatar(
                agentState = agentState,
                onClick = { isExpanded = true },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun StatusOrb(state: AgentState) {
    val color = when (state) {
        is AgentState.Idle -> VoidColor.Violet
        is AgentState.Running -> VoidColor.Cyan
        is AgentState.Done -> VoidColor.Green
        is AgentState.Error -> VoidColor.Red
    }
    Canvas(Modifier.size(10.dp)) { drawCircle(color) }
}

@Composable
private fun StatusLine(state: AgentState, stepText: String) {
    val label = when (state) {
        is AgentState.Idle -> "Ready for a command"
        is AgentState.Running -> stepText.ifBlank { "Working..." }
        is AgentState.Done -> stepText.ifBlank { "Task complete" }
        is AgentState.Error -> stepText.ifBlank { "Task failed" }
    }
    Text(label, color = VoidColor.TextSecondary, fontSize = 12.sp)
}

@Composable
private fun DividerLine() {
    Divider(color = VoidColor.BorderSubtle, thickness = 1.dp)
}

@Composable
private fun SuggestionChips(suggestions: List<String>, onSuggestionClick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.take(3).forEach { suggestion ->
            Surface(
                modifier = Modifier.clickable { onSuggestionClick(suggestion) },
                color = VoidColor.Void800,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(suggestion, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = VoidColor.TextSecondary, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun InputRowWithVoice(
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    placeholderText: String,
    voiceState: VoiceState,
    onVoicePressStart: () -> Unit,
    onVoicePressEnd: () -> Unit,
    onSend: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = commandText,
            onValueChange = onCommandTextChange,
            modifier = Modifier.weight(1f).padding(12.dp),
            textStyle = TextStyle(color = VoidColor.TextPrimary, fontSize = 14.sp),
            decorationBox = { inner ->
                if (commandText.isEmpty()) Text(placeholderText, color = VoidColor.TextDisabled)
                inner()
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )
        IconButton(onClick = {
            if (voiceState is VoiceState.Recording) onVoicePressEnd() else onVoicePressStart()
        }) {
            Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = VoidColor.Violet)
        }
        IconButton(onClick = onSend, enabled = commandText.isNotBlank()) {
            Icon(Icons.Default.Send, contentDescription = "Send command", tint = VoidColor.Violet)
        }
    }
}

@Composable
private fun TaskLogWidget(tasks: List<TaskNode>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tasks.forEach { task ->
            Text(task.command, color = VoidColor.TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MahiruCollapsedAvatar(
    agentState: AgentState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnClick by rememberUpdatedState(onClick)

    val statusScale by rememberInfiniteTransition(label = "status").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val statusColor = when (agentState) {
        is AgentState.Idle -> VoidColor.Violet
        is AgentState.Running -> VoidColor.Cyan
        is AgentState.Done -> VoidColor.Green
        is AgentState.Error -> VoidColor.Red
    }

    Box(
        modifier = modifier
            .width(150.dp)
            .height(210.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        AndroidView(
            factory = { ctx ->
                MahiruAvatarView(
                    context = ctx,
                    onAvatarTapped = { currentOnClick() }
                )
            },
            onRelease = { avatarView ->
                avatarView.destroy()
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .padding(8.dp)
                .size(14.dp)
                .scale(statusScale)
                .clickable { currentOnClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = statusColor)
            }
        }
    }
}

@Composable
private fun ExpandedOverlay(
    agentState: AgentState,
    voiceState: VoiceState,
    voiceManager: VoiceManager?,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    placeholderText: String,
    recentTasks: List<TaskNode>,
    suggestions: List<String> = emptyList(),
    onSend: () -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    onVoicePressStart: () -> Unit,
    onVoicePressEnd: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stepText = when (agentState) {
        is AgentState.Running -> agentState.step
        is AgentState.Done -> agentState.result
        is AgentState.Error -> agentState.message
        else -> ""
    }

    LaunchedEffect(agentState) {
        if (agentState is AgentState.Done) {
            delay(2000)
            onCollapse()
        } else if (agentState is AgentState.Error) {
            delay(4000)
            onCollapse()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = VoidColor.Void900,
        tonalElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(VoidColor.Violet)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(VoidColor.Violet.copy(alpha = 0.2f))
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusOrb(state = agentState)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MAHIRU",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight(600),
                                fontSize = 12.sp,
                                color = VoidColor.TextPrimary
                            )
                        )
                    }

                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = VoidColor.TextDisabled
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                StatusLine(state = agentState, stepText = stepText)

                Spacer(modifier = Modifier.height(12.dp))

                DividerLine()

                Spacer(modifier = Modifier.height(12.dp))

                if (suggestions.isNotEmpty() && agentState is AgentState.Idle) {
                    SuggestionChips(
                        suggestions = suggestions,
                        onSuggestionClick = onSuggestionClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                InputRowWithVoice(
                    commandText = commandText,
                    onCommandTextChange = onCommandTextChange,
                    placeholderText = if (commandText.isEmpty()) placeholderText else "",
                    voiceState = voiceState,
                    onVoicePressStart = onVoicePressStart,
                    onVoicePressEnd = onVoicePressEnd,
                    onSend = onSend
                )

                Spacer(modifier = Modifier.height(12.dp))

                DividerLine()

                Spacer(modifier = Modifier.height(12.dp))

                TaskLogWidget(tasks = recentTasks.take(3))
            }
        }
    }
}

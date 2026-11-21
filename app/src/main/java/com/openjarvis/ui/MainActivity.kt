package com.openjarvis.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.agent.AgentCore
import com.openjarvis.agent.AgentState
import com.openjarvis.ui.theme.VoidColor
import com.openjarvis.ui.theme.OpenJarvisTheme
import com.openjarvis.voice.VoiceManager

class MainActivity : ComponentActivity() {

    private lateinit var agentCore: AgentCore
    private lateinit var voiceManager: VoiceManager
    private var accessibilityAllowed by mutableStateOf(false)

    private val requestMicrophone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceInput()
        else Toast.makeText(this, "Microphone permission is needed for voice chat.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        // Shut down the old floating-window mode so it cannot cover the full-screen assistant.
        stopService(Intent(this, OverlayService::class.java))

        agentCore = AgentCore(this)
        voiceManager = VoiceManager(this)
        accessibilityAllowed = isAccessibilityServiceEnabled()

        setContent {
            OpenJarvisTheme {
                val agentState by agentCore.state.collectAsState()
                val voiceState by voiceManager.state.collectAsState()

                LaunchedEffect(voiceState) {
                    val result = voiceState as? VoiceManager.VoiceState.Result
                    if (result != null) {
                        agentCore.executeTask(result.text)
                        voiceManager.resetState()
                    }
                }
                LaunchedEffect(agentState) {
                    if (agentState is AgentState.Done) voiceManager.speak("I'm done. What else can I help with?")
                }

                if (!accessibilityAllowed) {
                    PermissionScreen(
                        accessibilityEnabled = false,
                        onEnableAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    )
                } else {
                    MahiruAssistantScreen(
                        agentState = agentState,
                        voiceState = voiceState,
                        onSend = { command -> agentCore.executeTask(command) },
                        onMicClick = ::requestVoiceInput,
                        onStopVoice = { voiceManager.stopListening() },
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accessibilityAllowed = isAccessibilityServiceEnabled()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onDestroy() {
        if (::voiceManager.isInitialized) voiceManager.release()
        super.onDestroy()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun requestVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceInput()
        } else {
            requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        voiceManager.setVoiceEnabled(true)
        voiceManager.startListening()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = ComponentName(this, JarvisAccessibilityService::class.java)
        return enabledServices.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}

@androidx.compose.runtime.Composable
private fun PermissionScreen(
    accessibilityEnabled: Boolean,
    onEnableAccessibility: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("One permission to begin", style = MaterialTheme.typography.headlineSmall, color = VoidColor.TextPrimary)
        Text(
            "Mahiru needs Accessibility access to interact with apps on your device.",
            modifier = Modifier.padding(vertical = 16.dp),
            color = VoidColor.TextSecondary,
            textAlign = TextAlign.Center
        )
        if (!accessibilityEnabled) {
            Button(onClick = onEnableAccessibility, modifier = Modifier.fillMaxWidth()) {
                Text("Enable Accessibility")
            }
        }
    }
}

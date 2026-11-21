package com.openjarvis.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.ui.dashboard.DashboardScreen
import com.openjarvis.ui.theme.OpenJarvisTheme
import com.openjarvis.ui.theme.VoidColor

class MainActivity : ComponentActivity() {

    private lateinit var graphifyRepo: GraphifyRepository
    private var accessibilityAllowed by mutableStateOf(false)
    private var overlayAllowed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        graphifyRepo = GraphifyRepository(this)

        setContent {
            OpenJarvisTheme {
                var recentTasks by remember { mutableStateOf<List<TaskNode>>(emptyList()) }

                LaunchedEffect(Unit) {
                    try {
                        recentTasks = graphifyRepo.getRecentTasks(10)
                    } catch (_: Exception) {
                    }
                }

                val accessibilityEnabled = accessibilityAllowed
                val overlayEnabled = overlayAllowed

                if (!accessibilityEnabled || !overlayEnabled) {
                    PermissionScreen(
                        accessibilityEnabled = accessibilityEnabled,
                        overlayEnabled = overlayEnabled,
                        onEnableAccessibility = { startAccessibilitySettings() },
                        onEnableOverlay = { startOverlaySettings() }
                    )
                } else {
                    DashboardScreen(
                        onStartOverlay = { startOverlayService() },
                        onOpenSettings = { openSettings() },
                        graphifyRepo = graphifyRepo,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accessibilityAllowed = isAccessibilityServiceEnabled()
        overlayAllowed = Settings.canDrawOverlays(this)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expected = ComponentName(this, JarvisAccessibilityService::class.java)
        return enabledServices.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    private fun startAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun startOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun startOverlayService() {
        OverlayService.start(this)
        Toast.makeText(this, "Mahiru is active", Toast.LENGTH_SHORT).show()
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}

@Composable
fun PermissionScreen(
    accessibilityEnabled: Boolean,
    overlayEnabled: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            color = VoidColor.TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Mahiru needs accessibility and overlay permissions to control your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = VoidColor.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!accessibilityEnabled) {
            Button(
                onClick = onEnableAccessibility,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Accessibility Service")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!overlayEnabled) {
            Button(
                onClick = onEnableOverlay,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Overlay Permission")
            }
        }

        if (accessibilityEnabled && overlayEnabled) {
            Text(
                text = "Mahiru is ready",
                style = MaterialTheme.typography.titleMedium,
                color = VoidColor.Violet
            )
        }
    }
}

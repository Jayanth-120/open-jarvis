package com.openjarvis.ui

import android.os.Bundle
import android.content.ComponentName
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.R
import com.openjarvis.ui.settings.SettingsScreen
import com.openjarvis.ui.theme.OpenJarvisTheme

class SettingsActivity : ComponentActivity() {
    
    private lateinit var masterKey: MasterKey
    private lateinit var prefs: android.content.SharedPreferences
    private var accessibilityAllowed by mutableStateOf(false)
    private var overlayAllowed by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        prefs = EncryptedSharedPreferences.create(
            this,
            "jarvis_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        setContent {
            OpenJarvisTheme {
                SettingsScreen(
                    onNavigateBack = { finish() },
                    initialProvider = prefs.getString("provider_name", "Groq") ?: "Groq",
                    initialBaseUrl = prefs.getString("provider_base_url", "") ?: "",
                    initialApiKey = prefs.getString("provider_api_key", "") ?: "",
                    initialModel = prefs.getString("provider_model", "llama-3.1-70b-versatile") ?: "llama-3.1-70b-versatile",
                    accessibilityEnabled = accessibilityAllowed,
                    overlayEnabled = overlayAllowed,
                    onSaveProvider = { name, baseUrl, apiKey, model ->
                        saveProviderSettings(name, baseUrl, apiKey, model)
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
    
    private fun saveProviderSettings(name: String, baseUrl: String, apiKey: String, model: String) {
        prefs.edit().apply {
            putString("provider_name", name)
            putString("provider_base_url", baseUrl)
            putString("provider_api_key", apiKey)
            putString("provider_model", model)
            apply()
        }
    }
}

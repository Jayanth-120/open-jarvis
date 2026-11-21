package com.openjarvis.intelligence

import android.content.Context
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import kotlinx.coroutines.delay

class AIAppInteractor(private val context: Context) {

    private val screenReader = ScreenReader(context)

    suspend fun openAIApp(appInfo: AIApps.AIAppInfo): Boolean {
        val service = JarvisAccessibilityService.instance ?: return false
        service.openAppByPackage(appInfo.packageName)
        delay(1500)
        return true
    }

    suspend fun typePrompt(prompt: String): Boolean {
        val service = JarvisAccessibilityService.instance ?: return false
        val typed = service.typeText(prompt)
        delay(500)
        return typed
    }

    suspend fun waitForResponse(timeoutMs: Long = 5000L): String {
        delay(timeoutMs.coerceAtMost(8000L))
        return try {
            screenReader.extractAllText()
        } catch (_: Exception) {
            ""
        }
    }
}

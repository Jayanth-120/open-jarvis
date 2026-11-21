package com.openjarvis.intelligence

import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import kotlinx.coroutines.delay

class AIAppInteractor(private val service: JarvisAccessibilityService) {

    private val screenReader = ScreenReader(service)

    suspend fun openAIApp(appInfo: AIAppMeta): Boolean {
        val activeService = JarvisAccessibilityService.instance ?: return false
        activeService.openAppByPackage(appInfo.packageName)
        delay(1500)
        return true
    }

    suspend fun typePrompt(prompt: String): Boolean {
        val activeService = JarvisAccessibilityService.instance ?: return false
        val typed = activeService.typeText(prompt)
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

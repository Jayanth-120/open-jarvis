package com.openjarvis.agent

import android.content.Context
import com.openjarvis.accessibility.ActionExecutor
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.llm.UniversalAdapter
import kotlinx.coroutines.delay

class SelfHealingExecutor(private val context: Context) {

    private val actionExecutor = ActionExecutor(context)
    private val universalAdapter = UniversalAdapter(context)

    suspend fun executeActionWithHealing(action: Action): Boolean {
        val directResult: Boolean = actionExecutor.executeSingle(action)
        if (directResult) return true

        val service = JarvisAccessibilityService.instance ?: return false
        val stepLabel = action.text ?: action.value ?: action.action

        if (action.action == Action.TAP && action.text != null) {
            delay(600)
            val retryTap: Boolean = service.tapByText(action.text)
            if (retryTap) return true
        }

        val alternativeResponse = universalAdapter.complete(
            systemPrompt = "Return a single JSON action object to recover from step: $stepLabel",
            userMessage = stepLabel
        ).getOrNull()

        if (!alternativeResponse.isNullOrBlank()) {
            val parsed = ActionJsonParser.parse(alternativeResponse)?.firstOrNull()
            if (parsed != null) {
                return actionExecutor.executeSingle(parsed)
            }
        }

        return false
    }
}

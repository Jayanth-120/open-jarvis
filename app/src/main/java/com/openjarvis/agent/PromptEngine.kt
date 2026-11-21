package com.openjarvis.agent

import android.content.Context
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.llm.UniversalAdapter

class PromptEngine(private val context: Context) {

    private val graphifyRepo = GraphifyRepository(context)
    private val universalAdapter = UniversalAdapter(context)

    suspend fun buildSystemPrompt(
        command: String,
        screenText: String,
        appReasoning: String,
        installedApps: String
    ): String {
        val memoryContext = try {
            graphifyRepo.buildMemoryContext(command)
        } catch (_: Exception) {
            ""
        }

        return """
You are Mahiru Shiina — an Android device control AI agent.
Respond ONLY with a valid JSON array of actions. No markdown fences, no commentary.

SCREEN_OCR: ${screenText.take(2000)}
APP_REASONING: $appReasoning
INSTALLED_AI_APPS: $installedApps
MEMORY_CONTEXT: ${if (memoryContext.isBlank()) "None" else memoryContext}
""".trimIndent()
    }

    suspend fun completePrompt(systemPrompt: String, userPrompt: String): Result<String> {
        return universalAdapter.complete(systemPrompt, userPrompt)
    }
}

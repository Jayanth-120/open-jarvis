package com.openjarvis.agent

import android.content.Context
import com.openjarvis.llm.UniversalAdapter
import org.json.JSONException

class ProviderFallbackChain(private val context: Context) {

    private val universalAdapter = UniversalAdapter(context)

    suspend fun executeWithFallback(
        systemPrompt: String,
        userMessage: String
    ): Result<String> {
        return try {
            val primaryResult = universalAdapter.complete(systemPrompt, userMessage)
            if (primaryResult.isSuccess) {
                primaryResult
            } else {
                val errorMsg = primaryResult.exceptionOrNull()?.message ?: "Provider failed"
                logFailure(universalAdapter.getProviderName(), errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: JSONException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun logFailure(provider: String, reason: String) {
        android.util.Log.w("ProviderFallbackChain", "Provider $provider failed: $reason")
    }
}

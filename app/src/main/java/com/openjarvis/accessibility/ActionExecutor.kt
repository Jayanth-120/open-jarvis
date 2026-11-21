package com.openjarvis.accessibility

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.openjarvis.agent.Action
import kotlinx.coroutines.delay

class ActionExecutor(private val context: Context) {

    suspend fun execute(actions: List<Action>): Result<Unit> {
        return try {
            for (action in actions) {
                executeSingle(action)
                delay(400)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeSingle(action: Action): Boolean {
        val service = JarvisAccessibilityService.instance
        return when (action.action) {
            Action.OPEN_APP -> {
                val pkg = action.packageName ?: action.label?.let { findPackageByLabel(it) }
                if (pkg != null) {
                    service?.openAppByPackage(pkg)
                    delay(800)
                    true
                } else {
                    false
                }
            }
            Action.TAP -> {
                val target = action.text ?: return false
                service?.tapByText(target) == true
            }
            Action.TYPE -> {
                val value = action.value ?: return false
                service?.typeText(value) == true
            }
            Action.PRESS_BACK -> service?.pressBack() == true
            Action.PRESS_HOME -> service?.pressHome() == true
            Action.PRESS_RECENTS -> service?.pressRecents() == true
            Action.WAIT_FOR -> {
                delay((action.timeoutMs ?: 1500L).coerceAtMost(5000L))
                true
            }
            else -> true
        }
    }

    private fun findPackageByLabel(label: String): String? {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        val normalized = label.lowercase().trim()
        for (app in apps) {
            val appLabel = app.loadLabel(pm).toString().lowercase()
            if (appLabel == normalized || appLabel.contains(normalized) || normalized.contains(appLabel)) {
                return app.activityInfo.packageName
            }
        }
        return null
    }
}

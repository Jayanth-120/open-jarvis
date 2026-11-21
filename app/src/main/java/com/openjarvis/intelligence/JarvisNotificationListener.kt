package com.openjarvis.intelligence

import android.app.Notification
import android.speech.tts.TextToSpeech
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.ui.MahiruPersona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class JarvisNotificationListener : NotificationListenerService() {
    
    private val notifications = mutableListOf<JarvisNotification>()
    private var announcer: TextToSpeech? = null
    
    companion object {
        private val _alerts = MutableStateFlow<List<JarvisNotification>>(emptyList())
        val alerts = _alerts.asStateFlow()

        private val PRIVACY_PROTECTED_APPS = setOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "com.bankofamerica.cashpromobile",
            "com.chase",
            "com.wells Fargo",
            "com.usbank",
            "com.citi",
            "com.barclays",
            "com.hsbc",
            "com.db",
            "com.americanexpress",
            "com.discover",
            "com.paypal",
            "com.squareup",
            "com.stripe",
            "com.razorpay"
        )
        
        fun shouldProcessNotification(packageName: String): Boolean {
            return packageName !in PRIVACY_PROTECTED_APPS
        }

        private val messagingApps = listOf(
            "com.whatsapp",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.instagram.android",
            "com.facebook.orca",
            "org.telegram.messenger",
            "com.slack",
            "com.discord"
        )
    }

    override fun onCreate() {
        super.onCreate()
        announcer = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                announcer?.language = Locale.getDefault()
                announcer?.let(MahiruPersona::configureVoice)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val current = activeNotifications.orEmpty()
            .filter { it.packageName != packageName && !it.isOngoing && !it.isGroup }
            .filter { shouldProcessNotification(it.packageName) }
            .map(::parseNotification)
            .takeLast(20)
        synchronized(notifications) {
            notifications.clear()
            notifications.addAll(current)
            _alerts.value = current.asReversed()
        }
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.isOngoing || sbn.isGroup || !shouldProcessNotification(sbn.packageName)) {
            return
        }
        
        val parsed = parseNotification(sbn)
        synchronized(notifications) {
            notifications.removeAll { it.packageName == parsed.packageName && it.id == parsed.id }
            notifications.add(parsed)
            while (notifications.size > 20) notifications.removeAt(0)
            _alerts.value = notifications.toList().asReversed()
        }
        announce(parsed)
        
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Preserve a short recent-alert history after the system notification is dismissed.
    }

    private fun announce(alert: JarvisNotification) {
        val detail = listOf(alert.title, alert.body).filter { it.isNotBlank() }.joinToString(": ")
        val message = when {
            alert.category == Notification.CATEGORY_CALL -> "Incoming call${detail.takeIf { it.isNotBlank() }?.let { " from $it" } ?: ""}."
            alert.isMessaging -> "New message${alert.sender?.let { " from $it" } ?: ""}${detail.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}."
            else -> "Notification from ${alert.appLabel}${detail.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}."
        }
        announcer?.speak(message, TextToSpeech.QUEUE_ADD, null, "mahiru-${alert.id}-${alert.timestamp}")
    }
    
    private fun parseNotification(sbn: StatusBarNotification): JarvisNotification {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val appLabel = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            sbn.packageName
        }
        
        val isMessaging = sbn.packageName in messagingApps
        
        val sender = if (isMessaging) {
            title.takeIf { it.isNotBlank() }
        } else null
        
        return JarvisNotification(
            id = sbn.id,
            packageName = sbn.packageName,
            appLabel = appLabel,
            title = title,
            body = body,
            timestamp = sbn.postTime,
            isMessaging = isMessaging,
            sender = sender,
            category = sbn.notification.category
        )
    }
    
    suspend fun replyToNotification(sender: String, packageName: String, reply: String): Boolean {
        return try {
            val notification = synchronized(notifications) { notifications.find {
                it.packageName == packageName && it.sender?.contains(sender, ignoreCase = true) == true
            } } ?: return false
            
            val service = JarvisAccessibilityService.instance
            
            service?.tapByText("Reply")
            service?.typeText(reply)
            service?.tapByText("Send")
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getUnread(packageName: String? = null): List<JarvisNotification> {
        return synchronized(notifications) {
            if (packageName != null) notifications.filter { it.packageName == packageName }
            else notifications.toList()
        }
    }
    
    fun getSummary(): String {
        val grouped = synchronized(notifications) { notifications.groupBy { it.packageName } }
        
        return grouped.entries.sortedByDescending { it.value.size }.take(5).joinToString(", ") { (pkg, msgs) ->
            val app = msgs.first().appLabel
            val count = msgs.size
            val lastSender = msgs.lastOrNull()?.sender
            if (lastSender != null) "$count $app from $lastSender"
            else "$count $app"
        }
    }
    
    fun clearAll(packageName: String? = null) {
        if (packageName != null) {
            synchronized(notifications) {
                notifications.removeAll { it.packageName == packageName }
                _alerts.value = notifications.toList().asReversed()
            }
        } else {
            synchronized(notifications) {
                notifications.clear()
                _alerts.value = emptyList()
            }
        }
    }

    override fun onDestroy() {
        announcer?.stop()
        announcer?.shutdown()
        announcer = null
        super.onDestroy()
    }
    
    data class JarvisNotification(
        val id: Int,
        val packageName: String,
        val appLabel: String,
        val title: String,
        val body: String,
        val timestamp: Long,
        val isMessaging: Boolean,
        val sender: String?,
        val category: String?
    )
    
}

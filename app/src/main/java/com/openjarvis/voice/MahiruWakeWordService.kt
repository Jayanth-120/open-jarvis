package com.openjarvis.voice

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.openjarvis.R
import com.openjarvis.agent.AgentCore
import com.openjarvis.agent.AgentState
import com.openjarvis.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object MahiruWakeWordStatus {
    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    internal fun setActive(active: Boolean) { _active.value = active }
}

class MahiruWakeWordService : Service() {
    companion object {
        const val ACTION_START = "com.openjarvis.voice.action.START_WAKE_WORD"
        const val ACTION_STOP = "com.openjarvis.voice.action.STOP_WAKE_WORD"
        private const val CHANNEL_ID = "mahiru_wake_word"
        private const val NOTIFICATION_ID = 4317
    }

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var speech: AndroidSTTEngine? = null
    private var voice: VoiceManager? = null
    private var agent: AgentCore? = null
    private var isRecognizing = false
    private var awaitingCommand = false
    private var commandInProgress = false
    private var sawRunningState = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            getSharedPreferences("assistant_modes", MODE_PRIVATE).edit().putBoolean("wake_word_enabled", false).apply()
            stopSelf()
            return START_NOT_STICKY
        }
        if (speech != null) return START_STICKY
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            getSharedPreferences("assistant_modes", MODE_PRIVATE).edit().putBoolean("wake_word_enabled", false).apply()
            stopSelf()
            return START_NOT_STICKY
        }

        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        MahiruWakeWordStatus.setActive(true)
        speech = AndroidSTTEngine(this)
        voice = VoiceManager(this).also { it.setVoiceEnabled(true) }
        agent = AgentCore(this)
        observeAgent()
        handler.postDelayed({ listenForWakeWord() }, 800)
        return START_STICKY
    }

    private fun listenForWakeWord() {
        val engine = speech ?: return
        if (isRecognizing || commandInProgress) return
        isRecognizing = true
        engine.startListening(
            onResult = { phrase ->
                isRecognizing = false
                processRecognizedPhrase(phrase.orEmpty())
            },
            onError = {
                isRecognizing = false
                if (!commandInProgress) handler.postDelayed({ listenForWakeWord() }, 700)
            }
        )
    }

    private fun processRecognizedPhrase(phrase: String) {
        val wakeWord = Regex("\\b(?:hey\\s+)?mahiru\\b", RegexOption.IGNORE_CASE)
        val match = wakeWord.find(phrase)
        if (awaitingCommand) {
            awaitingCommand = false
            val command = phrase.trim()
            if (command.isNotBlank()) runCommand(command) else listenForWakeWord()
            return
        }
        if (match == null) {
            handler.postDelayed({ listenForWakeWord() }, 250)
            return
        }

        val command = phrase.substring(match.range.last + 1).trim().trimStart(',', '.', '!', '?', ' ')
        if (command.isBlank()) {
            awaitingCommand = true
            voice?.speak("Yes? I'm listening.")
            handler.postDelayed({ listenForWakeWord() }, 2000)
        } else {
            runCommand(command)
        }
    }

    private fun runCommand(command: String) {
        commandInProgress = true
        sawRunningState = false
        voice?.speak("Okay, I'll take care of that.")
        agent?.executeTask(command)
        handler.postDelayed({
            if (commandInProgress) {
                commandInProgress = false
                voice?.speak("I couldn't finish that just now. Say Mahiru to try again.")
                listenForWakeWord()
            }
        }, 60_000)
    }

    private fun observeAgent() {
        serviceScope.launch {
            agent?.state?.collect { state ->
                if (state is AgentState.Running) sawRunningState = true
                if (sawRunningState && (state is AgentState.Done || state is AgentState.Error) && commandInProgress) {
                    commandInProgress = false
                    sawRunningState = false
                    val spoken = if (state is AgentState.Error) state.message else "Done. What else can I help with?"
                    voice?.speak(spoken)
                    handler.postDelayed({ listenForWakeWord() }, 3500)
                }
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Mahiru wake word", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Visible status while Mahiru is listening for her wake word"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Mahiru is listening")
        .setContentText("Mic active via Android speech recognition · tap to turn off")
        .setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .addAction(R.drawable.ic_notification, "Turn off", PendingIntent.getService(this, 1, Intent(this, MahiruWakeWordService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .build()

    override fun onDestroy() {
        MahiruWakeWordStatus.setActive(false)
        handler.removeCallbacksAndMessages(null)
        speech?.release()
        voice?.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}

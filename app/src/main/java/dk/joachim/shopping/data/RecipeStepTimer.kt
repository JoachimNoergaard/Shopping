package dk.joachim.shopping.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Extracts the first "N min." / "N min" duration from a step line (case-insensitive). */
fun parseInstructionMinutes(text: String): Int? {
    val regex = Regex("""(\d+)\s*min\.?\b""", RegexOption.IGNORE_CASE)
    val match = regex.find(text) ?: return null
    val n = match.groupValues[1].toIntOrNull() ?: return null
    return when {
        n < 1 -> null
        n > 24 * 60 -> null
        else -> n
    }
}

data class RecipeStepTimerEntry(
    val id: String,
    val recipeId: String,
    val recipeName: String,
    val sectionIndex: Int,
    val stepIndex: Int,
    /** Counts down past zero into negative values (overshoot). */
    val remainingSeconds: Int,
    val isRunning: Boolean = false,
    val initialTotalSeconds: Int,
    val stepPreview: String,
)

/**
 * Multiple kitchen timers for recipe steps. Survives switching recipes and tabs;
 * initialized from [Application.onCreate].
 */
object RecipeStepTimer {

    const val ACTION_TIMER_DEADLINE = "dk.joachim.shopping.action.TIMER_DEADLINE"
    const val EXTRA_TIMER_ID = "timer_id"

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val alarmLock = Any()
    private var alarmWakeLock: PowerManager.WakeLock? = null
    private var activeAlarmPlayer: MediaPlayer? = null
    private var activeRingtone: Ringtone? = null
    private var activeToneGenerator: ToneGenerator? = null

    private val alarmAudioAttrs: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private val _timers = MutableStateFlow<List<RecipeStepTimerEntry>>(emptyList())
    val timers: StateFlow<List<RecipeStepTimerEntry>> = _timers.asStateFlow()

    private var notificationSyncStarted = false

    private val scheduledAlarmTimerIds = mutableSetOf<String>()
    private val alarmScheduleLock = Any()

    fun init(context: Context) {
        appContext = context.applicationContext
        RecipeTimerNotifications.ensureChannel(appContext)
        if (!notificationSyncStarted) {
            notificationSyncStarted = true
            scope.launch {
                _timers.collect { timers ->
                    RecipeTimerNotifications.sync(appContext, timers)
                    rescheduleDeadlineAlarms(timers)
                }
            }
        }
    }

    init {
        scope.launch {
            while (isActive) {
                delay(1000L)
                _timers.update { list ->
                    list.map { entry ->
                        if (!entry.isRunning) return@map entry
                        val nextRemaining = entry.remainingSeconds - 1
                        val crossedZero = entry.remainingSeconds > 0 && nextRemaining <= 0
                        if (crossedZero) playAlarm()
                        entry.copy(remainingSeconds = nextRemaining)
                    }
                }
            }
        }
    }

    /**
     * Adds a running timer for this step. Replaces any existing timer for the same
     * [recipeId] / [sectionIndex] / [stepIndex].
     */
    fun start(
        durationSeconds: Int,
        stepPreview: String,
        recipeId: String,
        recipeName: String,
        sectionIndex: Int,
        stepIndex: Int,
    ) {
        if (durationSeconds <= 0) return
        val preview = stepPreview.trim().let { if (it.length > 100) it.take(100) + "…" else it }
        val name = recipeName.trim().let { if (it.length > 80) it.take(80) + "…" else it }
        val id = UUID.randomUUID().toString()
        val entry = RecipeStepTimerEntry(
            id = id,
            recipeId = recipeId,
            recipeName = name,
            sectionIndex = sectionIndex,
            stepIndex = stepIndex,
            remainingSeconds = durationSeconds,
            isRunning = true,
            initialTotalSeconds = durationSeconds,
            stepPreview = preview,
        )
        _timers.update { list ->
            list.filterNot {
                it.recipeId == recipeId &&
                    it.sectionIndex == sectionIndex &&
                    it.stepIndex == stepIndex
            } + entry
        }
    }

    fun pause(id: String) {
        _timers.update { list ->
            list.map { e -> if (e.id == id) e.copy(isRunning = false) else e }
        }
    }

    fun resume(id: String) {
        _timers.update { list ->
            list.map { e ->
                if (e.id != id) return@map e
                val hasSession = e.initialTotalSeconds > 0 || e.remainingSeconds != 0
                if (hasSession) e.copy(isRunning = true) else e
            }
        }
    }

    fun clear(id: String) {
        stopAlarmPlayback()
        _timers.update { list -> list.filter { it.id != id } }
    }

    fun clearAll() {
        stopAlarmPlayback()
        _timers.value = emptyList()
    }

    fun formatClock(seconds: Int): String {
        val negative = seconds < 0
        val abs = kotlin.math.abs(seconds)
        val m = abs / 60
        val s = abs % 60
        val core = "${m}:${s.toString().padStart(2, '0')}"
        return if (negative) "-$core" else core
    }

    /**
     * Called when [AlarmManager] fires at deadline (app may be in background or device idle).
     */
    internal fun handleSystemDeadline(timerId: String) {
        if (!::appContext.isInitialized) return
        val entry = _timers.value.find { it.id == timerId }
        val play = when {
            entry == null -> true
            entry.isRunning && entry.remainingSeconds > 0 -> {
                _timers.update { list ->
                    list.map { e ->
                        if (e.id != timerId) e
                        else e.copy(remainingSeconds = 0)
                    }
                }
                true
            }
            else -> false
        }
        if (play) playAlarm()
    }

    private fun rescheduleDeadlineAlarms(timers: List<RecipeStepTimerEntry>) {
        if (!::appContext.isInitialized) return
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val needAlarmIds = timers.filter { it.isRunning && it.remainingSeconds > 0 }
            .map { it.id }
            .toSet()
        val toCancel = synchronized(alarmScheduleLock) {
            val cancel = scheduledAlarmTimerIds - needAlarmIds
            scheduledAlarmTimerIds.clear()
            scheduledAlarmTimerIds.addAll(needAlarmIds)
            cancel
        }
        toCancel.forEach { id ->
            try {
                am.cancel(pendingIntentForDeadline(id))
            } catch (_: Exception) {
                // ignore
            }
        }
        timers.filter { it.isRunning && it.remainingSeconds > 0 }.forEach { e ->
            scheduleDeadlineAlarmForEntry(am, e)
        }
    }

    private fun scheduleDeadlineAlarmForEntry(am: AlarmManager, e: RecipeStepTimerEntry) {
        val pi = pendingIntentForDeadline(e.id)
        val triggerRtc = System.currentTimeMillis() + e.remainingSeconds * 1000L
        val triggerElapsed = SystemClock.elapsedRealtime() + e.remainingSeconds * 1000L
        val show = openAppForAlarmClockPendingIntent()
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                am.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerRtc, show),
                    pi,
                )
            } else {
                am.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerElapsed,
                    pi,
                )
            }
        } catch (_: Exception) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerElapsed,
                        pi,
                    )
                } else {
                    am.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerElapsed,
                        pi,
                    )
                }
            } catch (_: Exception) {
                // Exact alarms may be denied; in-app tick still updates when foreground
            }
        }
    }

    private fun openAppForAlarmClockPendingIntent(): PendingIntent {
        val launch = RecipeTimerNotifications.mainActivityIntentForTimers(appContext, _timers.value)
        return PendingIntent.getActivity(
            appContext,
            10002,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntentForDeadline(timerId: String): PendingIntent {
        return PendingIntent.getBroadcast(
            appContext,
            requestCodeForTimerAlarm(timerId),
            Intent(appContext, RecipeTimerAlarmReceiver::class.java).apply {
                action = ACTION_TIMER_DEADLINE
                putExtra(EXTRA_TIMER_ID, timerId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCodeForTimerAlarm(timerId: String): Int =
        timerId.hashCode() and 0x7FFF_FFFF

    private fun playAlarm() {
        if (!::appContext.isInitialized) return
        acquireAlarmCpuWakeLock()
        scope.launch(Dispatchers.Main.immediate) {
            stopAlarmPlaybackInner()
            val uris = listOf(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            ).filterNotNull().distinct()
            var played = false
            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    if (tryPrepareMediaPlayerAlarm(uri)) {
                        played = true
                        return@withContext
                    }
                }
            }
            if (played) return@launch
            for (uri in uris) {
                if (tryPlayRingtoneAlarm(uri)) return@launch
            }
            tryPlayToneGeneratorAlarm()
        }
    }

    private fun acquireAlarmCpuWakeLock() {
        synchronized(alarmLock) {
            try {
                alarmWakeLock?.takeIf { it.isHeld }?.release()
            } catch (_: Exception) {
                // ignore
            }
            alarmWakeLock = null
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            alarmWakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Shopping:RecipeTimerAlarm",
            ).apply {
                setReferenceCounted(false)
                acquire(15 * 60 * 1000L)
            }
        }
    }

    private fun releaseAlarmCpuWakeLock() {
        synchronized(alarmLock) {
            try {
                alarmWakeLock?.takeIf { it.isHeld }?.release()
            } catch (_: Exception) {
                // ignore
            }
            alarmWakeLock = null
        }
    }

    /** Blocking prepare on IO thread; start on caller thread — caller must be Main for ToneGenerator path. */
    private suspend fun tryPrepareMediaPlayerAlarm(uri: Uri): Boolean {
        return try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(alarmAudioAttrs)
                setDataSource(appContext, uri)
                isLooping = true
                prepare()
            }
            withContext(Dispatchers.Main.immediate) {
                synchronized(alarmLock) {
                    mp.start()
                    activeAlarmPlayer = mp
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun tryPlayRingtoneAlarm(uri: Uri): Boolean {
        return try {
            val ringtone = RingtoneManager.getRingtone(appContext, uri) ?: return false
            ringtone.audioAttributes = alarmAudioAttrs
            synchronized(alarmLock) { activeRingtone = ringtone }
            ringtone.play()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun tryPlayToneGeneratorAlarm() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            synchronized(alarmLock) { activeToneGenerator = tg }
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000)
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun stopAlarmPlayback() {
        stopAlarmPlaybackInner()
        releaseAlarmCpuWakeLock()
    }

    private fun stopAlarmPlaybackInner() {
        synchronized(alarmLock) {
            try {
                activeAlarmPlayer?.apply {
                    stop()
                    release()
                }
            } catch (_: Exception) {
                // ignore
            }
            activeAlarmPlayer = null
            try {
                activeRingtone?.stop()
            } catch (_: Exception) {
                // ignore
            }
            activeRingtone = null
            try {
                activeToneGenerator?.release()
            } catch (_: Exception) {
                // ignore
            }
            activeToneGenerator = null
        }
    }
}

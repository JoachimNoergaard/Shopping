package dk.joachim.shopping.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dk.joachim.shopping.MainActivity
import dk.joachim.shopping.R

internal object RecipeTimerNotifications {

    /** v2: silent channel — Android does not allow changing sound on an existing channel. */
    private const val CHANNEL_ID = "recipe_timers_v2"
    private const val NOTIFICATION_ID = 7101

    /** Stopwatch (emoji presentation) when timer has finished — prefixed on that line only. */
    private const val TIMER_LINE_ICON = "\u23F1\uFE0F "

    private fun bold(text: String): CharSequence {
        return SpannableString(text).apply {
            setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
            )
        }
    }

    private fun timerDetailLine(t: RecipeStepTimerEntry): String {
        val label = t.recipeName.trim().let { text ->
            if (text.length > 72) text.take(72) + "…" else text
        }.ifBlank { t.stepPreview.trim().take(40) }
        val prefix = if (t.remainingSeconds <= 0) TIMER_LINE_ICON else ""
        return "${prefix}${RecipeStepTimer.formatClock(t.remainingSeconds)} — $label"
    }

    /** Text of the step after [sectionIndex]/[stepIndex], or first section of a later block. */
    private fun nextInstructionStepAfter(recipe: Recipe, sectionIndex: Int, stepIndex: Int): String? {
        val sections = recipe.instructionSections
        if (sectionIndex !in sections.indices) return null
        val steps = sections[sectionIndex].steps
        steps.getOrNull(stepIndex + 1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        for (s in (sectionIndex + 1) until sections.size) {
            sections[s].steps.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun truncateForNotification(text: String, maxLen: Int = 180): String {
        val t = text.trim()
        if (t.length <= maxLen) return t
        return t.take(maxLen - 1) + "…"
    }

    /**
     * Launcher intent for timer notification / alarm clock “show” tap.
     * One recipe → open that recipe; several recipes → Madlavning menu-plan overview.
     */
    fun mainActivityIntentForTimers(context: Context, timers: List<RecipeStepTimerEntry>): Intent {
        val distinctRecipes = timers.map { it.recipeId }.distinct()
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            when {
                distinctRecipes.size == 1 ->
                    putExtra(MainActivity.EXTRA_TIMER_RECIPE_ID, distinctRecipes.first())
                distinctRecipes.size > 1 ->
                    putExtra(MainActivity.EXTRA_TIMER_MENU_PLANS, true)
            }
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.timer_notification_channel_desc)
            setShowBadge(true)
            setSound(null, null)
            enableVibration(false)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    fun sync(context: Context, timers: List<RecipeStepTimerEntry>) {
        val nm = NotificationManagerCompat.from(context)
        if (timers.isEmpty()) {
            nm.cancel(NOTIFICATION_ID)
            return
        }
        ensureChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            7102,
            mainActivityIntentForTimers(context, timers),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val running = timers.count { it.isRunning }
        val titlePlain = when {
            timers.size == 1 -> {
                val only = timers.first()
                if (only.isRunning) {
                    context.getString(R.string.timer_notification_single_running)
                } else {
                    context.getString(R.string.timer_notification_single_paused)
                }
            }
            else -> context.getString(
                R.string.timer_notification_compact,
                timers.size,
                running,
            )
        }
        val title: CharSequence = bold(titlePlain)

        val subtitle = context.getString(R.string.timer_notification_subtitle)
        val onlyTimer = timers.singleOrNull()
        val nextStepForRunning: String? =
            if (onlyTimer != null && onlyTimer.isRunning && onlyTimer.remainingSeconds > 0) {
                ShoppingRepository.findRecipeById(onlyTimer.recipeId)
                    ?.let { nextInstructionStepAfter(it, onlyTimer.sectionIndex, onlyTimer.stepIndex) }
                    ?.let { truncateForNotification(it) }
            } else {
                null
            }

        val contentText: CharSequence = if (onlyTimer != null) {
            val b = SpannableStringBuilder()
            val line1 = timerDetailLine(onlyTimer)
            val start1 = b.length
            b.append(line1)
            b.setSpan(
                StyleSpan(Typeface.BOLD),
                start1,
                b.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
            )
            if (nextStepForRunning != null) {
                b.append('\n')
                val nextLine = context.getString(
                    R.string.timer_notification_next_step,
                    nextStepForRunning,
                )
                val start2 = b.length
                b.append(nextLine)
                b.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start2,
                    b.length,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            }
            b
        } else {
            subtitle
        }

        val expandedSpannable = SpannableStringBuilder()
        if (subtitle.isNotEmpty()) {
            val subStart = expandedSpannable.length
            expandedSpannable.append(subtitle)
            expandedSpannable.setSpan(
                StyleSpan(Typeface.BOLD),
                subStart,
                expandedSpannable.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
            )
            if (timers.isNotEmpty()) expandedSpannable.append('\n')
        }
        timers.forEach { t ->
            val line = timerDetailLine(t)
            val start = expandedSpannable.length
            expandedSpannable.append(line)
            expandedSpannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                expandedSpannable.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
            )
            expandedSpannable.append('\n')
        }
        if (nextStepForRunning != null) {
            val nextLine = context.getString(
                R.string.timer_notification_next_step,
                nextStepForRunning,
            )
            val start = expandedSpannable.length
            expandedSpannable.append(nextLine)
            expandedSpannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                expandedSpannable.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
            )
            expandedSpannable.append('\n')
        }
        val summaryText: CharSequence =
            if (subtitle.isNotEmpty()) bold(subtitle) else subtitle

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(expandedSpannable)
            .setSummaryText(summaryText)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .setContentIntent(openApp)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColorized(false)
            .setColor(NotificationCompat.COLOR_DEFAULT)
            .build()
        try {
            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // e.g. POST_NOTIFICATIONS not granted on API 33+
        }
    }
}

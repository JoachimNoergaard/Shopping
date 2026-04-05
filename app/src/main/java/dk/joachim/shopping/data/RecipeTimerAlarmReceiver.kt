package dk.joachim.shopping.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecipeTimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RecipeStepTimer.ACTION_TIMER_DEADLINE) return
        val id = intent.getStringExtra(RecipeStepTimer.EXTRA_TIMER_ID) ?: return
        RecipeStepTimer.handleSystemDeadline(id)
    }
}

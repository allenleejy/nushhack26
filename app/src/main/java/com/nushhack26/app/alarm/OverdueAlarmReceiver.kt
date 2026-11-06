package com.nushhack26.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by AlarmManager at a due time this phone's holder must meet. Card 05 posts the notification here. */
class OverdueAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Card 05.
    }
}

package com.nushhack26.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nushhack26.app.App
import kotlinx.coroutines.launch

/** Fired by AlarmManager at a due time this phone's holder must meet. Rings if the camera is still held and late. */
class OverdueAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(OverdueAlarm.EXTRA_ITEM_ID) ?: return
        val container = (context.applicationContext as App).container
        val pending = goAsync()
        container.scope.launch {
            try {
                val me = container.identity.current() ?: return@launch
                val now = System.currentTimeMillis()
                container.custodyRepository.heldDues(me.personId)
                    .firstOrNull { it.itemId == itemId && it.dueMs <= now }
                    ?.let { OverdueAlarm.notifyOnce(context.applicationContext, it) }
            } finally {
                pending.finish()
            }
        }
    }
}

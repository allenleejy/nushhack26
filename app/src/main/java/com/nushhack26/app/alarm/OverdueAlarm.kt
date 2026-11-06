package com.nushhack26.app.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nushhack26.app.AppContainer
import com.nushhack26.app.MainActivity
import com.nushhack26.app.R
import com.nushhack26.app.data.repo.CustodyRepository
import com.nushhack26.app.data.repo.HeldDue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * The overdue alarm (card 05). Works with no network: due times come from the signed records
 * on this phone. In the app, a one-second check notices a camera going overdue; in the
 * background, AlarmManager wakes [OverdueAlarmReceiver] at each due time. Each loan alarms once.
 */
object OverdueAlarm {
    const val CHANNEL_ID = "overdue_returns"
    const val EXTRA_ITEM_ID = "item_id"
    private const val PREFS = "overdue_alarm"
    private const val KEY_SCHEDULED = "scheduled"
    private const val KEY_NOTIFIED = "notified"
    private const val KEY_ASKED_PERMISSION = "asked_permission"

    /** Watches this phone's custody for as long as the process lives. Call once from App. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(context: Context, container: AppContainer) {
        val app = context.applicationContext
        val repo = container.custodyRepository
        val held = container.identity.identity.flatMapLatest { me ->
            if (me == null) flowOf(emptyList()) else repo.observeHeldDues(me.personId)
        }
        container.scope.launch {
            held.collect { dues -> reschedule(app, dues) }
        }
        container.scope.launch {
            combine(held, CustodyRepository.ticker()) { dues, now -> dues.filter { it.dueMs <= now } }
                .collect { late -> late.forEach { notifyOnce(app, it) } }
        }
    }

    /** One exact-ish alarm per held camera, at its due time; alarms for cameras no longer held are cancelled. */
    fun reschedule(context: Context, dues: List<HeldDue>) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wanted = dues.map { it.itemId }.toSet()
        prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().filter { it !in wanted }.forEach { itemId ->
            alarms.cancel(pendingIntent(context, itemId))
        }
        dues.forEach { due ->
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due.dueMs, pendingIntent(context, due.itemId))
        }
        // Forget alarms already sounded for loans that have ended, so the next loan alarms again.
        val heads = dues.map { it.headId }.toSet()
        prefs.edit()
            .putStringSet(KEY_SCHEDULED, wanted)
            .putStringSet(KEY_NOTIFIED, prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty().filter { it in heads }.toSet())
            .apply()
    }

    /** Posts the overdue notification for [due], unless this loan has already alarmed. */
    @Synchronized
    fun notifyOnce(context: Context, due: HeldDue) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val notified = prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty()
        if (due.headId in notified) return
        if (!canNotify(context)) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${due.code} is overdue")
            .setContentText("Return this camera to the store now.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(VIBRATION)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(due.itemId.hashCode(), notification)
        } catch (_: SecurityException) {
            return
        }
        prefs.edit().putStringSet(KEY_NOTIFIED, notified + due.headId).apply()
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "Overdue returns", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rings when a camera you hold is past its due time."
            enableVibration(true)
            vibrationPattern = VIBRATION
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
        }
        manager.createNotificationChannel(channel)
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(context: Context, itemId: String): PendingIntent = PendingIntent.getBroadcast(
        context, itemId.hashCode(),
        Intent(context, OverdueAlarmReceiver::class.java).putExtra(EXTRA_ITEM_ID, itemId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Asked once, the first time this phone's person holds a camera (Android 13+). */
    @Composable
    fun RequestPermissionWhenHolding(holding: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(holding) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (holding && !canNotify(context) && !prefs.getBoolean(KEY_ASKED_PERMISSION, false)) {
                prefs.edit().putBoolean(KEY_ASKED_PERMISSION, true).apply()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val VIBRATION = longArrayOf(0, 600, 300, 600, 300, 600)
}

package com.nextinera.smsbroadcaster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nextinera.smsbroadcaster.Prefs.appSecret
import com.nextinera.smsbroadcaster.Prefs.deviceName
import com.nextinera.smsbroadcaster.Prefs.enabled
import com.nextinera.smsbroadcaster.Prefs.isConfigured
import com.nextinera.smsbroadcaster.Prefs.serverUrl
import java.util.concurrent.TimeUnit

/**
 * Receives every incoming SMS and hands it to a WorkManager job that
 * reliably POSTs it to the server (survives offline / reboot via retries).
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!context.enabled || !context.isConfigured()) return

        // A single SMS can arrive as multiple parts — join them per sender.
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: "Unknown"
        val body = buildString {
            for (m in messages) append(m.messageBody ?: "")
        }
        if (body.isBlank()) return

        val sentAt = messages[0].timestampMillis

        val data = Data.Builder()
            .putString(ForwardWorker.KEY_URL, context.serverUrl)
            .putString(ForwardWorker.KEY_SECRET, context.appSecret)
            .putString(ForwardWorker.KEY_SENDER, sender)
            .putString(ForwardWorker.KEY_BODY, body)
            .putString(ForwardWorker.KEY_DEVICE, context.deviceName)
            .putLong(ForwardWorker.KEY_SENT_AT, sentAt)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = OneTimeWorkRequestBuilder<ForwardWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag("sms_forward")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueue(req)
    }
}

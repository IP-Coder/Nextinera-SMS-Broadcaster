package com.nextinera.smsbroadcaster

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Does the actual network POST. Returns Result.retry() on any failure so
 * WorkManager retries with exponential backoff until it succeeds.
 */
class ForwardWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_SECRET = "secret"
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_DEVICE = "device"
        const val KEY_SENT_AT = "sent_at"
    }

    override fun doWork(): Result {
        val url = inputData.getString(KEY_URL).orEmpty()
        val secret = inputData.getString(KEY_SECRET).orEmpty()
        if (url.isBlank() || secret.isBlank()) return Result.failure()

        val sender = inputData.getString(KEY_SENDER).orEmpty()
        val body = inputData.getString(KEY_BODY).orEmpty()
        val device = inputData.getString(KEY_DEVICE).orEmpty()
        val sentMillis = inputData.getLong(KEY_SENT_AT, 0L)

        val payload = JSONObject().apply {
            put("sender", sender)
            put("message", body)
            put("device", device)
            put("sent_at", iso(sentMillis))
            put("secret", secret)
        }.toString()

        return try {
            val code = post(url, secret, payload)
            when {
                code in 200..299 -> Result.success()
                code in 400..499 -> Result.failure() // bad secret/request: retrying won't help
                else -> Result.retry()                // 5xx / server down
            }
        } catch (e: Exception) {
            Result.retry() // network error, try again later
        }
    }

    private fun post(urlStr: String, secret: String, json: String): Int {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("X-App-Secret", secret)
        }
        conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        conn.disconnect()
        return code
    }

    private fun iso(millis: Long): String {
        if (millis <= 0L) return ""
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        fmt.timeZone = TimeZone.getDefault()
        return fmt.format(Date(millis))
    }
}

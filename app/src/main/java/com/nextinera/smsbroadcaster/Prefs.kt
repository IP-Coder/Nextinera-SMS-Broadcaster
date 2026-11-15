package com.nextinera.smsbroadcaster

import android.content.Context

/** Tiny wrapper over SharedPreferences for the app's settings. */
object Prefs {
    private const val FILE = "sms_broadcaster_prefs"
    private const val KEY_URL = "server_url"
    private const val KEY_SECRET = "app_secret"
    private const val KEY_DEVICE = "device_name"
    private const val KEY_ENABLED = "enabled"

    // Pre-filled defaults so the app works right after install (edit in-app anytime).
    private const val DEFAULT_URL = "https://nextinera.com/sms/sms.php"
    private const val DEFAULT_SECRET = "85985c4817acf64cc9fe9afc2f22bfdc0d972eaac97ff111"

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var Context.serverUrl: String
        get() = sp(this).getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(v) { sp(this).edit().putString(KEY_URL, v.trim()).apply() }

    var Context.appSecret: String
        get() = sp(this).getString(KEY_SECRET, DEFAULT_SECRET) ?: DEFAULT_SECRET
        set(v) { sp(this).edit().putString(KEY_SECRET, v.trim()).apply() }

    var Context.deviceName: String
        get() = sp(this).getString(KEY_DEVICE, android.os.Build.MODEL ?: "Android") ?: "Android"
        set(v) { sp(this).edit().putString(KEY_DEVICE, v.trim()).apply() }

    var Context.enabled: Boolean
        get() = sp(this).getBoolean(KEY_ENABLED, true)
        set(v) { sp(this).edit().putBoolean(KEY_ENABLED, v).apply() }

    fun Context.isConfigured(): Boolean =
        serverUrl.isNotBlank() && appSecret.isNotBlank()
}

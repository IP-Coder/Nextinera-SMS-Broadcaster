package com.nextinera.smsbroadcaster

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.materialswitch.MaterialSwitch
import com.nextinera.smsbroadcaster.Prefs.appSecret
import com.nextinera.smsbroadcaster.Prefs.deviceName
import com.nextinera.smsbroadcaster.Prefs.enabled
import com.nextinera.smsbroadcaster.Prefs.serverUrl

class MainActivity : AppCompatActivity() {

    private lateinit var urlField: EditText
    private lateinit var secretField: EditText
    private lateinit var deviceField: EditText
    private lateinit var enabledSwitch: MaterialSwitch
    private lateinit var status: TextView

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlField = findViewById(R.id.urlField)
        secretField = findViewById(R.id.secretField)
        deviceField = findViewById(R.id.deviceField)
        enabledSwitch = findViewById(R.id.enabledSwitch)
        status = findViewById(R.id.status)

        urlField.setText(serverUrl)
        secretField.setText(appSecret)
        deviceField.setText(deviceName)
        enabledSwitch.isChecked = enabled

        findViewById<Button>(R.id.saveBtn).setOnClickListener { save() }
        findViewById<Button>(R.id.testBtn).setOnClickListener { sendTest() }
        findViewById<Button>(R.id.permBtn).setOnClickListener { requestPerms() }
        findViewById<Button>(R.id.batteryBtn).setOnClickListener { requestIgnoreBattery() }

        enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            enabled = isChecked
            refreshStatus()
        }

        requestPerms()
        refreshStatus()
    }

    private fun save() {
        serverUrl = urlField.text.toString()
        appSecret = secretField.text.toString()
        deviceName = deviceField.text.toString().ifBlank { Build.MODEL ?: "Android" }
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun sendTest() {
        save()
        if (serverUrl.isBlank() || appSecret.isBlank()) {
            Toast.makeText(this, "Enter server URL and secret first", Toast.LENGTH_LONG).show()
            return
        }
        val data = Data.Builder()
            .putString(ForwardWorker.KEY_URL, serverUrl)
            .putString(ForwardWorker.KEY_SECRET, appSecret)
            .putString(ForwardWorker.KEY_SENDER, "TEST")
            .putString(ForwardWorker.KEY_BODY, "Test message from SMS Broadcaster. OTP 654321")
            .putString(ForwardWorker.KEY_DEVICE, deviceName)
            .putLong(ForwardWorker.KEY_SENT_AT, System.currentTimeMillis())
            .build()
        val req = OneTimeWorkRequestBuilder<ForwardWorker>().setInputData(data).build()
        WorkManager.getInstance(this).enqueue(req)
        Toast.makeText(this, "Test sent — check Telegram", Toast.LENGTH_LONG).show()
    }

    private fun requestPerms() {
        val perms = mutableListOf(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permLauncher.launch(perms.toTypedArray())
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBattery() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun hasSmsPerm(): Boolean =
        checkSelfPermission(Manifest.permission.RECEIVE_SMS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun refreshStatus() {
        val configured = serverUrl.isNotBlank() && appSecret.isNotBlank()
        val sms = hasSmsPerm()
        val lines = StringBuilder()
        lines.append(if (sms) "✓ SMS permission granted\n" else "✗ SMS permission NOT granted\n")
        lines.append(if (configured) "✓ Server configured\n" else "✗ Server URL / secret missing\n")
        lines.append(if (enabled) "✓ Forwarding is ON" else "✗ Forwarding is OFF")
        status.text = lines.toString()
    }
}

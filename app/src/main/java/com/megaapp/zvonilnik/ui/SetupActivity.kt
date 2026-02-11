package com.megaapp.zvonilnik.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.megaapp.zvonilnik.R

class SetupActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnRetry: Button

    private var stepInProgress = false

    private val runtimePermsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            stepInProgress = false
            runNextStep()
        }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            stepInProgress = false
            runNextStep()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        tvStatus = findViewById(R.id.tvSetupStatus)
        btnRetry = findViewById(R.id.btnSetupRetry)

        btnRetry.setOnClickListener {
            stepInProgress = false
            runNextStep()
        }

        runNextStep()
    }

    override fun onResume() {
        super.onResume()
        if (!stepInProgress) runNextStep()
    }

    private fun runNextStep() {
        // 1) Runtime permissions
        if (!hasRuntimePermissions()) {
            tvStatus.text = "Шаг 1/4: Разрешения приложения (уведомления, контакты, phone state)…"
            btnRetry.visibility = Button.GONE
            stepInProgress = true

            val perms = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_CONTACTS)
                add(Manifest.permission.READ_PHONE_STATE)
            }.toTypedArray()

            runtimePermsLauncher.launch(perms)
            return
        }

        // 2) Exact alarms
        if (!hasExactAlarmsAccess()) {
            tvStatus.text = "Шаг 2/4: Разрешить точные будильники (Exact alarms)…"
            btnRetry.visibility = Button.GONE
            stepInProgress = true

            launchSettingsSafe(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            })
            return
        }

        // 3) Full screen intents (Android 14+)
        if (!hasFullScreenIntentAccess()) {
            tvStatus.text = "Шаг 3/4: Разрешить Full-screen intents (Android 14+)…"
            btnRetry.visibility = Button.GONE
            stepInProgress = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                launchSettingsSafe(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                })
            } else {
                stepInProgress = false
                runNextStep()
            }
            return
        }

        // 4) Battery optimizations
        if (!isIgnoringBatteryOptimizations()) {
            tvStatus.text = "Шаг 4/4: Разрешить работу без ограничений батареи…"
            btnRetry.visibility = Button.GONE
            stepInProgress = true

            launchSettingsSafe(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
            return
        }

        tvStatus.text = "Готово ✅\nМожно возвращаться и тестировать звонильник."
        btnRetry.visibility = Button.GONE
        finish()
    }

    private fun hasRuntimePermissions(): Boolean {
        val contactsOk = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED

        val phoneStateOk = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED

        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true

        return contactsOk && notifOk && phoneStateOk
    }

    private fun hasExactAlarmsAccess(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = getSystemService(AlarmManager::class.java)
        return am.canScheduleExactAlarms()
    }

    private fun hasFullScreenIntentAccess(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = getSystemService(NotificationManager::class.java)
        return nm.canUseFullScreenIntent()
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun launchSettingsSafe(intent: Intent) {
        try {
            settingsLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            btnRetry.visibility = Button.VISIBLE
            tvStatus.text = tvStatus.text.toString() + "\n\nНе смог открыть экран напрямую. Открой настройки приложения вручную."
            stepInProgress = false

            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            runCatching { settingsLauncher.launch(fallback) }.onFailure {
                btnRetry.visibility = Button.VISIBLE
            }
        } catch (_: SecurityException) {
            btnRetry.visibility = Button.VISIBLE
            tvStatus.text = tvStatus.text.toString() + "\n\nСистема не дала открыть экран. Открой вручную через Settings."
            stepInProgress = false
        }
    }
}

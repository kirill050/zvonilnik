package com.megaapp.zvonilnik

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.megaapp.zvonilnik.data.DbProvider

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        runCatching {
            DbProvider.init(this)
        }.onFailure {
            Log.e("Zvonilnik", "DbProvider.init failed", it)
        }

        runCatching {
            createNotificationChannels()
        }.onFailure {
            Log.e("Zvonilnik", "createNotificationChannels failed", it)
        }

        // Telecom/PhoneAccount ПОКА НЕ ТРОГАЕМ.
        // На Samsung требует READ_PHONE_NUMBERS даже на getPhoneAccount().
        // Вернем позже отдельным шагом, если реально понадобится BT-интеграция.
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = getSystemService(NotificationManager::class.java)
        val calls = NotificationChannel(
            Consts.NOTIF_CHANNEL_CALLS,
            "Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming call simulation"
            setShowBadge(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(calls)
    }
}

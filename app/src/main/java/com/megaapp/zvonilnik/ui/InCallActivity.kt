package com.megaapp.zvonilnik.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.megaapp.zvonilnik.R
import com.megaapp.zvonilnik.telecom.CallController
import com.megaapp.zvonilnik.telecom.CallSessionStore
import com.megaapp.zvonilnik.telecom.Ringer

class InCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Гарантированно гасим ringing
        Ringer.startInCall()

        setContentView(R.layout.activity_in_call)

        val s = CallSessionStore.current

        val name = intent.getStringExtra(IncomingCallActivity.EXTRA_NAME)
            ?: s?.displayName
            ?: s?.displayNumber
            ?: "#0Неизвестный"

        val number = intent.getStringExtra(IncomingCallActivity.EXTRA_NUMBER)
            ?: s?.displayNumber
            ?: ":"

        findViewById<TextView>(R.id.tvName).text = name
        findViewById<TextView>(R.id.tvNumber).text = number

        // Фото/буква
        val iv = findViewById<ImageView>(R.id.ivAvatar)
        val tvLetter = findViewById<TextView>(R.id.tvAvatarLetter)
        val photo = s?.photoUri

        if (!photo.isNullOrBlank()) {
            iv.setImageURI(Uri.parse(photo))
            iv.visibility = View.VISIBLE
            tvLetter.visibility = View.GONE
        } else {
            iv.visibility = View.GONE
            tvLetter.visibility = View.VISIBLE
            tvLetter.text = avatarLetter(name)
        }

        // Таймер с сохранением базы активного времени
        val ch = findViewById<Chronometer>(R.id.chronometer)
        val activeAtMs = s?.activeAt
        ch.base = if (activeAtMs != null) {
            val delta = (System.currentTimeMillis() - activeAtMs).coerceAtLeast(0L)
            SystemClock.elapsedRealtime() - delta
        } else {
            SystemClock.elapsedRealtime()
        }
        ch.start()

        findViewById<View>(R.id.btnEnd).setOnClickListener {
            CallController.hangup(this)
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* ignore */ }
        })
    }

    private fun avatarLetter(name: String): String {
        val trimmed = name.removePrefix("#0").trim()
        return trimmed.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }
}

package com.megaapp.zvonilnik.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.app.KeyguardManager
import android.app.WallpaperManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.megaapp.zvonilnik.R
import com.megaapp.zvonilnik.telecom.CallController
import com.megaapp.zvonilnik.telecom.CallSessionStore
import android.os.Environment
import android.provider.Settings

class IncomingCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        // Замена deprecated flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {  // API 27+
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km.requestDismissKeyguard(this, null)  // Если нужно unlock; иначе убери
        } else {
            // Fallback для старых, если нужно
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_incoming_call)
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
        val bg = findViewById<ImageView>(R.id.bgWallpaper)

        try {

            val wm = WallpaperManager.getInstance(this)

            val pfd = wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)

            if (pfd != null) {
                val bitmap = android.graphics.BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                pfd.close()

                bg.setImageBitmap(bitmap)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bg.setRenderEffect(
                RenderEffect.createBlurEffect(
                    120f,
                    120f,
                    Shader.TileMode.CLAMP
                )
            )
        }

        bindOrFinish()

        findViewById<DualSwipeActionsView>(R.id.dualSwipe).apply {
            onAnswer = {
                CallSessionStore.current?.let { s ->
                    CallController.answer(this@IncomingCallActivity)
                    startActivity(
                        Intent(this@IncomingCallActivity, InCallActivity::class.java).apply {
                            putExtra(EXTRA_NAME, s.displayName ?: s.displayNumber)
                            putExtra(EXTRA_NUMBER, s.displayNumber)
                            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        }
                    )
                    finish()
                }
            }
            onDecline = {
                CallController.decline(this@IncomingCallActivity)
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

    override fun onResume() {
        super.onResume()
        bindOrFinish()
    }

    private fun bindOrFinish() {
        val s = CallSessionStore.current
        if (s == null || s.state != CallSessionStore.State.RINGING) {
            finish()
            return
        }

        val name = s.displayName ?: s.displayNumber
        findViewById<TextView>(R.id.tvName).text = name
        findViewById<TextView>(R.id.tvNumber).text = s.displayNumber

        val iv = findViewById<ImageView>(R.id.ivAvatar)
        val tvLetter = findViewById<TextView>(R.id.tvAvatarLetter)

        val photo = s.photoUri
        if (!photo.isNullOrBlank()) {
            iv.setImageURI(Uri.parse(photo))
            iv.visibility = ImageView.VISIBLE
            tvLetter.visibility = TextView.GONE
        } else {
            iv.visibility = ImageView.GONE
            tvLetter.visibility = TextView.VISIBLE
            tvLetter.text = avatarLetter(name)
        }
    }

    private fun avatarLetter(name: String): String {
        val trimmed = name.removePrefix("#0").trim()
        return trimmed.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }

    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_NUMBER = "extra_number"
    }
}

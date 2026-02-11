package com.megaapp.zvonilnik.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object Ringer {
    private const val TAG = "Zvonilnik"

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    // Вызывается при старте входящего (ringing)
    fun start(context: Context) {
        val am = context.getSystemService(AudioManager::class.java)
        val mode = am.ringerMode

        val shouldVibrate = (mode == AudioManager.RINGER_MODE_VIBRATE || mode == AudioManager.RINGER_MODE_NORMAL)
        val shouldRing = (mode == AudioManager.RINGER_MODE_NORMAL)

        Log.e(TAG, "Ringer.start: mode=$mode ring=$shouldRing vib=$shouldVibrate")

        if (shouldRing) startRingtone(context) else stopRingtone()
        if (shouldVibrate) startVibrate(context) else stopVibrate()
    }

    // Вызывается при переходе в “разговор” (ACTIVE)
    fun startInCall() {
        // КЛЮЧЕВО: при ответе прекращаем “ringing” полностью
        stopRingtone()
        stopVibrate()
    }

    fun stopAll() {
        stopRingtone()
        stopVibrate()
    }

    private fun startRingtone(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val rt = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= 21) {
                rt.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone = rt
            rt.play()
        } catch (t: Throwable) {
            Log.e(TAG, "startRingtone failed", t)
        }
    }

    private fun stopRingtone() {
        try {
            ringtone?.stop()
        } catch (_: Throwable) {
        } finally {
            ringtone = null
        }
    }

    private fun getVibrator(context: Context): Vibrator {
        if (vibrator != null) return vibrator!!

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        return vibrator!!
    }

    private fun startVibrate(context: Context) {
        try {
            val v = getVibrator(context)
            val pattern = longArrayOf(0, 400, 250, 400, 250, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "startVibrate failed", t)
        }
    }

    private fun stopVibrate() {
        try {
            vibrator?.cancel()
        } catch (_: Throwable) {
        }
    }
}

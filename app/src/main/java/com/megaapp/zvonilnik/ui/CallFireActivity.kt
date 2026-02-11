package com.megaapp.zvonilnik.ui

import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telecom.TelecomManager
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.megaapp.zvonilnik.Consts
import com.megaapp.zvonilnik.alarm.ZvonilnikScheduler
import com.megaapp.zvonilnik.data.DbProvider
import com.megaapp.zvonilnik.notif.CallNotification
import com.megaapp.zvonilnik.telecom.CallSessionStore
import java.util.concurrent.Executors

class CallFireActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val id = intent?.getLongExtra(Consts.EXTRA_ID, -1L) ?: -1L
        Log.e("Zvonilnik", "CallFireActivity.onCreate id=$id")

        if (id <= 0L) {
            finish()
            return
        }

        val pm = getSystemService(PowerManager::class.java)
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "zvonilnik:fireActivity").apply {
            setReferenceCounted(false)
            acquire(15_000L)
        }

        io.execute {
            try {
                runCatching { DbProvider.init(applicationContext) }

                val dao = runCatching { DbProvider.dao() }.getOrElse {
                    Log.e("Zvonilnik", "DbProvider.dao failed", it)
                    runOnUiThread { finish() }
                    return@execute
                }

                val z = dao.getById(id)
                Log.e("Zvonilnik", "Fire loaded entity: ${z != null}")

                if (z == null || !z.enabled) {
                    runOnUiThread { finish() }
                    return@execute
                }

                // Если идёт реальный звонок — переносим на минуту
                val telecom = getSystemService(TelecomManager::class.java)
                val inCall = runCatching { telecom.isInCall }.getOrElse {
                    Log.e("Zvonilnik", "telecom.isInCall failed (no permission?)", it)
                    false
                }

                if (inCall) {
                    val postponed = System.currentTimeMillis() + 60_000L
                    dao.update(z.copy(nextTriggerAtMillis = postponed))
                    ZvonilnikScheduler.scheduleExact(this, id, postponed)
                    Log.e("Zvonilnik", "In real call -> postponed 60s")
                    runOnUiThread { finish() }
                    return@execute
                }

                // Префиксы по ТЗ
                val displayName = z.contactName?.let { "#0$it" }
                val displayNumber = if (z.contactName.isNullOrBlank()) ":${z.phoneNumber}" else z.phoneNumber
                Log.e("Zvonilnik", "Start ringing name=$displayName number=$displayNumber")

                runCatching {
                    CallSessionStore.startLocalRinging(this, id, displayName, displayNumber)
                }.onFailure {
                    Log.e("Zvonilnik", "startLocalRinging failed", it)
                }

                runCatching {
                    CallNotification.showIncoming(this, displayName, displayNumber)
                }.onFailure {
                    Log.e("Zvonilnik", "showIncoming notif failed", it)
                }

                // UI входящего. Здесь делаем нормальный try/finally (никаких runCatching+finally).
                runOnUiThread {
                    try {
                        startActivity(
                            android.content.Intent(this, IncomingCallActivity::class.java).apply {
                                flags =
                                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                    } catch (t: Throwable) {
                        Log.e("Zvonilnik", "start IncomingCallActivity failed", t)
                    } finally {
                        finish()
                    }
                }

                // Перескейджулинг
                if (z.repeatMask != 0) {
                    val next = ZvonilnikScheduler.computeNextTriggerMillis(z.hour, z.minute, z.repeatMask)
                    dao.update(z.copy(nextTriggerAtMillis = next))
                    ZvonilnikScheduler.scheduleExact(this, id, next)
                } else {
                    dao.update(z.copy(enabled = false))
                }
            } catch (t: Throwable) {
                Log.e("Zvonilnik", "FIRE FAILED", t)
                runOnUiThread { finish() }
            } finally {
                runCatching { if (wl.isHeld) wl.release() }
            }
        }
    }
}

package com.megaapp.zvonilnik.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.telecom.TelecomManager
import com.megaapp.zvonilnik.Consts
import com.megaapp.zvonilnik.data.DbProvider
import com.megaapp.zvonilnik.notif.CallNotification
import com.megaapp.zvonilnik.telecom.CallSessionStore
import com.megaapp.zvonilnik.ui.IncomingCallActivity
import java.util.concurrent.Executors

class ZvonilnikAlarmReceiver : BroadcastReceiver() {

    private val io = Executors.newSingleThreadExecutor()

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Consts.ACTION_FIRE) return

        val pending = goAsync()

        val id = intent.getLongExtra(Consts.EXTRA_ID, -1L)
        if (id <= 0L) {
            pending.finish()
            return
        }

//        val pm = context.getSystemService(PowerManager::class.java)
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "zvonilnik:alarm").apply {
//            setReferenceCounted(false)
//            acquire(15_000L)
        }
        wl.acquire(10_000)

        io.execute {
            try {
                val dao = DbProvider.dao()
                val z = dao.getById(id) ?: return@execute
                if (!z.enabled) return@execute

                // Если идёт реальный звонок — переносим на минуту
                val telecom = context.getSystemService(TelecomManager::class.java)
                if (telecom.isInCall) {
                    val postponed = System.currentTimeMillis() + 60_000L
                    dao.update(z.copy(nextTriggerAtMillis = postponed))
                    ZvonilnikScheduler.scheduleExact(context, id, postponed)
                    return@execute
                }

                // Префиксы по ТЗ
                val displayName = z.contactName?.let { "#0$it" }
                val displayNumber = if (z.contactName.isNullOrBlank()) ":${z.phoneNumber}" else z.phoneNumber

                // 1) Всегда стартуем локальный RINGING (это даст звук/вибро независимо от UI)
                CallSessionStore.startLocalRinging(context, id, displayName, displayNumber)

                // 2) Full-screen notification (должна поднять IncomingCallActivity на lockscreen)
                CallNotification.showIncoming(context, displayName, displayNumber)

                // 3) Если экран НЕ залочен — явно открываем IncomingCallActivity (фикс кейса "я в приложении и оно вылетает")
                val km = context.getSystemService(KeyguardManager::class.java)
//                if (!km.isKeyguardLocked) {
                    runCatching {
                        context.startActivity(
                            Intent(context, IncomingCallActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                        )
                    }
//                }

                // Перескейджулим следующий (если повтор)
                if (z.repeatMask != 0) {
                    val next = ZvonilnikScheduler.computeNextTriggerMillis(
                        hour = z.hour,
                        minute = z.minute,
                        repeatMask = z.repeatMask
                    )
                    dao.update(z.copy(nextTriggerAtMillis = next))
                    ZvonilnikScheduler.scheduleExact(context, id, next)
                } else {
                    dao.update(z.copy(enabled = false))
                }
            } finally {
                runCatching { if (wl.isHeld) wl.release() }
                pending.finish()
            }
        }
    }
}

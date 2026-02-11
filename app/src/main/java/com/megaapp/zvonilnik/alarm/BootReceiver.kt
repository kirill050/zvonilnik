package com.megaapp.zvonilnik.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.megaapp.zvonilnik.data.DbProvider
import java.util.concurrent.Executors

class BootReceiver : BroadcastReceiver() {

    private val io = Executors.newSingleThreadExecutor()

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()
        val appCtx = context.applicationContext

        io.execute {
            try {
                runCatching { DbProvider.init(appCtx) }

                val dao = DbProvider.dao()
                val all = dao.getAllSorted()
                val now = System.currentTimeMillis()

                all.forEach { z ->
                    if (!z.enabled) return@forEach

                    // Если next уже в прошлом — решаем что делать
                    if (z.nextTriggerAtMillis <= now) {
                        if (z.repeatMask == 0) {
                            // одноразовый “проспан” -> выключаем
                            dao.update(z.copy(enabled = false))
                            Log.e("Zvonilnik", "BOOT: disable past one-shot id=${z.id}")
                            return@forEach
                        } else {
                            // повторяющийся -> пересчитать следующий
                            val next = ZvonilnikScheduler.computeNextTriggerMillis(
                                hour = z.hour,
                                minute = z.minute,
                                repeatMask = z.repeatMask,
                                nowMillis = now
                            )
                            dao.update(z.copy(nextTriggerAtMillis = next))
                            ZvonilnikScheduler.scheduleExact(appCtx, z.id, next)
                            Log.e("Zvonilnik", "BOOT: reschedule repeat id=${z.id} -> $next")
                            return@forEach
                        }
                    }

                    // next в будущем — просто восстановить аларм
                    ZvonilnikScheduler.scheduleExact(appCtx, z.id, z.nextTriggerAtMillis)
                    Log.e("Zvonilnik", "BOOT: reschedule id=${z.id} -> ${z.nextTriggerAtMillis}")
                }
            } catch (t: Throwable) {
                Log.e("Zvonilnik", "BOOT: failed", t)
            } finally {
                pending.finish()
            }
        }
    }
}

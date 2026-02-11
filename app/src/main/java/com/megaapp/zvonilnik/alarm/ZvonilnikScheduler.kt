package com.megaapp.zvonilnik.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.megaapp.zvonilnik.Consts

object ZvonilnikScheduler {

    fun scheduleExact(context: Context, id: Long, triggerAtMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java)

        val firePi = buildFirePendingIntent(context, id)

        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, firePi),
            firePi
        )
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = findFirePendingIntent(context, id) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    private fun buildFirePendingIntent(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, ZvonilnikFireReceiver::class.java).apply {
            action = Consts.ACTION_FIRE
            putExtra(Consts.EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun findFirePendingIntent(context: Context, id: Long): PendingIntent? {
        val intent = Intent(context, ZvonilnikFireReceiver::class.java).apply {
            action = Consts.ACTION_FIRE
            putExtra(Consts.EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

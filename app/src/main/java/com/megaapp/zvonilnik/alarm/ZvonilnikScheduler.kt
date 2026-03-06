package com.megaapp.zvonilnik.alarm

import android.app.AlarmManager
import android.os.Build
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.megaapp.zvonilnik.Consts
import com.megaapp.zvonilnik.ui.CallFireActivity
import com.megaapp.zvonilnik.ui.MainActivity
import java.time.*
import java.time.temporal.ChronoUnit
import android.provider.Settings
import android.util.Log

object ZvonilnikScheduler {

//    fun scheduleExact(context: Context, id: Long, triggerAtMillis: Long) {
//        val am = context.getSystemService(AlarmManager::class.java)
//
//        val firePi = buildFirePendingIntent(context, id)
//
//        val showPi = PendingIntent.getActivity(
//            context,
//            9001,
//            Intent(context, MainActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
//            },
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showPi), firePi)
//    }
fun scheduleExact(context: Context, id: Long, triggerAtMillis: Long) {

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (!alarmManager.canScheduleExactAlarms()) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        Log.w("Zvonilnik", "Exact alarm permission required")
        return
    }

    val intent = Intent(context, ZvonilnikAlarmReceiver::class.java).apply {
        action = Consts.ACTION_FIRE
        putExtra(Consts.EXTRA_ID, id)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        id.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent
    )

    Log.i("Zvonilnik", "Alarm scheduled id=$id time=$triggerAtMillis")
}

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java)

        val pi = findFirePendingIntentOrNull(context, id)
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
        // если null — это нормально (alarms могло не быть), просто no-op
    }

//    private fun buildFirePendingIntent(context: Context, id: Long): PendingIntent {
//        val intent = buildFireIntent(context, id)
//        return PendingIntent.getActivity(
//            context,
//            requestCode(id),
//            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//    }
    private fun buildFirePendingIntent(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, ZvonilnikAlarmReceiver::class.java).apply {
            action = Consts.ACTION_FIRE
            putExtra(Consts.EXTRA_ID, id)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode(id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

//    private fun findFirePendingIntentOrNull(context: Context, id: Long): PendingIntent? {
//        val intent = buildFireIntent(context, id)
//        return PendingIntent.getActivity(
//            context,
//            requestCode(id),
//            intent,
//            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
//        )
//    }
        private fun findFirePendingIntentOrNull(context: Context, id: Long): PendingIntent? {
            val intent = Intent(context, ZvonilnikAlarmReceiver::class.java).apply {
                action = Consts.ACTION_FIRE
                putExtra(Consts.EXTRA_ID, id)
            }

            return PendingIntent.getBroadcast(
                context,
                requestCode(id),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        }

    private fun buildFireIntent(context: Context, id: Long): Intent {
        return Intent(context, CallFireActivity::class.java).apply {
            action = Consts.ACTION_FIRE
            putExtra(Consts.EXTRA_ID, id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }

    private fun requestCode(id: Long): Int {
        val x = id % Int.MAX_VALUE
        return x.toInt()
    }

    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        repeatMask: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone).truncatedTo(ChronoUnit.SECONDS)

        fun atDay(d: LocalDate): ZonedDateTime =
            d.atTime(hour, minute).atZone(zone)

        if (repeatMask == 0) {
            var candidate = atDay(now.toLocalDate())
            if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
            return candidate.toInstant().toEpochMilli()
        }

        for (delta in 0..7) {
            val d = now.toLocalDate().plusDays(delta.toLong())
            val zdt = atDay(d)
            if (!zdt.isAfter(now)) continue

            val dow = zdt.dayOfWeek
            if (matchesRepeatMask(dow, repeatMask)) {
                return zdt.toInstant().toEpochMilli()
            }
        }

        return now.plusMinutes(1).toInstant().toEpochMilli()
    }

    private fun matchesRepeatMask(dow: DayOfWeek, mask: Int): Boolean {
        val bit = when (dow) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }
        return (mask and (1 shl bit)) != 0
    }
}

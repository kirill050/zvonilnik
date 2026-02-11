package com.megaapp.zvonilnik.notif

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.megaapp.zvonilnik.Consts
import com.megaapp.zvonilnik.R
import com.megaapp.zvonilnik.ui.InCallActivity
import com.megaapp.zvonilnik.ui.IncomingCallActivity
import com.megaapp.zvonilnik.ui.MainActivity

object CallNotification {

    fun showIncoming(context: Context, displayName: String?, displayNumber: String) {
        val fullScreen = PendingIntent.getActivity(
            context,
            2001,
            Intent(context, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerPi = PendingIntent.getBroadcast(
            context,
            2002,
            Intent(context, CallActionReceiver::class.java).apply { action = Consts.ACTION_ANSWER },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declinePi = PendingIntent.getBroadcast(
            context,
            2003,
            Intent(context, CallActionReceiver::class.java).apply { action = Consts.ACTION_DECLINE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = displayName ?: displayNumber

        val n = NotificationCompat.Builder(context, Consts.NOTIF_CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Входящий вызов")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true) // звук/вибро делаем сами
            .setFullScreenIntent(fullScreen, true)
            .addAction(0, "Сбросить", declinePi)
            .addAction(0, "Ответить", answerPi)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(Consts.NOTIF_ID_CALL, n)
        }
    }

    fun showOngoing(context: Context, displayName: String?, displayNumber: String) {
        val contentPi = PendingIntent.getActivity(
            context,
            2010,
            Intent(context, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hangupPi = PendingIntent.getBroadcast(
            context,
            2011,
            Intent(context, CallActionReceiver::class.java).apply { action = Consts.ACTION_HANGUP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = displayName ?: displayNumber

        val n = NotificationCompat.Builder(context, Consts.NOTIF_CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Идёт вызов")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setContentIntent(contentPi)
            .addAction(0, "Завершить", hangupPi)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(Consts.NOTIF_ID_CALL, n)
        }
    }

    fun showMissed(context: Context, displayName: String?, displayNumber: String) {
        val openPi = PendingIntent.getActivity(
            context,
            2020,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = displayName ?: displayNumber

        val n = NotificationCompat.Builder(context, Consts.NOTIF_CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Пропущенный вызов")
            .setContentText(title)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(Consts.NOTIF_ID_MISSED, n)
        }
    }

    fun cancel(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(Consts.NOTIF_ID_CALL)
        }
    }
}

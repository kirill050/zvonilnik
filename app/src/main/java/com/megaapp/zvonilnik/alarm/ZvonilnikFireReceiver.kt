package com.megaapp.zvonilnik.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.megaapp.zvonilnik.Consts
import com.megaapp.zvonilnik.notifications.IncomingCallNotifier

class ZvonilnikFireReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val id = intent?.getLongExtra(Consts.EXTRA_ID, -1L) ?: -1L
        if (id <= 0) return

        Log.e("Zvonilnik", "FireReceiver triggered id=$id")

        IncomingCallNotifier.showIncoming(context, id)
    }
}

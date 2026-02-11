package com.megaapp.zvonilnik.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.megaapp.zvonilnik.Consts
import com.megaapp.zvonilnik.telecom.CallController
import com.megaapp.zvonilnik.ui.InCallActivity

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Consts.ACTION_ANSWER -> {
                CallController.answer(context)
                // Пользователь нажал кнопку — запуск activity обычно разрешён
                runCatching {
                    context.startActivity(
                        Intent(context, InCallActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                }
            }
            Consts.ACTION_DECLINE -> CallController.decline(context)
            Consts.ACTION_HANGUP -> CallController.hangup(context)
        }
    }
}

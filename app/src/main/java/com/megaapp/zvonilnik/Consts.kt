package com.megaapp.zvonilnik

object Consts {
    const val ACTION_FIRE = "com.megaapp.zvonilnik.ACTION_FIRE"
    const val EXTRA_ID = "extra_id"

    const val EXTRA_NAME = "extra_name"
    const val EXTRA_NUMBER = "extra_number"

    const val ACTION_ANSWER = "com.megaapp.zvonilnik.ACTION_ANSWER"
    const val ACTION_DECLINE = "com.megaapp.zvonilnik.ACTION_DECLINE"
    const val ACTION_HANGUP = "com.megaapp.zvonilnik.ACTION_HANGUP"

    const val NOTIF_CHANNEL_CALLS = "calls"
    const val NOTIF_ID_CALL = 10_001
    const val NOTIF_ID_MISSED = 10_002

    const val CALL_TIMEOUT_MS = 30_000L

    // Пока Telecom не используем, но исходники ConnectionService/Registrar компилируются.
    const val PHONE_ACCOUNT_ID = "zvonilnik_self_managed"
}

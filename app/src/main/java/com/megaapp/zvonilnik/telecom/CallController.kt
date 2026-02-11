package com.megaapp.zvonilnik.telecom

import android.content.Context

object CallController {
    fun answer(context: Context) {
        CallSessionStore.markActive(context)
    }

    fun decline(context: Context) {
        CallSessionStore.decline(context)
    }

    fun hangup(context: Context) {
        CallSessionStore.hangup(context)
    }
}

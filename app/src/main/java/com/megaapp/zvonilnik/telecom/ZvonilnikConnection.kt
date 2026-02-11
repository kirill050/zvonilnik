package com.megaapp.zvonilnik.telecom

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.TelecomManager
import com.megaapp.zvonilnik.Consts

class ZvonilnikConnection(
    private val appContext: Context,
    private val displayName: String?,
    private val displayNumber: String
) : Connection() {

    private val main = Handler(Looper.getMainLooper())
    private var timeoutPosted = false

    init {
        connectionProperties = PROPERTY_SELF_MANAGED
        audioModeIsVoip = true

        setAddress(Uri.parse("tel:$displayNumber"), TelecomManager.PRESENTATION_ALLOWED)
        setCallerDisplayName(displayName ?: displayNumber, TelecomManager.PRESENTATION_ALLOWED)
    }

    fun scheduleTimeout() {
        if (timeoutPosted) return
        timeoutPosted = true

        main.postDelayed({
            if (state == STATE_RINGING) {
                setDisconnected(DisconnectCause(DisconnectCause.MISSED))
                destroy()
                CallSessionStore.disconnectAsMissed(appContext)
            }
        }, Consts.CALL_TIMEOUT_MS)
    }

    override fun onAnswer() {
        setActive()
        CallSessionStore.markActive(appContext)
    }

    override fun onReject() {
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
        CallSessionStore.decline(appContext)
    }

    override fun onDisconnect() {
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
        CallSessionStore.hangup(appContext)
    }
}

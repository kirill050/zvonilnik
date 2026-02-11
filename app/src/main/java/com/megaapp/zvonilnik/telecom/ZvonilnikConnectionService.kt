package com.megaapp.zvonilnik.telecom

import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import com.megaapp.zvonilnik.Consts

class ZvonilnikConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {
        val extras = request.extras ?: Bundle.EMPTY

        val displayName = extras.getString(Consts.EXTRA_NAME)
        val displayNumber = extras.getString(Consts.EXTRA_NUMBER) ?: ":unknown"
        val id = extras.getLong(Consts.EXTRA_ID, -1L)

        val conn = ZvonilnikConnection(applicationContext, displayName, displayNumber)
        conn.setRinging()
        conn.scheduleTimeout()

        // Мы больше не храним Connection внутри Store.
        // Но если Telecom когда-то будет активирован — стартуем ringing-сессию так же, как в обычном флоу.
        CallSessionStore.startLocalRinging(applicationContext, id, displayName, displayNumber)

        return conn
    }
}

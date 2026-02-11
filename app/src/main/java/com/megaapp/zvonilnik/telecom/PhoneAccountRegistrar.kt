package com.megaapp.zvonilnik.telecom

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.megaapp.zvonilnik.Consts

object PhoneAccountRegistrar {

    fun ensureRegistered(context: Context): PhoneAccountHandle {
        val appContext = context.applicationContext
        val telecom = appContext.getSystemService(TelecomManager::class.java)

        val handle = PhoneAccountHandle(
            ComponentName(appContext, ZvonilnikConnectionService::class.java),
            Consts.PHONE_ACCOUNT_ID
        )

        // На некоторых Samsung getPhoneAccount может требовать READ_PHONE_NUMBERS.
        val existing = try {
            telecom.getPhoneAccount(handle)
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        }

        if (existing == null) {
            val pa = PhoneAccount.builder(handle, "Zvonilnik")
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                .build()
            runCatching { telecom.registerPhoneAccount(pa) }
        }

        return handle
    }

    fun buildIncomingExtras(displayName: String?, displayNumber: String, id: Long = -1L): Bundle {
        return Bundle().apply {
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, Uri.parse("tel:$displayNumber"))
            putString(Consts.EXTRA_NAME, displayName)
            putString(Consts.EXTRA_NUMBER, displayNumber)
            putLong(Consts.EXTRA_ID, id)
        }
    }
}

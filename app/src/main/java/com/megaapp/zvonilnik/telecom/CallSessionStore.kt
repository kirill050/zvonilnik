package com.megaapp.zvonilnik.telecom

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.megaapp.zvonilnik.Consts
import com.megaapp.zvonilnik.contacts.ContactPhotoResolver
import com.megaapp.zvonilnik.notif.CallNotification

object CallSessionStore {

    enum class State { RINGING, ACTIVE, DISCONNECTED }

    private val main = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    @Volatile
    var current: Session? = null
        private set

    data class Session(
        val zvonilnikId: Long,
        val displayName: String?,
        val displayNumber: String,
        val photoUri: String?,
        val createdAt: Long,
        var state: State,
        var activeAt: Long? = null
    )

    fun startLocalRinging(context: Context, id: Long, displayName: String?, displayNumber: String) {
        val appCtx = context.applicationContext

        val photo = ContactPhotoResolver.findPhotoUri(appCtx, displayNumber)

        current = Session(
            zvonilnikId = id,
            displayName = displayName,
            displayNumber = displayNumber,
            photoUri = photo,
            createdAt = System.currentTimeMillis(),
            state = State.RINGING
        )

        cancelTimeout()
        scheduleTimeout(appCtx)

        Ringer.start(appCtx)
    }

    fun markActive(context: Context) {
        val appCtx = context.applicationContext
        val s = current ?: return
        if (s.state == State.ACTIVE) return

        s.state = State.ACTIVE
        s.activeAt = System.currentTimeMillis()

        cancelTimeout()

        // прекращаем рингтон/вибро
        Ringer.startInCall()

        CallNotification.showOngoing(appCtx, s.displayName, s.displayNumber)
    }

    fun decline(context: Context) = disconnectInternal(context.applicationContext, missed = false)
    fun hangup(context: Context) = disconnectInternal(context.applicationContext, missed = false)
    fun disconnectAsMissed(context: Context) = disconnectInternal(context.applicationContext, missed = true)

    private fun disconnectInternal(appCtx: Context, missed: Boolean) {
        val s = current ?: return
        s.state = State.DISCONNECTED

        cancelTimeout()
        Ringer.stopAll()

        CallNotification.cancel(appCtx)
        if (missed) CallNotification.showMissed(appCtx, s.displayName, s.displayNumber)

        current = null
    }

    private fun scheduleTimeout(appCtx: Context) {
        val r = Runnable {
            val s = current ?: return@Runnable
            if (s.state == State.RINGING) disconnectInternal(appCtx, missed = true)
        }
        timeoutRunnable = r
        main.postDelayed(r, Consts.CALL_TIMEOUT_MS)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { main.removeCallbacks(it) }
        timeoutRunnable = null
    }
}

package com.megaapp.zvonilnik.data

import android.content.Context
import androidx.room.Room

object DbProvider {
    @Volatile
    private var db: AppDb? = null

    fun init(context: Context) {
        if (db != null) return
        synchronized(this) {
            if (db == null) {
                db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "zvonilnik.db"
                ).build()
            }
        }
    }

    fun dao(): ZvonilnikDao = requireNotNull(db).dao()
}

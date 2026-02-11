package com.megaapp.zvonilnik.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ZvonilnikEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): ZvonilnikDao
}

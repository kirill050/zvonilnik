package com.megaapp.zvonilnik.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zvonilnik")
data class ZvonilnikEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    val enabled: Boolean = true,

    // Повторы как у будильника: bit0=Mon ... bit6=Sun. 0 = одноразовый
    val repeatMask: Int = 0,

    // Для UI/редактора (пока минимум)
    val hour: Int,
    val minute: Int,

    // Кто “звонит”
    val contactName: String? = null,
    val phoneNumber: String,

    // Абсолютное время следующего срабатывания (RTC millis)
    val nextTriggerAtMillis: Long
)

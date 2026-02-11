package com.megaapp.zvonilnik.util

import java.util.Calendar

object ZvonilnikTime {

    /**
     * Возвращает ближайшее будущее время срабатывания
     */
    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        now: Long = System.currentTimeMillis()
    ): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

        // если время уже прошло сегодня — переносим на завтра
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return cal.timeInMillis
    }
}

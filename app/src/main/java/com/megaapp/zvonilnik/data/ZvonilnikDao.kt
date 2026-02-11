package com.megaapp.zvonilnik.data

import androidx.room.*

@Dao
interface ZvonilnikDao {

    @Insert
    fun insert(entity: ZvonilnikEntity): Long

    @Update
    fun update(entity: ZvonilnikEntity)

    @Delete
    fun delete(entity: ZvonilnikEntity)

    @Query("SELECT * FROM zvonilnik WHERE id = :id LIMIT 1")
    fun getById(id: Long): ZvonilnikEntity?

    @Query("SELECT * FROM zvonilnik ORDER BY enabled DESC, nextTriggerAtMillis ASC")
    fun getAllSorted(): List<ZvonilnikEntity>

    @Query("SELECT COUNT(*) FROM zvonilnik WHERE enabled = 1 AND hour = :hour AND minute = :minute")
    fun countEnabledAtTime(hour: Int, minute: Int): Int

    @Query("DELETE FROM zvonilnik")
    fun clear()
}

package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DrspDao {
    @Query("SELECT * FROM drsp_daily_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<DrspDailyLog>>

    @Query("SELECT * FROM drsp_daily_logs WHERE date = :date LIMIT 1")
    suspend fun getLogByDate(date: String): DrspDailyLog?

    @Query("SELECT * FROM drsp_daily_logs ORDER BY date DESC LIMIT 30")
    fun getRecentLogs(): Flow<List<DrspDailyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DrspDailyLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<DrspDailyLog>)
}

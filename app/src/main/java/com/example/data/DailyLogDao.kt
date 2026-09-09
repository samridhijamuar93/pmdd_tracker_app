package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_drsp_logs ORDER BY dateString ASC")
    fun getAllLogsFlow(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_drsp_logs ORDER BY dateString ASC")
    suspend fun getAllLogs(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_drsp_logs WHERE dateString = :dateString LIMIT 1")
    fun getLogByDateFlow(dateString: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_drsp_logs WHERE dateString = :dateString LIMIT 1")
    suspend fun getLogByDate(dateString: String): DailyLogEntity?

    @Query("SELECT * FROM daily_drsp_logs WHERE dateString >= :startDate AND dateString <= :endDate ORDER BY dateString ASC")
    fun getLogsBetweenDatesFlow(startDate: String, endDate: String): Flow<List<DailyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<DailyLogEntity>)

    @Update
    suspend fun updateLog(log: DailyLogEntity)

    @Query("DELETE FROM daily_drsp_logs WHERE dateString = :dateString")
    suspend fun deleteLogByDate(dateString: String)

    @Query("DELETE FROM daily_drsp_logs")
    suspend fun clearAll()
}

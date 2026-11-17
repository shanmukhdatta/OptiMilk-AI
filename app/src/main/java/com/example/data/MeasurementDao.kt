package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY timestampMillis DESC")
    fun observeAllMeasurements(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE isDemo = 0 ORDER BY timestampMillis DESC LIMIT 1")
    fun observeLatestRealMeasurement(): Flow<MeasurementEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(entity: MeasurementEntity): Long

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM measurements WHERE isDemo = 1")
    suspend fun deleteAllDemoMeasurements()

    @Query("DELETE FROM measurements")
    suspend fun deleteAllMeasurements()
}

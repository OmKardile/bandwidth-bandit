package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM daily_usage ORDER BY date DESC")
    fun getAllUsages(): Flow<List<UsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE date = :date LIMIT 1")
    suspend fun getUsageByDate(date: String): UsageEntity?

    @Query("SELECT * FROM daily_usage WHERE date = :date LIMIT 1")
    fun getUsageByDateFlow(date: String): Flow<UsageEntity?>

    @Query("SELECT * FROM daily_usage WHERE date LIKE :monthPattern ORDER BY date ASC")
    fun getUsagesByMonth(monthPattern: String): Flow<List<UsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUsage(usage: UsageEntity)
}

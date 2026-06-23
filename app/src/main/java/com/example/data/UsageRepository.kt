package com.example.data

import kotlinx.coroutines.flow.Flow

class UsageRepository(private val usageDao: UsageDao) {
    val allUsages: Flow<List<UsageEntity>> = usageDao.getAllUsages()

    suspend fun getUsageByDate(date: String): UsageEntity? {
        return usageDao.getUsageByDate(date)
    }

    fun getUsageByDateFlow(date: String): Flow<UsageEntity?> {
        return usageDao.getUsageByDateFlow(date)
    }

    fun getUsagesByMonth(month: String): Flow<List<UsageEntity>> {
        // month should be "yyyy-MM", let's build the SQL pattern
        return usageDao.getUsagesByMonth("$month%")
    }

    suspend fun insertOrUpdateUsage(usage: UsageEntity) {
        usageDao.insertOrUpdateUsage(usage)
    }

    /**
     * Add data bytes safely by checking if an entry exists for today, inserting or updating it.
     */
    suspend fun addDataBytes(date: String, mobileRxDelta: Long, mobileTxDelta: Long, wifiRxDelta: Long, wifiTxDelta: Long) {
        val existing = usageDao.getUsageByDate(date)
        if (existing == null) {
            val newEntry = UsageEntity(
                date = date,
                mobileRx = maxOf(0L, mobileRxDelta),
                mobileTx = maxOf(0L, mobileTxDelta),
                wifiRx = maxOf(0L, wifiRxDelta),
                wifiTx = maxOf(0L, wifiTxDelta)
            )
            usageDao.insertOrUpdateUsage(newEntry)
        } else {
            val updated = existing.copy(
                mobileRx = existing.mobileRx + maxOf(0L, mobileRxDelta),
                mobileTx = existing.mobileTx + maxOf(0L, mobileTxDelta),
                wifiRx = existing.wifiRx + maxOf(0L, wifiRxDelta),
                wifiTx = existing.wifiTx + maxOf(0L, wifiTxDelta)
            )
            usageDao.insertOrUpdateUsage(updated)
        }
    }
}

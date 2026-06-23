package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_usage")
data class UsageEntity(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val mobileRx: Long = 0L,
    val mobileTx: Long = 0L,
    val wifiRx: Long = 0L,
    val wifiTx: Long = 0L
)

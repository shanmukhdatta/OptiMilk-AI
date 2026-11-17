package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sampleName: String,
    val timestampMillis: Long,
    val red: Int,
    val green: Int,
    val blue: Int,
    val clear: Int,
    val concentration: Double,
    val unit: String,
    val status: String,
    val deviceName: String,
    val isDemo: Boolean = false,
)

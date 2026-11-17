package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MeasurementEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OptiMilkDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: OptiMilkDatabase? = null

        fun getInstance(context: Context): OptiMilkDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OptiMilkDatabase::class.java,
                    "optimilk_ai.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}

package com.example.data

import kotlinx.coroutines.flow.Flow

class MeasurementRepository(
    private val measurementDao: MeasurementDao,
    private val preferences: AppPreferences? = null,
) {
    val allMeasurements: Flow<List<MeasurementEntity>> = measurementDao.observeAllMeasurements()
    val latestRealMeasurement: Flow<MeasurementEntity?> = measurementDao.observeLatestRealMeasurement()
    val settingsFlow: Flow<UserSettings>? = preferences?.settingsFlow

    suspend fun saveMeasurement(entity: MeasurementEntity): Long {
        return measurementDao.insertMeasurement(entity)
    }

    suspend fun deleteMeasurement(id: Long) {
        measurementDao.deleteById(id)
    }

    suspend fun clearDemoMeasurements() {
        measurementDao.deleteAllDemoMeasurements()
    }

    suspend fun clearAllMeasurements() {
        measurementDao.deleteAllMeasurements()
    }

    suspend fun setDemoModeEnabled(enabled: Boolean) {
        preferences?.setDemoModeEnabled(enabled)
    }

    suspend fun setDefaultSamplePrefix(prefix: String) {
        preferences?.setDefaultSamplePrefix(prefix)
    }

    suspend fun saveLastDevice(address: String, name: String) {
        preferences?.saveLastDevice(address, name)
    }
}

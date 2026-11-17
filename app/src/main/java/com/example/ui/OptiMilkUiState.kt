package com.example.ui

import com.example.ble.BleConnectionState
import com.example.ble.DiscoveredBleDevice
import com.example.data.MeasurementEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MeasurementStatus(val label: String) {
    READY("Ready"),
    MEASURING("Measuring"),
    COMPLETE("Complete"),
    ERROR("Error"),
}

enum class CalibrationStatus {
    IDLE,
    CALIBRATING,
    SUCCESS,
    ERROR,
}

data class LiveReading(
    val r: Int,
    val g: Int,
    val b: Int,
    val c: Int,
    val concentration: Double,
    val unit: String,
    val timestampMillis: Long,
    val sampleName: String,
    val deviceName: String,
    val isDemo: Boolean,
    val isSaved: Boolean = false,
)

enum class HistoryFilter(val label: String) {
    ALL("All"),
    HARDWARE("ESP32"),
    DEMO("Demo"),
}

data class OptiMilkUiState(
    val connectionState: BleConnectionState = BleConnectionState.Disconnected,
    val discoveredDevices: List<DiscoveredBleDevice> = emptyList(),
    val isScanning: Boolean = false,
    val isBluetoothSupported: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothPermissions: Boolean = false,
    val bleErrorMessage: String? = null,
    val measurementStatus: MeasurementStatus = MeasurementStatus.READY,
    val measurementError: String? = null,
    val currentReading: LiveReading? = null,
    val editableSampleName: String = "Milk Sample #1",
    val calibrationStatus: CalibrationStatus = CalibrationStatus.IDLE,
    val calibrationMessage: String? = null,
    val lastCalibrationTimestamp: Long? = null,
    val lastSuccessfulMeasurementTimestamp: Long? = null,
    val demoModeEnabled: Boolean = false,
    val defaultSamplePrefix: String = "Milk Sample",
    val lastDeviceAddress: String? = null,
    val lastDeviceName: String? = null,
    val historyItems: List<MeasurementEntity> = emptyList(),
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val selectedHistoryItem: MeasurementEntity? = null,
    val statusBannerMessage: String? = null,
) {
    val isBusy: Boolean
        get() = measurementStatus == MeasurementStatus.MEASURING ||
            calibrationStatus == CalibrationStatus.CALIBRATING

    val isHardwareConnected: Boolean
        get() = connectionState is BleConnectionState.Connected

    val canSendDeviceCommands: Boolean
        get() = !isBusy && (isHardwareConnected || demoModeEnabled)

    val filteredHistoryItems: List<MeasurementEntity>
        get() = when (historyFilter) {
            HistoryFilter.ALL -> historyItems
            HistoryFilter.HARDWARE -> historyItems.filter { !it.isDemo }
            HistoryFilter.DEMO -> historyItems.filter { it.isDemo }
        }
}

object OptiMilkFormatters {
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
    private val shortTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun formatDateTime(millis: Long): String {
        return dateTimeFormat.format(Date(millis))
    }

    fun formatShortTime(millis: Long): String {
        return shortTimeFormat.format(Date(millis))
    }

    fun formatConcentration(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }
}

package com.example.ble

import kotlinx.coroutines.flow.StateFlow

data class DiscoveredBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val advertisesNus: Boolean,
    val lastSeenMillis: Long = System.currentTimeMillis(),
)

sealed interface BleConnectionState {
    data object Disconnected : BleConnectionState
    data class Connecting(val deviceName: String, val address: String) : BleConnectionState
    data class Connected(
        val deviceName: String,
        val address: String,
        val mtu: Int = BleProtocol.DEFAULT_ATT_MTU,
    ) : BleConnectionState
    data class Error(val message: String, val address: String? = null) : BleConnectionState
}

interface ColorimeterClient {
    val connectionState: StateFlow<BleConnectionState>
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>>
    val isScanning: StateFlow<Boolean>
    val lastError: StateFlow<String?>

    fun isBluetoothSupported(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun hasRequiredPermissions(): Boolean

    fun startScan()
    fun stopScan()
    fun connect(address: String, displayName: String? = null)
    fun disconnect()
    fun clearError()

    suspend fun calibrateBlank(): Result<BleResponse.CalibrationResult>
    suspend fun measureSample(): Result<BleResponse.MeasurementResult>
}

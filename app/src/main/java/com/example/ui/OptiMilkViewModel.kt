package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ble.AndroidBleColorimeterClient
import com.example.ble.BleConnectionState
import com.example.ble.ColorimeterClient
import com.example.ble.DemoColorimeterSimulator
import com.example.ble.DiscoveredBleDevice
import com.example.data.AppPreferences
import com.example.data.MeasurementEntity
import com.example.data.MeasurementRepository
import com.example.data.OptiMilkDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OptiMilkViewModel(
    private val colorimeterClient: ColorimeterClient,
    private val repository: MeasurementRepository,
) : AndroidViewModel(Application()) {

    private var sampleCounter = 1
    private val _uiState = MutableStateFlow(OptiMilkUiState())
    val uiState: StateFlow<OptiMilkUiState> = _uiState.asStateFlow()

    init {
        refreshBluetoothCapabilities()
        observeClientAndData()
    }

    private fun observeClientAndData() {
        viewModelScope.launch {
            combine(
                colorimeterClient.connectionState,
                colorimeterClient.discoveredDevices,
                colorimeterClient.isScanning,
                colorimeterClient.lastError,
            ) { conn, devices, scanning, bleErr ->
                BleSnapshot(conn, devices, scanning, bleErr)
            }.collect { snap ->
                _uiState.update { state ->
                    val connected = snap.connectionState as? BleConnectionState.Connected
                    if (connected != null) {
                        viewModelScope.launch {
                            repository.saveLastDevice(connected.address, connected.deviceName)
                        }
                    }
                    state.copy(
                        connectionState = snap.connectionState,
                        discoveredDevices = snap.devices,
                        isScanning = snap.isScanning,
                        bleErrorMessage = snap.bleError,
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.allMeasurements.collect { items ->
                sampleCounter = (items.size + 1).coerceAtLeast(1)
                _uiState.update { state ->
                    val latestReal = items.firstOrNull { !it.isDemo }
                    state.copy(
                        historyItems = items,
                        lastSuccessfulMeasurementTimestamp =
                            state.lastSuccessfulMeasurementTimestamp ?: latestReal?.timestampMillis,
                    )
                }
            }
        }

        repository.settingsFlow?.let { settingsFlow ->
            viewModelScope.launch {
                settingsFlow.collect { settings ->
                    _uiState.update { state ->
                        val updatedName = if (state.currentReading == null || state.currentReading.isSaved) {
                            "${settings.defaultSamplePrefix} #$sampleCounter"
                        } else {
                            state.editableSampleName
                        }
                        // Never leave Demo readings displayed when Demo Mode is turned off
                        val cleanedReading = if (!settings.demoModeEnabled && state.currentReading?.isDemo == true) {
                            null
                        } else {
                            state.currentReading
                        }
                        state.copy(
                            demoModeEnabled = settings.demoModeEnabled,
                            defaultSamplePrefix = settings.defaultSamplePrefix,
                            lastDeviceAddress = settings.lastDeviceAddress,
                            lastDeviceName = settings.lastDeviceName,
                            editableSampleName = updatedName,
                            currentReading = cleanedReading,
                            measurementStatus = if (cleanedReading == null && state.measurementStatus == MeasurementStatus.COMPLETE) {
                                MeasurementStatus.READY
                            } else {
                                state.measurementStatus
                            },
                        )
                    }
                }
            }
        }
    }

    fun refreshBluetoothCapabilities() {
        _uiState.update {
            it.copy(
                isBluetoothSupported = colorimeterClient.isBluetoothSupported(),
                isBluetoothEnabled = colorimeterClient.isBluetoothEnabled(),
                hasBluetoothPermissions = colorimeterClient.hasRequiredPermissions(),
            )
        }
    }

    fun startBleScan() {
        refreshBluetoothCapabilities()
        colorimeterClient.startScan()
    }

    fun stopBleScan() {
        colorimeterClient.stopScan()
    }

    fun connectToDevice(device: DiscoveredBleDevice) {
        refreshBluetoothCapabilities()
        colorimeterClient.connect(device.address, device.name)
    }

    fun reconnectLastDevice() {
        val address = _uiState.value.lastDeviceAddress ?: return
        val name = _uiState.value.lastDeviceName
        refreshBluetoothCapabilities()
        colorimeterClient.connect(address, name)
    }

    fun disconnectDevice() {
        colorimeterClient.disconnect()
        _uiState.update {
            it.copy(
                calibrationStatus = CalibrationStatus.IDLE,
                calibrationMessage = null,
            )
        }
    }

    fun clearBleError() {
        colorimeterClient.clearError()
        _uiState.update { it.copy(bleErrorMessage = null) }
    }

    fun updateEditableSampleName(name: String) {
        _uiState.update { it.copy(editableSampleName = name) }
    }

    fun calibrateBlank() {
        val snapshot = _uiState.value
        if (snapshot.isBusy) return

        val useDemo = !snapshot.isHardwareConnected && snapshot.demoModeEnabled
        if (!snapshot.isHardwareConnected && !useDemo) {
            _uiState.update {
                it.copy(
                    calibrationStatus = CalibrationStatus.ERROR,
                    calibrationMessage = "Connect ESP32 over Bluetooth before calibrating blank cuvette.",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    calibrationStatus = CalibrationStatus.CALIBRATING,
                    calibrationMessage = if (useDemo) {
                        "Simulating blank cuvette calibration..."
                    } else {
                        "Sending CALIBRATE command to ESP32..."
                    },
                )
            }

            val result = if (useDemo) {
                DemoColorimeterSimulator.simulateCalibration()
            } else {
                colorimeterClient.calibrateBlank()
            }

            result.fold(
                onSuccess = { ack ->
                    val now = System.currentTimeMillis()
                    _uiState.update {
                        it.copy(
                            calibrationStatus = CalibrationStatus.SUCCESS,
                            lastCalibrationTimestamp = now,
                            calibrationMessage = ack.message ?: if (useDemo) {
                                "Demo blank calibration acknowledged"
                            } else {
                                "Blank calibration complete"
                            },
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            calibrationStatus = CalibrationStatus.ERROR,
                            calibrationMessage = error.message ?: "Blank calibration failed.",
                        )
                    }
                },
            )
        }
    }

    fun measureSample() {
        val snapshot = _uiState.value
        if (snapshot.isBusy) return

        val useDemo = !snapshot.isHardwareConnected && snapshot.demoModeEnabled
        if (!snapshot.isHardwareConnected && !useDemo) {
            _uiState.update {
                it.copy(
                    measurementStatus = MeasurementStatus.ERROR,
                    measurementError = "ESP32 is disconnected. Connect your colorimeter before measuring.",
                )
            }
            return
        }

        viewModelScope.launch {
            val nextSampleName = "${snapshot.defaultSamplePrefix} #$sampleCounter"
            _uiState.update {
                it.copy(
                    measurementStatus = MeasurementStatus.MEASURING,
                    measurementError = null,
                    statusBannerMessage = null,
                    editableSampleName = nextSampleName,
                )
            }

            val result = if (useDemo) {
                DemoColorimeterSimulator.simulateMeasurement()
            } else {
                colorimeterClient.measureSample()
            }

            result.fold(
                onSuccess = { payload ->
                    val now = System.currentTimeMillis()
                    val deviceLabel = when (val conn = _uiState.value.connectionState) {
                        is BleConnectionState.Connected -> conn.deviceName
                        else -> "Demo Simulator"
                    }
                    val reading = LiveReading(
                        r = payload.r,
                        g = payload.g,
                        b = payload.b,
                        c = payload.c,
                        concentration = payload.concentration,
                        unit = payload.unit,
                        timestampMillis = now,
                        sampleName = nextSampleName,
                        deviceName = deviceLabel,
                        isDemo = useDemo,
                        isSaved = false,
                    )
                    _uiState.update {
                        it.copy(
                            measurementStatus = MeasurementStatus.COMPLETE,
                            measurementError = null,
                            currentReading = reading,
                            lastSuccessfulMeasurementTimestamp = if (useDemo) {
                                it.lastSuccessfulMeasurementTimestamp
                            } else {
                                now
                            },
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            measurementStatus = MeasurementStatus.ERROR,
                            measurementError = error.message ?: "Measurement failed or timed out.",
                        )
                    }
                },
            )
        }
    }

    fun saveCurrentReading() {
        val state = _uiState.value
        val reading = state.currentReading ?: return
        if (reading.isSaved) return

        val chosenName = state.editableSampleName.trim().ifEmpty {
            "${state.defaultSamplePrefix} #$sampleCounter"
        }

        viewModelScope.launch {
            val entity = MeasurementEntity(
                sampleName = chosenName,
                timestampMillis = reading.timestampMillis,
                red = reading.r,
                green = reading.g,
                blue = reading.b,
                clear = reading.c,
                concentration = reading.concentration,
                unit = reading.unit,
                status = "ok",
                deviceName = reading.deviceName,
                isDemo = reading.isDemo,
            )
            repository.saveMeasurement(entity)
            sampleCounter++
            _uiState.update { current ->
                current.copy(
                    currentReading = current.currentReading?.copy(
                        sampleName = chosenName,
                        isSaved = true,
                    ),
                    editableSampleName = "${current.defaultSamplePrefix} #$sampleCounter",
                    statusBannerMessage = if (reading.isDemo) {
                        "Saved '$chosenName' to History (tagged DEMO)"
                    } else {
                        "Saved '$chosenName' to History"
                    },
                )
            }
        }
    }

    fun dismissStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun selectHistoryItem(item: MeasurementEntity?) {
        _uiState.update { it.copy(selectedHistoryItem = item) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteMeasurement(id)
            _uiState.update { state ->
                if (state.selectedHistoryItem?.id == id) {
                    state.copy(selectedHistoryItem = null)
                } else {
                    state
                }
            }
        }
    }

    fun setDemoModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDemoModeEnabled(enabled)
        }
    }

    fun setDefaultSamplePrefix(prefix: String) {
        viewModelScope.launch {
            repository.setDefaultSamplePrefix(prefix)
        }
    }

    fun clearDemoHistory() {
        viewModelScope.launch {
            repository.clearDemoMeasurements()
            _uiState.update { it.copy(statusBannerMessage = "Cleared simulated demo readings") }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllMeasurements()
            _uiState.update { it.copy(statusBannerMessage = "Cleared all local measurement history") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        colorimeterClient.disconnect()
    }

    private data class BleSnapshot(
        val connectionState: BleConnectionState,
        val devices: List<DiscoveredBleDevice>,
        val isScanning: Boolean,
        val bleError: String?,
    )

    class Factory(private val appContext: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = OptiMilkDatabase.getInstance(appContext)
            val prefs = AppPreferences(appContext)
            val repo = MeasurementRepository(db.measurementDao(), prefs)
            val bleClient = AndroidBleColorimeterClient(appContext.applicationContext)
            return OptiMilkViewModel(bleClient, repo) as T
        }
    }
}

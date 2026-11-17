package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.charset.StandardCharsets

/**
 * Production Android BLE GATT client implementing the Nordic UART Service (NUS) contract
 * for the OptiMilk-AI ESP32 colorimeter.
 */
class AndroidBleColorimeterClient(
    private val context: Context,
) : ColorimeterClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commandMutex = Mutex()
    private val frameBuffer = IncomingFrameBuffer()

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private var activeGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var negotiatedMtu: Int = BleProtocol.DEFAULT_ATT_MTU

    private var scanTimeoutJob: Job? = null
    private var mtuFallbackJob: Job? = null
    private var pendingDeviceName: String = "OptiMilk ESP32"
    private var pendingDeviceAddress: String = ""

    @Volatile
    private var writeAckDeferred: CompletableDeferred<Boolean>? = null

    @Volatile
    private var pendingResponseDeferred: CompletableDeferred<Result<BleResponse>>? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    override fun isBluetoothSupported(): Boolean {
        val hasBleFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        return hasBleFeature && bluetoothAdapter != null
    }

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    override fun hasRequiredPermissions(): Boolean {
        return BlePermissionHelper.hasAllPermissions(context)
    }

    override fun clearError() {
        _lastError.value = null
        if (_connectionState.value is BleConnectionState.Error) {
            _connectionState.value = BleConnectionState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        if (!isBluetoothSupported()) {
            _lastError.value = "Bluetooth Low Energy is not supported on this device."
            return
        }
        if (!BlePermissionHelper.hasScanPermission(context)) {
            _lastError.value = "Bluetooth scan permission is required to discover ESP32 devices."
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _lastError.value = "Bluetooth is turned off. Please enable Bluetooth to scan."
            return
        }
        val scanner: BluetoothLeScanner? = adapter.bluetoothLeScanner
        if (scanner == null) {
            _lastError.value = "BLE scanner is unavailable. Check that Bluetooth is enabled."
            return
        }

        if (_isScanning.value) return

        _lastError.value = null
        _discoveredDevices.value = emptyList()
        _isScanning.value = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (e: SecurityException) {
            _isScanning.value = false
            _lastError.value = "Missing Bluetooth scan permission: ${e.message}"
            return
        } catch (e: Exception) {
            _isScanning.value = false
            _lastError.value = "Unable to start BLE scan: ${e.message}"
            return
        }

        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(BleProtocol.SCAN_DURATION_MS)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        if (!_isScanning.value) return
        _isScanning.value = false
        try {
            if (BlePermissionHelper.hasScanPermission(context)) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
        } catch (_: Exception) {
            // Safe cleanup if Bluetooth was turned off mid-scan
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(address: String, displayName: String?) {
        stopScan()
        if (!isBluetoothSupported()) {
            val msg = "Bluetooth Low Energy is not supported on this device."
            _lastError.value = msg
            _connectionState.value = BleConnectionState.Error(msg, address)
            return
        }
        if (!BlePermissionHelper.hasConnectPermission(context)) {
            val msg = "Bluetooth connect permission is required."
            _lastError.value = msg
            _connectionState.value = BleConnectionState.Error(msg, address)
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            val msg = "Bluetooth is disabled. Enable Bluetooth and try again."
            _lastError.value = msg
            _connectionState.value = BleConnectionState.Error(msg, address)
            return
        }

        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            val msg = "Invalid Bluetooth device address: $address"
            _lastError.value = msg
            _connectionState.value = BleConnectionState.Error(msg, address)
            return
        }

        closeCurrentGatt()

        val resolvedName = displayName?.takeIf { it.isNotBlank() }
            ?: safeDeviceName(device)
            ?: "ESP32 Colorimeter"

        pendingDeviceName = resolvedName
        pendingDeviceAddress = address
        negotiatedMtu = BleProtocol.DEFAULT_ATT_MTU
        frameBuffer.clear()
        _lastError.value = null
        _connectionState.value = BleConnectionState.Connecting(resolvedName, address)

        try {
            activeGatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE,
            )
            if (activeGatt == null) {
                val msg = "Failed to initiate GATT connection to $resolvedName."
                _lastError.value = msg
                _connectionState.value = BleConnectionState.Error(msg, address)
            }
        } catch (e: SecurityException) {
            val msg = "Missing Bluetooth connect permission: ${e.message}"
            _lastError.value = msg
            _connectionState.value = BleConnectionState.Error(msg, address)
        } catch (e: Exception) {
            val msg = "Connection error: ${e.message}"
            _lastError.value = msg
            _connectionState.value = BleConnectionState.Error(msg, address)
        }
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        stopScan()
        failPendingOperations("Disconnected from ESP32")
        closeCurrentGatt()
        _connectionState.value = BleConnectionState.Disconnected
    }

    override suspend fun calibrateBlank(): Result<BleResponse.CalibrationResult> {
        val rawResult = sendCommandAndAwaitResponse(
            command = BleProtocol.CMD_CALIBRATE,
            timeoutMs = BleProtocol.CALIBRATE_TIMEOUT_MS,
        )
        return rawResult.fold(
            onSuccess = { response ->
                when (response) {
                    is BleResponse.CalibrationResult -> Result.success(response)
                    is BleResponse.ErrorResponse -> Result.failure(IllegalStateException(response.message))
                    is BleResponse.MeasurementResult -> Result.failure(
                        IllegalStateException("Expected calibration confirmation, received measurement payload.")
                    )
                }
            },
            onFailure = { Result.failure(it) },
        )
    }

    override suspend fun measureSample(): Result<BleResponse.MeasurementResult> {
        val rawResult = sendCommandAndAwaitResponse(
            command = BleProtocol.CMD_MEASURE,
            timeoutMs = BleProtocol.MEASURE_TIMEOUT_MS,
        )
        return rawResult.fold(
            onSuccess = { response ->
                when (response) {
                    is BleResponse.MeasurementResult -> Result.success(response)
                    is BleResponse.ErrorResponse -> Result.failure(IllegalStateException(response.message))
                    is BleResponse.CalibrationResult -> Result.failure(
                        IllegalStateException("Expected measurement result, received calibration payload.")
                    )
                }
            },
            onFailure = { Result.failure(it) },
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendCommandAndAwaitResponse(
        command: String,
        timeoutMs: Long,
    ): Result<BleResponse> = commandMutex.withLock {
        val gatt = activeGatt
        val txChar = writeCharacteristic
        if (_connectionState.value !is BleConnectionState.Connected || gatt == null || txChar == null) {
            return@withLock Result.failure(
                IllegalStateException("ESP32 is not connected. Connect over Bluetooth before sending commands.")
            )
        }
        if (!BlePermissionHelper.hasConnectPermission(context)) {
            return@withLock Result.failure(
                SecurityException("Bluetooth connect permission is not granted.")
            )
        }

        frameBuffer.clear()
        val responseDeferred = CompletableDeferred<Result<BleResponse>>()
        pendingResponseDeferred = responseDeferred

        val bytes = command.toByteArray(StandardCharsets.UTF_8)
        val maxPayload = (negotiatedMtu - 3).coerceAtLeast(20)
        val chunks = bytes.toList().chunked(maxPayload).map { it.toByteArray() }

        val writeType = if (
            (txChar.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 &&
            (txChar.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
        ) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        try {
            for (chunk in chunks) {
                val ackDeferred = CompletableDeferred<Boolean>()
                writeAckDeferred = ackDeferred

                val started = writeCharacteristicCompat(gatt, txChar, chunk, writeType)
                if (!started) {
                    writeAckDeferred = null
                    pendingResponseDeferred = null
                    return@withLock Result.failure(
                        IllegalStateException("Failed to write command '${command.trim()}' to ESP32 GATT.")
                    )
                }

                val acked = withTimeoutOrNull(4_000L) { ackDeferred.await() } ?: false
                writeAckDeferred = null
                if (!acked) {
                    pendingResponseDeferred = null
                    return@withLock Result.failure(
                        IllegalStateException("BLE write acknowledgment timed out while sending command.")
                    )
                }
            }

            val awaited = withTimeoutOrNull(timeoutMs) { responseDeferred.await() }
            pendingResponseDeferred = null
            return@withLock awaited ?: Result.failure(
                IllegalStateException(
                    "Timed out waiting for ESP32 response after ${timeoutMs / 1000}s. Verify firmware and sensor state."
                )
            )
        } catch (e: Exception) {
            writeAckDeferred = null
            pendingResponseDeferred = null
            return@withLock Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray,
        writeType: Int,
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, payload, writeType) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = payload
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeDescriptorCompat(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeCurrentGatt() {
        mtuFallbackJob?.cancel()
        mtuFallbackJob = null
        writeCharacteristic = null
        notifyCharacteristic = null
        val gatt = activeGatt
        activeGatt = null
        if (gatt != null) {
            try {
                if (BlePermissionHelper.hasConnectPermission(context)) {
                    gatt.disconnect()
                }
            } catch (_: Exception) {
            }
            try {
                gatt.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun failPendingOperations(reason: String) {
        writeAckDeferred?.complete(false)
        writeAckDeferred = null
        pendingResponseDeferred?.complete(Result.failure(IllegalStateException(reason)))
        pendingResponseDeferred = null
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String? {
        return try {
            if (BlePermissionHelper.hasConnectPermission(context)) device.name else null
        } catch (_: SecurityException) {
            null
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            val advertisedName = result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
            val cachedName = safeDeviceName(device)?.takeIf { it.isNotBlank() }
            val name = advertisedName ?: cachedName ?: "BLE Device (${address.takeLast(5)})"

            val serviceUuids: List<ParcelUuid> = result.scanRecord?.serviceUuids.orEmpty()
            val advertisesNus = serviceUuids.any { uuid ->
                uuid.uuid in BleProtocol.SUPPORTED_SERVICE_UUIDS
            } || name.contains("optimilk", ignoreCase = true) ||
                name.contains("esp32", ignoreCase = true)

            val entry = DiscoveredBleDevice(
                name = name,
                address = address,
                rssi = result.rssi,
                advertisesNus = advertisesNus,
                lastSeenMillis = System.currentTimeMillis(),
            )

            _discoveredDevices.update { current ->
                val mutable = current.toMutableList()
                val existingIdx = mutable.indexOfFirst { it.address == address }
                if (existingIdx >= 0) {
                    mutable[existingIdx] = entry
                } else {
                    mutable.add(entry)
                }
                mutable.sortedWith(
                    compareByDescending<DiscoveredBleDevice> { it.advertisesNus }
                        .thenByDescending { it.rssi }
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _lastError.value = "BLE scan failed (error code $errorCode). Ensure Bluetooth & Location services are enabled."
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt != activeGatt && activeGatt != null) return

            if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_CONNECTED) {
                failPendingOperations("BLE connection lost (GATT status $status)")
                closeCurrentGatt()
                val msg = when (status) {
                    133 -> "Connection failed (GATT 133). Move closer to the ESP32 and retry."
                    8 -> "Connection timed out (ESP32 out of range or powered off)."
                    19 -> "ESP32 terminated the Bluetooth connection."
                    else -> "Disconnected from ESP32 (GATT status $status)."
                }
                _lastError.value = msg
                _connectionState.value = BleConnectionState.Error(msg, pendingDeviceAddress)
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (!BlePermissionHelper.hasConnectPermission(context)) {
                        closeCurrentGatt()
                        _connectionState.value = BleConnectionState.Error("Missing BLUETOOTH_CONNECT permission")
                        return
                    }
                    val mtuRequested = try {
                        gatt.requestMtu(BleProtocol.REQUESTED_ATT_MTU)
                    } catch (_: Exception) {
                        false
                    }
                    if (!mtuRequested) {
                        gatt.discoverServices()
                    } else {
                        // Guard in case stack does not invoke onMtuChanged promptly
                        mtuFallbackJob?.cancel()
                        mtuFallbackJob = scope.launch {
                            delay(1_500L)
                            if (_connectionState.value is BleConnectionState.Connecting) {
                                try {
                                    if (BlePermissionHelper.hasConnectPermission(context)) {
                                        gatt.discoverServices()
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    failPendingOperations("ESP32 disconnected")
                    closeCurrentGatt()
                    _connectionState.value = BleConnectionState.Disconnected
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            mtuFallbackJob?.cancel()
            mtuFallbackJob = null
            if (status == BluetoothGatt.GATT_SUCCESS && mtu >= BleProtocol.DEFAULT_ATT_MTU) {
                negotiatedMtu = mtu
            }
            if (BlePermissionHelper.hasConnectPermission(context)) {
                gatt.discoverServices()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                val msg = "GATT service discovery failed (status $status)."
                _lastError.value = msg
                closeCurrentGatt()
                _connectionState.value = BleConnectionState.Error(msg, pendingDeviceAddress)
                return
            }

            val nusService = BleProtocol.SUPPORTED_SERVICE_UUIDS.firstNotNullOfOrNull { uuid ->
                gatt.getService(uuid)
            }
            if (nusService == null) {
                val msg = "Nordic UART Service (6E400001-...) not found on ${pendingDeviceName}."
                _lastError.value = msg
                closeCurrentGatt()
                _connectionState.value = BleConnectionState.Error(msg, pendingDeviceAddress)
                return
            }

            val txWrite = BleProtocol.SUPPORTED_WRITE_UUIDS.firstNotNullOfOrNull { uuid ->
                nusService.getCharacteristic(uuid)
            }
            val rxNotify = BleProtocol.SUPPORTED_NOTIFY_UUIDS.firstNotNullOfOrNull { uuid ->
                nusService.getCharacteristic(uuid)
            }

            if (txWrite == null || rxNotify == null) {
                val msg = "ESP32 UART characteristics (Write 6E400002 / Notify 6E400003) are missing."
                _lastError.value = msg
                closeCurrentGatt()
                _connectionState.value = BleConnectionState.Error(msg, pendingDeviceAddress)
                return
            }

            writeCharacteristic = txWrite
            notifyCharacteristic = rxNotify

            if (!BlePermissionHelper.hasConnectPermission(context)) {
                closeCurrentGatt()
                _connectionState.value = BleConnectionState.Error("Missing Bluetooth connect permission")
                return
            }

            val notifyEnabled = gatt.setCharacteristicNotification(rxNotify, true)
            if (!notifyEnabled) {
                val msg = "Failed to enable BLE notifications on ESP32."
                _lastError.value = msg
                closeCurrentGatt()
                _connectionState.value = BleConnectionState.Error(msg, pendingDeviceAddress)
                return
            }

            val cccd = rxNotify.getDescriptor(BleProtocol.CCCD_UUID)
            if (cccd != null) {
                val writeStarted = writeDescriptorCompat(
                    gatt,
                    cccd,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                )
                if (!writeStarted) {
                    val msg = "Failed to subscribe to ESP32 notification descriptor (CCCD)."
                    _lastError.value = msg
                    closeCurrentGatt()
                    _connectionState.value = BleConnectionState.Error(msg, pendingDeviceAddress)
                }
            } else {
                // Rare firmware without CCCD; proceed to Connected
                _connectionState.value = BleConnectionState.Connected(
                    deviceName = pendingDeviceName,
                    address = pendingDeviceAddress,
                    mtu = negotiatedMtu,
                )
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (descriptor.uuid == BleProtocol.CCCD_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _lastError.value = null
                    _connectionState.value = BleConnectionState.Connected(
                        deviceName = pendingDeviceName,
                        address = pendingDeviceAddress,
                        mtu = negotiatedMtu,
                    )
                } else {
                    val msg = "Failed to enable ESP32 notifications (CCCD status $status)."
                    _lastError.value = msg
                    closeCurrentGatt()
                    _connectionState.value = BleConnectionState.Error(msg, pendingDeviceAddress)
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            writeAckDeferred?.complete(status == BluetoothGatt.GATT_SUCCESS)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: return
                handleNotificationBytes(characteristic, value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleNotificationBytes(characteristic, value)
        }
    }

    private fun handleNotificationBytes(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        if (characteristic.uuid !in BleProtocol.SUPPORTED_NOTIFY_UUIDS) return
        val completeFrames = frameBuffer.appendChunk(value)
        for (frame in completeFrames) {
            val parsed = BleMessageParser.parse(frame)
            pendingResponseDeferred?.complete(parsed)
        }
    }
}

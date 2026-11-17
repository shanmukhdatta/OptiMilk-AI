package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ble.BleConnectionState
import com.example.ble.BleResponse
import com.example.ble.ColorimeterClient
import com.example.ble.DiscoveredBleDevice
import com.example.data.MeasurementRepository
import com.example.data.OptiMilkDatabase
import com.example.ui.CalibrationStatus
import com.example.ui.MeasurementStatus
import com.example.ui.OptiMilkUiState
import com.example.ui.OptiMilkViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.OptiMilkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OptiMilkViewModelAndStorageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: OptiMilkDatabase
    private lateinit var repository: MeasurementRepository
    private lateinit var fakeClient: FakeColorimeterClient

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OptiMilkDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = MeasurementRepository(database.measurementDao(), preferences = null)
        fakeClient = FakeColorimeterClient()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `measuring while disconnected reports error and does not fabricate readings`() = runTest(testDispatcher) {
        val vm = OptiMilkViewModel(fakeClient, repository)
        advanceUntilIdle()

        vm.measureSample()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(MeasurementStatus.ERROR, state.measurementStatus)
        assertNull(state.currentReading)
        assertTrue(state.measurementError?.contains("disconnected", ignoreCase = true) == true)
        assertEquals(0, fakeClient.measureCalls)
    }

    @Test
    fun `connected measurement receives real esp32 payload and saves to Room history`() = runTest(testDispatcher) {
        val vm = OptiMilkViewModel(fakeClient, repository)
        fakeClient.simulateConnected("OptiMilk-ESP32", "AA:BB:CC:DD:EE:FF")
        fakeClient.nextMeasurement = Result.success(
            BleResponse.MeasurementResult(
                r = 290,
                g = 772,
                b = 598,
                c = 1102,
                concentration = 3.45,
                unit = "%",
                status = "ok",
            )
        )
        advanceUntilIdle()

        vm.measureSample()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, fakeClient.measureCalls)
        assertEquals(MeasurementStatus.COMPLETE, state.measurementStatus)
        assertNotNull(state.currentReading)
        assertEquals(3.45, state.currentReading!!.concentration, 0.0001)
        assertEquals(290, state.currentReading!!.r)
        assertEquals(772, state.currentReading!!.g)
        assertEquals(598, state.currentReading!!.b)
        assertEquals(1102, state.currentReading!!.c)

        vm.updateEditableSampleName("Batch 07 Cow Milk")
        vm.saveCurrentReading()
        advanceUntilIdle()

        val stored = repository.allMeasurements.first()
        assertEquals(1, stored.size)
        assertEquals("Batch 07 Cow Milk", stored.first().sampleName)
        assertEquals(3.45, stored.first().concentration, 0.0001)
        assertEquals(false, stored.first().isDemo)

        vm.deleteHistoryItem(stored.first().id)
        advanceUntilIdle()
        assertTrue(repository.allMeasurements.first().isEmpty())
    }

    @Test
    fun `calibration acknowledgment and failure states are handled properly`() = runTest(testDispatcher) {
        val vm = OptiMilkViewModel(fakeClient, repository)
        fakeClient.simulateConnected("OptiMilk-ESP32", "AA:BB:CC:DD:EE:FF")
        advanceUntilIdle()

        fakeClient.nextCalibration = Result.success(BleResponse.CalibrationResult("ok", "Blank OK"))
        vm.calibrateBlank()
        advanceUntilIdle()

        assertEquals(1, fakeClient.calibrateCalls)
        assertEquals(CalibrationStatus.SUCCESS, vm.uiState.value.calibrationStatus)

        fakeClient.nextCalibration = Result.failure(IllegalStateException("Sensor saturation"))
        vm.calibrateBlank()
        advanceUntilIdle()

        assertEquals(CalibrationStatus.ERROR, vm.uiState.value.calibrationStatus)
        assertEquals("Sensor saturation", vm.uiState.value.calibrationMessage)
    }

    @Test
    fun `home screen renders OptiMilk-AI white and green dashboard controls`() {
        composeTestRule.setContent {
            OptiMilkTheme {
                HomeScreen(
                    uiState = OptiMilkUiState(
                        connectionState = BleConnectionState.Connected("OptiMilk-ESP32", "AA:BB:CC:11:22:33"),
                    ),
                    onOpenBluetoothScreen = {},
                    onDisconnect = {},
                    onMeasureSample = {},
                    onCalibrateBlank = {},
                    onSampleNameChange = {},
                    onSaveResult = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OptiMilk-AI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        composeTestRule.onNodeWithTag("measure_sample_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("calibrate_blank_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_red_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_green_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_blue_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("channel_clear_card").performScrollTo().assertIsDisplayed()
    }

    private class FakeColorimeterClient : ColorimeterClient {
        private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
        override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

        private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
        override val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

        private val _isScanning = MutableStateFlow(false)
        override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        override val lastError: StateFlow<String?> = _lastError.asStateFlow()

        var measureCalls = 0
        var calibrateCalls = 0
        var nextMeasurement: Result<BleResponse.MeasurementResult> = Result.failure(IllegalStateException("Unset"))
        var nextCalibration: Result<BleResponse.CalibrationResult> = Result.success(BleResponse.CalibrationResult("ok"))

        fun simulateConnected(name: String, address: String) {
            _connectionState.value = BleConnectionState.Connected(name, address)
        }

        override fun isBluetoothSupported(): Boolean = true
        override fun isBluetoothEnabled(): Boolean = true
        override fun hasRequiredPermissions(): Boolean = true
        override fun startScan() { _isScanning.value = true }
        override fun stopScan() { _isScanning.value = false }
        override fun connect(address: String, displayName: String?) {
            _connectionState.value = BleConnectionState.Connected(displayName ?: "ESP32", address)
        }
        override fun disconnect() {
            _connectionState.value = BleConnectionState.Disconnected
        }
        override fun clearError() { _lastError.value = null }

        override suspend fun calibrateBlank(): Result<BleResponse.CalibrationResult> {
            calibrateCalls++
            return nextCalibration
        }

        override suspend fun measureSample(): Result<BleResponse.MeasurementResult> {
            measureCalls++
            return nextMeasurement
        }
    }
}

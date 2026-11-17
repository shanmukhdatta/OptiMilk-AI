package com.example.ble

import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Isolated Demo Mode simulator for evaluating the OptiMilk-AI workflow without ESP32 hardware.
 * Demo readings are strictly tagged and never stored or displayed as real hardware measurements.
 */
object DemoColorimeterSimulator {
    private var isCalibrated = false

    suspend fun simulateCalibration(): Result<BleResponse.CalibrationResult> {
        delay(1_150L)
        isCalibrated = true
        return Result.success(
            BleResponse.CalibrationResult(
                status = "ok",
                message = "Demo blank cuvette baseline recorded",
            )
        )
    }

    suspend fun simulateMeasurement(): Result<BleResponse.MeasurementResult> {
        delay(1_450L)
        // Generate realistic simulated TCS34725 RGBC counts and corresponding concentration
        val baseConcentration = if (isCalibrated) {
            Random.nextDouble(2.6, 5.4)
        } else {
            Random.nextDouble(3.1, 4.6)
        }
        val roundedConc = (baseConcentration * 100.0).roundToInt() / 100.0

        val r = (240 + (roundedConc * 18) + Random.nextInt(-12, 13)).toInt().coerceIn(80, 1024)
        val g = (690 + (roundedConc * 22) + Random.nextInt(-16, 17)).toInt().coerceIn(120, 1400)
        val b = (520 + (roundedConc * 16) + Random.nextInt(-14, 15)).toInt().coerceIn(100, 1200)
        val c = ((r + g + b) * 0.68).toInt()

        return Result.success(
            BleResponse.MeasurementResult(
                r = r,
                g = g,
                b = b,
                c = c,
                concentration = roundedConc,
                unit = "%",
                status = "ok",
            )
        )
    }
}

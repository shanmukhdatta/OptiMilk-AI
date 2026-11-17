package com.example.ble

import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Nordic UART Service (NUS) BLE communication contract for the OptiMilk-AI ESP32 colorimeter.
 *
 * Standard 128-bit NUS UUIDs end with `-E50E24DCCA9E`. For maximum compatibility with firmware
 * that copies the 11-char suffix literally (`-E50E24DCCA9`), both variants are matched during
 * GATT service discovery, while standard `...DCCA9E` is preferred.
 */
object BleProtocol {
    const val NUS_SERVICE_UUID_STR = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
    const val NUS_WRITE_CHAR_UUID_STR = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
    const val NUS_NOTIFY_CHAR_UUID_STR = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

    // Fallback matching literal 11-hex-digit suffix if firmware used it verbatim
    const val ALT_SERVICE_UUID_STR = "6E400001-B5A3-F393-E0A9-E50E24DCCA9"
    const val ALT_WRITE_CHAR_UUID_STR = "6E400002-B5A3-F393-E0A9-E50E24DCCA9"
    const val ALT_NOTIFY_CHAR_UUID_STR = "6E400003-B5A3-F393-E0A9-E50E24DCCA9"

    val NUS_SERVICE_UUID: UUID = UUID.fromString(NUS_SERVICE_UUID_STR)
    val NUS_WRITE_CHAR_UUID: UUID = UUID.fromString(NUS_WRITE_CHAR_UUID_STR)
    val NUS_NOTIFY_CHAR_UUID: UUID = UUID.fromString(NUS_NOTIFY_CHAR_UUID_STR)

    val ALT_SERVICE_UUID: UUID = UUID.fromString(ALT_SERVICE_UUID_STR)
    val ALT_WRITE_CHAR_UUID: UUID = UUID.fromString(ALT_WRITE_CHAR_UUID_STR)
    val ALT_NOTIFY_CHAR_UUID: UUID = UUID.fromString(ALT_NOTIFY_CHAR_UUID_STR)

    val SUPPORTED_SERVICE_UUIDS: List<UUID> = listOf(NUS_SERVICE_UUID, ALT_SERVICE_UUID)
    val SUPPORTED_WRITE_UUIDS: List<UUID> = listOf(NUS_WRITE_CHAR_UUID, ALT_WRITE_CHAR_UUID)
    val SUPPORTED_NOTIFY_UUIDS: List<UUID> = listOf(NUS_NOTIFY_CHAR_UUID, ALT_NOTIFY_CHAR_UUID)

    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val CMD_MEASURE = "MEASURE\n"
    const val CMD_CALIBRATE = "CALIBRATE\n"

    const val MEASURE_TIMEOUT_MS = 18_000L
    const val CALIBRATE_TIMEOUT_MS = 14_000L
    const val SCAN_DURATION_MS = 12_000L
    const val DEFAULT_ATT_MTU = 23
    const val REQUESTED_ATT_MTU = 247
}

sealed interface BleResponse {
    data class MeasurementResult(
        val r: Int,
        val g: Int,
        val b: Int,
        val c: Int,
        val concentration: Double,
        val unit: String,
        val status: String,
    ) : BleResponse

    data class CalibrationResult(
        val status: String,
        val message: String? = null,
    ) : BleResponse

    data class ErrorResponse(
        val message: String,
        val rawJson: String? = null,
    ) : BleResponse
}

/**
 * Buffers fragmented UTF-8 BLE GATT notification chunks and extracts complete JSON frames.
 * Supports newline-delimited frames (`\n` or `\r\n`) as well as balanced `{...}` JSON objects
 * in case firmware omits a trailing newline.
 */
class IncomingFrameBuffer {
    private val buffer = StringBuilder()

    @Synchronized
    fun appendChunk(bytes: ByteArray): List<String> {
        if (bytes.isEmpty()) return emptyList()
        val chunk = String(bytes, StandardCharsets.UTF_8)
        buffer.append(chunk)

        val frames = mutableListOf<String>()

        // 1. Extract all newline-delimited lines first
        while (true) {
            val newlineIdx = buffer.indexOf("\n")
            if (newlineIdx == -1) break
            val line = buffer.substring(0, newlineIdx).trim()
            buffer.delete(0, newlineIdx + 1)
            if (line.isNotEmpty()) {
                frames.add(line)
            }
        }

        // 2. Fallback: if firmware sent a complete JSON object without trailing '\n'
        val remaining = buffer.toString().trimStart()
        if (remaining.startsWith("{")) {
            val completeEndIndex = findBalancedJsonEnd(buffer.toString())
            if (completeEndIndex != -1) {
                val jsonFrame = buffer.substring(0, completeEndIndex + 1).trim()
                buffer.delete(0, completeEndIndex + 1)
                if (jsonFrame.isNotEmpty()) {
                    frames.add(jsonFrame)
                }
            }
        }

        // Guard against unbounded garbage accumulation
        if (buffer.length > 8192) {
            buffer.clear()
        }

        return frames
    }

    @Synchronized
    fun clear() {
        buffer.clear()
    }

    private fun findBalancedJsonEnd(text: String): Int {
        var depth = 0
        var inString = false
        var escaped = false
        var started = false
        for (i in text.indices) {
            val ch = text[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\' && inString) {
                escaped = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (ch == '{') {
                    depth++
                    started = true
                } else if (ch == '}') {
                    depth--
                    if (started && depth == 0) {
                        return i
                    }
                }
            }
        }
        return -1
    }
}

object BleMessageParser {
    fun parse(rawPayload: String): Result<BleResponse> {
        val trimmed = rawPayload.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty response from ESP32"))
        }

        val json = try {
            JSONObject(trimmed)
        } catch (e: JSONException) {
            return Result.failure(
                IllegalArgumentException("Malformed JSON from ESP32: ${e.message}")
            )
        }

        val status = json.optString("status", "").trim().lowercase()
        val type = json.optString("type", "").trim().lowercase()

        if (status == "error" || type == "error") {
            val errMsg = json.optString("message", "").ifBlank {
                json.optString("error", "ESP32 reported a sensor or processing error")
            }
            return Result.success(BleResponse.ErrorResponse(message = errMsg, rawJson = trimmed))
        }

        return when (type) {
            "result" -> parseMeasurementResult(json, status)
            "calibration" -> parseCalibrationResult(json, status)
            "" -> Result.failure(IllegalArgumentException("Missing 'type' field in ESP32 response"))
            else -> Result.failure(IllegalArgumentException("Unsupported ESP32 response type: '$type'"))
        }
    }

    private fun parseMeasurementResult(json: JSONObject, status: String): Result<BleResponse> {
        if (status != "ok") {
            return Result.failure(
                IllegalArgumentException("Unexpected measurement status: '${status.ifBlank { "missing" }}'")
            )
        }
        val requiredNumericFields = listOf("r", "g", "b", "c", "concentration")
        for (field in requiredNumericFields) {
            if (!json.has(field) || json.isNull(field)) {
                return Result.failure(
                    IllegalArgumentException("Missing '$field' field in measurement result")
                )
            }
        }
        val unit = json.optString("unit", "").trim()
        if (unit.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Missing 'unit' field in measurement result")
            )
        }

        return try {
            val r = json.getInt("r")
            val g = json.getInt("g")
            val b = json.getInt("b")
            val c = json.getInt("c")
            val concentration = json.getDouble("concentration")
            if (r < 0 || g < 0 || b < 0 || c < 0) {
                return Result.failure(
                    IllegalArgumentException("Invalid negative optical sensor reading from ESP32")
                )
            }
            if (concentration.isNaN() || concentration.isInfinite()) {
                return Result.failure(
                    IllegalArgumentException("Invalid concentration value from ESP32")
                )
            }
            Result.success(
                BleResponse.MeasurementResult(
                    r = r,
                    g = g,
                    b = b,
                    c = c,
                    concentration = concentration,
                    unit = unit,
                    status = status,
                )
            )
        } catch (e: JSONException) {
            Result.failure(
                IllegalArgumentException("Invalid numeric value in measurement JSON: ${e.message}")
            )
        }
    }

    private fun parseCalibrationResult(json: JSONObject, status: String): Result<BleResponse> {
        if (status != "ok") {
            val msg = json.optString("message", "Calibration failed with status '$status'")
            return Result.success(BleResponse.ErrorResponse(message = msg))
        }
        val msg = json.optString("message", "").takeIf { it.isNotBlank() }
        return Result.success(BleResponse.CalibrationResult(status = status, message = msg))
    }
}

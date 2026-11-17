package com.example

import com.example.ble.BleMessageParser
import com.example.ble.BleProtocol
import com.example.ble.BleResponse
import com.example.ble.IncomingFrameBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BleProtocolTest {

    @Test
    fun `parses valid measurement result json`() {
        val payload = """
            {
              "type": "result",
              "r": 290,
              "g": 772,
              "b": 598,
              "c": 1102,
              "concentration": 3.45,
              "unit": "%",
              "status": "ok"
            }
        """.trimIndent()

        val result = BleMessageParser.parse(payload)
        assertTrue(result.isSuccess)
        val measurement = result.getOrThrow() as BleResponse.MeasurementResult
        assertEquals(290, measurement.r)
        assertEquals(772, measurement.g)
        assertEquals(598, measurement.b)
        assertEquals(1102, measurement.c)
        assertEquals(3.45, measurement.concentration, 0.0001)
        assertEquals("%", measurement.unit)
    }

    @Test
    fun `parses valid calibration acknowledgment json`() {
        val payload = """{"type":"calibration","status":"ok"}"""
        val result = BleMessageParser.parse(payload)
        assertTrue(result.isSuccess)
        val cal = result.getOrThrow() as BleResponse.CalibrationResult
        assertEquals("ok", cal.status)
    }

    @Test
    fun `handles device error response json`() {
        val payload = """{"type":"result","status":"error","message":"TCS34725 sensor timeout"}"""
        val result = BleMessageParser.parse(payload)
        assertTrue(result.isSuccess)
        val err = result.getOrThrow() as BleResponse.ErrorResponse
        assertEquals("TCS34725 sensor timeout", err.message)
    }

    @Test
    fun `rejects malformed json and missing required fields`() {
        assertTrue(BleMessageParser.parse("{not valid json").isFailure)
        assertTrue(
            BleMessageParser.parse("""{"type":"result","r":100,"g":200,"status":"ok"}""").isFailure
        )
        assertTrue(
            BleMessageParser.parse("""{"type":"result","r":-1,"g":200,"b":100,"c":300,"concentration":2.1,"unit":"%","status":"ok"}""").isFailure
        )
    }

    @Test
    fun `incoming frame buffer reassembles fragmented 20 byte BLE packets`() {
        val buffer = IncomingFrameBuffer()
        val fullJson = """{"type":"result","r":310,"g":780,"b":610,"c":1150,"concentration":3.82,"unit":"%","status":"ok"}""" + "\n"
        val chunks = fullJson.toByteArray(Charsets.UTF_8).toList().chunked(20).map { it.toByteArray() }

        val collected = mutableListOf<String>()
        for (chunk in chunks) {
            collected.addAll(buffer.appendChunk(chunk))
        }

        assertEquals(1, collected.size)
        val parsed = BleMessageParser.parse(collected.first()).getOrThrow() as BleResponse.MeasurementResult
        assertEquals(3.82, parsed.concentration, 0.0001)
    }

    @Test
    fun `incoming frame buffer handles balanced json without trailing newline`() {
        val buffer = IncomingFrameBuffer()
        val part1 = """{"type":"calibration","""
        val part2 = """"status":"ok"}"""
        assertTrue(buffer.appendChunk(part1.toByteArray()).isEmpty())
        val frames = buffer.appendChunk(part2.toByteArray())
        assertEquals(1, frames.size)
        assertTrue(BleMessageParser.parse(frames.first()).getOrThrow() is BleResponse.CalibrationResult)
    }

    @Test
    fun `nordic uart command strings end with newline`() {
        assertEquals("MEASURE\n", BleProtocol.CMD_MEASURE)
        assertEquals("CALIBRATE\n", BleProtocol.CMD_CALIBRATE)
    }
}

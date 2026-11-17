# OptiMilk-AI

**OptiMilk-AI** is an intelligent, low-cost optical milk colorimeter system designed for real-time milk quality evaluation, fat/concentration estimation, and adulteration screening. The platform pairs an ESP32 microcontroller and a TCS34725 RGB color sensor with an edge linear regression model and a native Android companion application.

---

## Repository Architecture

- **`app/`**: Native Android companion application built with Kotlin, Jetpack Compose (Material 3), Room DB, and BLE GATT NUS client.
  - [Read dedicated Android App Documentation](app/README.md)
  - Prebuilt debug APK: [`app/OptiMilk-AI-debug.apk`](app/OptiMilk-AI-debug.apk)
- **`gradle/`**, **`build.gradle.kts`**, **`settings.gradle.kts`**, **`gradle.properties`**: Modern Gradle build configuration with Version Catalogs.
- **`.github/workflows/android-build.yml`**: CI/CD pipeline running unit/Robolectric tests, assembling debug APKs, and uploading build artifacts.

---

## OptiMilk-AI Android Companion App

Native Kotlin + Jetpack Compose (Material 3) Android companion app for the **OptiMilk-AI** ESP32 + TCS34725 RGB optical milk colorimeter.

### Key Features

- **Scientific Laboratory Dashboard**: Clean UI displaying ESP32 connection status, predicted milk concentration, measurement states (`Ready`, `Measuring`, `Complete`, `Error`), custom sample naming, timestamps, and live R/G/B/Clear sensor channel readings.
- **Bluetooth Low Energy (BLE) GATT Client**: Uses Android `BluetoothLeScanner` and `BluetoothGatt` over Nordic UART Service (NUS) with runtime permissions for Android 12+ (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`) and legacy devices (`ACCESS_FINE_LOCATION`).
- **Calibration & Measurement Workflows**: Sends newline-delimited `CALIBRATE\n` and `MEASURE\n` UTF-8 commands, handles fragmented GATT notification chunks, validates JSON payloads, enforces single-in-flight command execution, and provides error handling.
- **Local Room Storage**: Stores confirmed readings (sample name, timestamp, R/G/B/C channels, predicted concentration, unit) locally via Room + SQLite with inspection and deletion.
- **Isolated Demo Mode**: Built-in simulated colorimeter mode in Settings, badged with `DEMO` to test UI and data workflows without physical hardware.

---

## ESP32 BLE GATT Protocol (Nordic UART Service)

| Item | UUID / Format | Description |
| --- | --- | --- |
| **Service UUID** | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` | Primary Nordic UART Service (NUS) UUID |
| **Write Characteristic (RX)** | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` | UTF-8 commands from Android -> ESP32 |
| **Notify Characteristic (TX)** | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` | CCCD (`0x2902`) enabled; JSON responses ESP32 -> Android |

### Commands (Android -> ESP32)

- `CALIBRATE\n` — Acquire blank cuvette reference baseline.
- `MEASURE\n` — Cycle R/G/B LEDs, read TCS34725 channels, execute linear regression on ESP32, and return JSON result.

### Expected JSON Responses (ESP32 -> Android)

Measurement response (newline-terminated UTF-8 JSON):
```json
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
```

Calibration response:
```json
{
  "type": "calibration",
  "status": "ok"
}
```

Error response:
```json
{
  "type": "result",
  "status": "error",
  "message": "TCS34725 sensor read failed"
}
```

---

## Build & Install APK

### Prebuilt Debug APK

The prebuilt, verified debug APK is tracked and available directly in the repository:
- **Path**: [`app/OptiMilk-AI-debug.apk`](app/OptiMilk-AI-debug.apk)

### Install via ADB

```bash
adb install -r app/OptiMilk-AI-debug.apk
```
Package ID: `com.optimilk.ai`

### Local Build Instructions

Requirements: JDK 17, Android SDK (API 34/35), Gradle 8.13+.

1. Run unit & Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
2. Build the debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
3. Built APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub Actions CI/CD

Automated pipeline configured in `.github/workflows/android-build.yml`:
- Runs tests via `:app:testDebugUnitTest`
- Assembles APK via `:app:assembleDebug`
- Uploads `OptiMilk-AI-debug.apk` as a workflow artifact on each push to `main`/`master`.

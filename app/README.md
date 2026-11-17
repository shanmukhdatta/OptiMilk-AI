# OptiMilk-AI Android Colorimeter Companion

Native Kotlin + Jetpack Compose (Material 3) Android companion app for the **OptiMilk-AI** ESP32 + TCS34725 RGB optical milk colorimeter.

## Features

- **White & Green Light Scientific Dashboard**: Clean laboratory UI displaying ESP32 connection status, predicted milk concentration, measurement status (`Ready`, `Measuring`, `Complete`, `Error`), sample naming, timestamps, and live R/G/B/Clear sensor channel readings.
- **Real Bluetooth Low Energy (BLE) GATT Client**: Uses Android `BluetoothLeScanner` and `BluetoothGatt` over the Nordic UART Service (NUS) with Android 12+ (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`) and legacy (`ACCESS_FINE_LOCATION`) runtime permissions.
- **Blank Calibration & Sample Measurement**: Sends newline-delimited `CALIBRATE\n` and `MEASURE\n` UTF-8 commands, handles fragmented GATT notification chunks, validates JSON payloads, enforces single-in-flight command execution, and surfaces timeouts/errors cleanly.
- **Local Room History**: Stores confirmed readings (sample name, date/time, R/G/B/C channels, predicted concentration, unit) locally via Room + SQLite with detail inspection and deletion.
- **Isolated Demo Mode**: Optional simulated mode (in **Settings**) clearly badged `DEMO` everywhere and separated from real ESP32 hardware measurements.

## ESP32 BLE GATT Protocol (Nordic UART Service)

| Item | UUID / Format | Notes |
| --- | --- | --- |
| **Service UUID** | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` | Also accepts `6E400001-B5A3-F393-E0A9-E50E24DCCA9` fallback |
| **Write Characteristic (RX)** | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` | UTF-8 commands from Android -> ESP32 |
| **Notify Characteristic (TX)** | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` | CCCD (`0x2902`) enabled; JSON responses ESP32 -> Android |

### Commands (Android -> ESP32)

- `CALIBRATE\n` — Acquire blank cuvette reference baseline.
- `MEASURE\n` — Cycle R/G/B LEDs, read TCS34725 channels, run linear regression on ESP32, and return JSON result.

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

## Build & Install APK

### Build Locally

1. Open the project in Android Studio (or ensure Android SDK & JDK 17 are configured).
2. Run unit & Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. Build the debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
4. The prebuilt debug APK is copied to:
   `app/OptiMilk-AI-debug.apk`

### Install on an Android Phone

- Copy `app/OptiMilk-AI-debug.apk` to your Android phone and tap it to install (enable *Install unknown apps* if prompted), **or** install via USB debugging:
  ```bash
  adb install -r app/OptiMilk-AI-debug.apk
  ```
- Package ID: `com.optimilk.ai`

### GitHub Actions Automated Build

A workflow at `.github/workflows/android-build.yml` runs `:app:testDebugUnitTest`, builds `:app:assembleDebug`, copies the output to `app/OptiMilk-AI-debug.apk`, and uploads `OptiMilk-AI-debug-apk` as a downloadable artifact.

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.CalibrationStatus
import com.example.ui.MeasurementStatus
import com.example.ui.OptiMilkFormatters
import com.example.ui.OptiMilkUiState
import com.example.ui.components.Esp32ConnectionCard
import com.example.ui.components.MeasurementResultCard
import com.example.ui.components.OpticalChannelsRow
import com.example.ui.components.OptiMilkBrandHeader
import com.example.ui.theme.BorderSage
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorBorder
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorSoftBg
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceMintTint
import com.example.ui.theme.TextPrimaryInk
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningBorder
import com.example.ui.theme.WarningSoftBg

@Composable
fun HomeScreen(
    uiState: OptiMilkUiState,
    onOpenBluetoothScreen: () -> Unit,
    onDisconnect: () -> Unit,
    onMeasureSample: () -> Unit,
    onCalibrateBlank: () -> Unit,
    onSampleNameChange: (String) -> Unit,
    onSaveResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. OptiMilk-AI Logo & Branding Header
            OptiMilkBrandHeader(demoModeEnabled = uiState.demoModeEnabled)

            // Demo Mode warning notice when hardware is disconnected
            AnimatedVisibility(visible = uiState.demoModeEnabled && !uiState.isHardwareConnected) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WarningSoftBg,
                    border = BorderStroke(1.dp, WarningBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Demo Mode is active. Readings below are simulated for UI testing and are never stored as real ESP32 hardware measurements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryInk,
                        )
                    }
                }
            }

            // 2. Modern ESP32 Bluetooth Connection Card
            Esp32ConnectionCard(
                connectionState = uiState.connectionState,
                demoModeEnabled = uiState.demoModeEnabled,
                onOpenBluetoothScreen = onOpenBluetoothScreen,
                onDisconnect = onDisconnect,
            )

            // 3. Large Premium Measurement Result Card (with status, sample name, timestamp)
            MeasurementResultCard(
                measurementStatus = uiState.measurementStatus,
                currentReading = uiState.currentReading,
                lastSuccessfulTimestamp = uiState.lastSuccessfulMeasurementTimestamp,
                measurementError = uiState.measurementError,
                editableSampleName = uiState.editableSampleName,
                onSampleNameChange = onSampleNameChange,
                onSaveResult = onSaveResult,
            )

            // 4 & 5. Primary Actions: Green Measure Sample + Outlined Calibrate Blank
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onMeasureSample,
                    enabled = !uiState.isBusy,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = PureWhite,
                        disabledContainerColor = EmeraldPrimary.copy(alpha = 0.45f),
                        disabledContentColor = PureWhite,
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 0.dp,
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("measure_sample_button"),
                ) {
                    if (uiState.measurementStatus == MeasurementStatus.MEASURING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PureWhite,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Measuring Sample...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Measure Sample",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                OutlinedButton(
                    onClick = onCalibrateBlank,
                    enabled = !uiState.isBusy,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.25.dp,
                        color = if (uiState.isBusy) BorderSage else EmeraldPrimary,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PureWhite,
                        contentColor = EmeraldPrimary,
                        disabledContentColor = TextSecondarySlate,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("calibrate_blank_button"),
                ) {
                    if (uiState.calibrationStatus == CalibrationStatus.CALIBRATING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = EmeraldPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calibrating Blank...",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calibrate Blank",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // Calibration Status Feedback Banner
            AnimatedVisibility(visible = uiState.calibrationStatus != CalibrationStatus.IDLE) {
                val isError = uiState.calibrationStatus == CalibrationStatus.ERROR
                val isSuccess = uiState.calibrationStatus == CalibrationStatus.SUCCESS
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        isError -> ErrorSoftBg
                        isSuccess -> MintLight
                        else -> SurfaceMintTint
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isError -> ErrorBorder
                            isSuccess -> MintBorder
                            else -> BorderSage
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calibration_status_banner"),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isError) ErrorCrimson else EmeraldPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.calibrationMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isError) ErrorCrimson else EmeraldDark,
                                fontWeight = FontWeight.Medium,
                            )
                            uiState.lastCalibrationTimestamp?.let { ts ->
                                if (isSuccess) {
                                    Text(
                                        text = "Calibrated at ${OptiMilkFormatters.formatShortTime(ts)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondarySlate,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Red, Green, Blue & Clear Optical Sensor Channel Cards
            OpticalChannelsRow(reading = uiState.currentReading)

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

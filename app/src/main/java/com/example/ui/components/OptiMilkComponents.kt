package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ble.BleConnectionState
import com.example.ui.LiveReading
import com.example.ui.MeasurementStatus
import com.example.ui.OptiMilkFormatters
import com.example.ui.theme.BorderSage
import com.example.ui.theme.ChannelBlueAccent
import com.example.ui.theme.ChannelBlueBg
import com.example.ui.theme.ChannelClearAccent
import com.example.ui.theme.ChannelClearBg
import com.example.ui.theme.ChannelGreenAccent
import com.example.ui.theme.ChannelGreenBg
import com.example.ui.theme.ChannelRedAccent
import com.example.ui.theme.ChannelRedBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorBorder
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorSoftBg
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceMintTint
import com.example.ui.theme.TextPrimaryInk
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningBorder
import com.example.ui.theme.WarningSoftBg

@Composable
fun OptiMilkBrandHeader(
    demoModeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderSage),
                shadowElevation = 2.dp,
                modifier = Modifier.size(48.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_optimilk_logo),
                    contentDescription = "OptiMilk-AI logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(4.dp),
                )
            }
            Column {
                Text(
                    text = "OptiMilk-AI",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryInk,
                )
                Text(
                    text = "Optical Milk Colorimeter",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySlate,
                )
            }
        }

        if (demoModeEnabled) {
            Surface(
                shape = RoundedCornerShape(50),
                color = WarningSoftBg,
                border = BorderStroke(1.dp, WarningBorder),
                modifier = Modifier.testTag("demo_mode_badge"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Science,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "DEMO MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun Esp32ConnectionCard(
    connectionState: BleConnectionState,
    demoModeEnabled: Boolean,
    onOpenBluetoothScreen: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = connectionState is BleConnectionState.Connected
    val isConnecting = connectionState is BleConnectionState.Connecting

    val cardBg by animateColorAsState(
        targetValue = if (isConnected) SurfaceMintTint else PureWhite,
        animationSpec = tween(250),
        label = "connectionCardBg",
    )
    val cardBorder = if (isConnected) MintBorder else BorderSage

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isConnected) 2.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isConnected) MintLight else SurfaceMintTint)
                            .border(1.dp, if (isConnected) MintBorder else BorderSage, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when {
                                isConnected -> Icons.Filled.BluetoothConnected
                                isConnecting -> Icons.Filled.Bluetooth
                                else -> Icons.Filled.BluetoothDisabled
                            },
                            contentDescription = "ESP32 Bluetooth status",
                            tint = if (isConnected || isConnecting) EmeraldPrimary else TextSecondarySlate,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (connectionState) {
                                is BleConnectionState.Connected -> connectionState.deviceName
                                is BleConnectionState.Connecting -> "Connecting to ${connectionState.deviceName}..."
                                is BleConnectionState.Error -> "ESP32 Connection Issue"
                                BleConnectionState.Disconnected -> "ESP32 Colorimeter"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = when (connectionState) {
                                is BleConnectionState.Connected ->
                                    "BLE UART • ${connectionState.address} • MTU ${connectionState.mtu}"
                                is BleConnectionState.Connecting ->
                                    "Discovering Nordic UART Service..."
                                is BleConnectionState.Error ->
                                    connectionState.message
                                BleConnectionState.Disconnected ->
                                    if (demoModeEnabled) {
                                        "Hardware offline • Demo Mode active"
                                    } else {
                                        "Ready to pair via Nordic UART BLE"
                                    }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (connectionState is BleConnectionState.Error) ErrorCrimson else TextSecondarySlate,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                ConnectionBadge(connectionState = connectionState)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("disconnect_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSage),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PureWhite,
                            contentColor = TextPrimaryInk,
                        ),
                    ) {
                        Text("Disconnect", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onOpenBluetoothScreen,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("manage_ble_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MintBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MintLight,
                            contentColor = EmeraldDark,
                        ),
                    ) {
                        Text("BLE Devices", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = onOpenBluetoothScreen,
                        enabled = !isConnecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("connect_esp32_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (demoModeEnabled) SurfaceMintTint else EmeraldPrimary,
                            contentColor = if (demoModeEnabled) EmeraldDark else PureWhite,
                        ),
                        border = if (demoModeEnabled) BorderStroke(1.dp, MintBorder) else null,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = PureWhite,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Bluetooth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect ESP32", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionBadge(connectionState: BleConnectionState) {
    val isConnected = connectionState is BleConnectionState.Connected
    val isConnecting = connectionState is BleConnectionState.Connecting
    val isError = connectionState is BleConnectionState.Error

    val bg = when {
        isConnected -> MintLight
        isError -> ErrorSoftBg
        else -> SurfaceMintTint
    }
    val fg = when {
        isConnected -> EmeraldDark
        isError -> ErrorCrimson
        else -> TextSecondarySlate
    }
    val border = when {
        isConnected -> MintBorder
        isError -> ErrorBorder
        else -> BorderSage
    }
    val label = when {
        isConnected -> "Connected"
        isConnecting -> "Connecting"
        isError -> "Error"
        else -> "Disconnected"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "connDotPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.testTag("connection_status_pill"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(if (isConnected || isConnecting) pulseAlpha else 1f)
                    .clip(CircleShape)
                    .background(
                        when {
                            isConnected -> EmeraldPrimary
                            isConnecting -> EmeraldPrimary
                            isError -> ErrorCrimson
                            else -> TextSecondarySlate
                        }
                    ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun MeasurementResultCard(
    measurementStatus: MeasurementStatus,
    currentReading: LiveReading?,
    lastSuccessfulTimestamp: Long?,
    measurementError: String?,
    editableSampleName: String,
    onSampleNameChange: (String) -> Unit,
    onSaveResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMeasuring = measurementStatus == MeasurementStatus.MEASURING

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PureWhite,
        border = BorderStroke(
            width = 1.dp,
            color = when (measurementStatus) {
                MeasurementStatus.ERROR -> ErrorBorder
                MeasurementStatus.COMPLETE -> MintBorder
                else -> BorderSage
            },
        ),
        shadowElevation = 3.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurfaceMintTint, PureWhite),
                    )
                )
                .padding(20.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header Row: Title + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "PREDICTED CONCENTRATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondarySlate,
                            letterSpacing = 0.8.sp,
                        )
                        if (currentReading?.isDemo == true) {
                            Text(
                                text = "Simulated Demo Reading",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarningAmber,
                            )
                        }
                    }

                    MeasurementStatusPill(status = measurementStatus)
                }

                // Main Concentration Hero Display
                if (isMeasuring) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(34.dp),
                            color = EmeraldPrimary,
                            strokeWidth = 3.5.dp,
                        )
                        Column {
                            Text(
                                text = "Acquiring optical channels...",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryInk,
                            )
                            Text(
                                text = "ESP32 cycling R/G/B LEDs & running regression model",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySlate,
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("concentration_value"),
                    ) {
                        Text(
                            text = if (currentReading != null) {
                                OptiMilkFormatters.formatConcentration(currentReading.concentration)
                            } else {
                                "--.--"
                            },
                            style = MaterialTheme.typography.displayLarge,
                            color = if (currentReading != null) EmeraldDark else TextSecondarySlate.copy(alpha = 0.5f),
                        )
                        Text(
                            text = currentReading?.unit ?: "%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (currentReading != null) EmeraldPrimary else TextSecondarySlate.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }

                // Error Banner if measurement failed
                if (measurementStatus == MeasurementStatus.ERROR && !measurementError.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ErrorSoftBg,
                        border = BorderStroke(1.dp, ErrorBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("measurement_error_banner"),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Measurement error",
                                tint = ErrorCrimson,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = measurementError,
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorCrimson,
                            )
                        }
                    }
                }

                // Metadata: Sample name & timestamp
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceMintTint,
                    border = BorderStroke(1.dp, BorderSage),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Sample",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySlate,
                            )
                            Text(
                                text = currentReading?.sampleName ?: "No sample measured yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimaryInk,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Last Measurement",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySlate,
                            )
                            val displayTimestamp = currentReading?.timestampMillis ?: lastSuccessfulTimestamp
                            Text(
                                text = displayTimestamp?.let { OptiMilkFormatters.formatDateTime(it) }
                                    ?: "Awaiting first reading",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                color = TextPrimaryInk,
                            )
                        }
                    }
                }

                // Save Result Section when an unsaved measurement is present
                if (currentReading != null && !isMeasuring) {
                    if (currentReading.isSaved) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MintLight,
                            border = BorderStroke(1.dp, MintBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("saved_confirmation_badge"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BookmarkAdded,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = if (currentReading.isDemo) {
                                        "Saved '${currentReading.sampleName}' to local History (DEMO)"
                                    } else {
                                        "Saved '${currentReading.sampleName}' to local History"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = editableSampleName,
                                onValueChange = onSampleNameChange,
                                singleLine = true,
                                label = { Text("Sample Name") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = BorderSage,
                                    focusedLabelColor = EmeraldPrimary,
                                    unfocusedContainerColor = PureWhite,
                                    focusedContainerColor = PureWhite,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sample_name_input"),
                            )
                            Button(
                                onClick = onSaveResult,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = PureWhite,
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .padding(top = 4.dp)
                                    .testTag("save_result_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementStatusPill(status: MeasurementStatus) {
    val (bg, fg, border) = when (status) {
        MeasurementStatus.READY -> Triple(SurfaceMintTint, TextSecondarySlate, BorderSage)
        MeasurementStatus.MEASURING -> Triple(MintLight, EmeraldDark, MintBorder)
        MeasurementStatus.COMPLETE -> Triple(MintLight, EmeraldPrimary, MintBorder)
        MeasurementStatus.ERROR -> Triple(ErrorSoftBg, ErrorCrimson, ErrorBorder)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.testTag("measurement_status_pill"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (status == MeasurementStatus.COMPLETE) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun OpticalChannelsRow(
    reading: LiveReading?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "TCS34725 Optical Sensor Channels",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimaryInk,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChannelCard(
                label = "RED",
                value = reading?.r?.toString() ?: "—",
                accent = ChannelRedAccent,
                bgTint = ChannelRedBg,
                modifier = Modifier.weight(1f).testTag("channel_red_card"),
            )
            ChannelCard(
                label = "GREEN",
                value = reading?.g?.toString() ?: "—",
                accent = ChannelGreenAccent,
                bgTint = ChannelGreenBg,
                modifier = Modifier.weight(1f).testTag("channel_green_card"),
            )
            ChannelCard(
                label = "BLUE",
                value = reading?.b?.toString() ?: "—",
                accent = ChannelBlueAccent,
                bgTint = ChannelBlueBg,
                modifier = Modifier.weight(1f).testTag("channel_blue_card"),
            )
            ChannelCard(
                label = "CLEAR",
                value = reading?.c?.toString() ?: "—",
                accent = ChannelClearAccent,
                bgTint = ChannelClearBg,
                isClear = true,
                modifier = Modifier.weight(1f).testTag("channel_clear_card"),
            )
        }
    }
}

@Composable
private fun ChannelCard(
    label: String,
    value: String,
    accent: Color,
    bgTint: Color,
    modifier: Modifier = Modifier,
    isClear: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderSage),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (isClear) {
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(12.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondarySlate,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgTint)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (value == "—") TextSecondarySlate.copy(alpha = 0.5f) else TextPrimaryInk,
                    maxLines = 1,
                )
            }
        }
    }
}

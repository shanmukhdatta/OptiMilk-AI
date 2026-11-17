package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ble.BleProtocol
import com.example.ui.OptiMilkUiState
import com.example.ui.theme.BorderSage
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorBorder
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.MintBorder
import com.example.ui.theme.MintLight
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceMintTint
import com.example.ui.theme.TextPrimaryInk
import com.example.ui.theme.TextSecondarySlate
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    uiState: OptiMilkUiState,
    onToggleDemoMode: (Boolean) -> Unit,
    onUpdateSamplePrefix: (String) -> Unit,
    onClearDemoHistory: () -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var prefixInput by remember(uiState.defaultSamplePrefix) {
        mutableStateOf(uiState.defaultSamplePrefix)
    }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Instrument Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimaryInk,
                    )
                    Text(
                        text = "Configure Demo Mode, sample naming, and BLE firmware protocol",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySlate,
                    )
                }
            }

            // Demo Mode Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, if (uiState.demoModeEnabled) MintBorder else BorderSage),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Science,
                                    contentDescription = null,
                                    tint = if (uiState.demoModeEnabled) WarningAmber else EmeraldPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Column {
                                    Text(
                                        text = "Demo Mode",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimaryInk,
                                    )
                                    Text(
                                        text = if (uiState.demoModeEnabled) "Active (Simulated ESP32)" else "Off (Real Hardware Only)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (uiState.demoModeEnabled) WarningAmber else TextSecondarySlate,
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.demoModeEnabled,
                                onCheckedChange = onToggleDemoMode,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = TextSecondarySlate,
                                    uncheckedTrackColor = SurfaceMintTint,
                                    uncheckedBorderColor = BorderSage,
                                ),
                                modifier = Modifier.testTag("demo_mode_switch"),
                            )
                        }
                        Text(
                            text = "Test calibration, measurement, and history storage without physical hardware. Demo readings are always clearly labeled DEMO and kept separate from ESP32 hardware readings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySlate,
                        )
                    }
                }
            }

            // Sample Naming Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, BorderSage),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Default Sample Naming",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryInk,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = prefixInput,
                                onValueChange = { prefixInput = it },
                                singleLine = true,
                                label = { Text("Sample Prefix") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = BorderSage,
                                    focusedLabelColor = EmeraldPrimary,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = { onUpdateSamplePrefix(prefixInput) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = PureWhite,
                                ),
                                modifier = Modifier.height(54.dp).padding(top = 4.dp),
                            ) {
                                Text("Apply")
                            }
                        }
                    }
                }
            }

            // ESP32 Nordic UART BLE Protocol Reference Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceMintTint,
                    border = BorderStroke(1.dp, BorderSage),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bluetooth,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "ESP32 Nordic UART BLE Contract",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryInk,
                            )
                        }

                        ProtocolSpecRow("Service UUID", BleProtocol.NUS_SERVICE_UUID_STR)
                        ProtocolSpecRow("Write Char", BleProtocol.NUS_WRITE_CHAR_UUID_STR)
                        ProtocolSpecRow("Notify Char", BleProtocol.NUS_NOTIFY_CHAR_UUID_STR)
                        ProtocolSpecRow("Commands", "CALIBRATE\\n  |  MEASURE\\n")

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PureWhite,
                            border = BorderStroke(1.dp, BorderSage),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "{\n  \"type\": \"result\",\n  \"r\": 290, \"g\": 772, \"b\": 598, \"c\": 1102,\n  \"concentration\": 3.45,\n  \"unit\": \"%\",\n  \"status\": \"ok\"\n}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                                color = EmeraldDark,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }

            // Local Data Management
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, BorderSage),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Local Storage Management",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryInk,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onClearDemoHistory,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderSage),
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Text("Clear Demo Data", color = TextPrimaryInk)
                            }
                            OutlinedButton(
                                onClick = { showClearAllDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ErrorBorder),
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = null,
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear All", color = ErrorCrimson)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = PureWhite,
            title = { Text("Clear All History?", color = TextPrimaryInk) },
            text = {
                Text(
                    "This will permanently delete all saved ESP32 and Demo readings from this phone.",
                    color = TextSecondarySlate,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllHistory()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorCrimson,
                        contentColor = PureWhite,
                    ),
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearAllDialog = false },
                    border = BorderStroke(1.dp, BorderSage),
                ) {
                    Text("Cancel", color = TextPrimaryInk)
                }
            },
        )
    }
}

@Composable
private fun ProtocolSpecRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondarySlate,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
            color = TextPrimaryInk,
        )
    }
}

package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ble.BleConnectionState
import com.example.ble.BlePermissionHelper
import com.example.ble.DiscoveredBleDevice
import com.example.ui.OptiMilkUiState
import com.example.ui.components.ConnectionBadge
import com.example.ui.theme.BorderSage
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    uiState: OptiMilkUiState,
    onRefreshCapabilities: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (DiscoveredBleDevice) -> Unit,
    onReconnectLast: () -> Unit,
    onDisconnect: () -> Unit,
    onClearError: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var permissionsDeclined by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        onRefreshCapabilities()
        val allGranted = grants.values.all { it }
        permissionsDeclined = !allGranted
        if (allGranted && uiState.isBluetoothEnabled) {
            onStartScan()
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        onRefreshCapabilities()
    }

    LaunchedEffect(Unit) {
        onRefreshCapabilities()
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopScan()
        }
    }

    BackHandler {
        onStopScan()
        onNavigateBack()
    }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ESP32 Bluetooth",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimaryInk,
                        )
                        Text(
                            text = "Nordic UART BLE Connection",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondarySlate,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onStopScan()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("bluetooth_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = TextPrimaryInk,
                        )
                    }
                },
                actions = {
                    ConnectionBadge(connectionState = uiState.connectionState)
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureWhite,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Active Connection / Reconnect Status Card
                item {
                    ActiveBleSessionCard(
                        connectionState = uiState.connectionState,
                        lastDeviceName = uiState.lastDeviceName,
                        lastDeviceAddress = uiState.lastDeviceAddress,
                        onDisconnect = onDisconnect,
                        onReconnectLast = onReconnectLast,
                    )
                }

                // Hardware / Bluetooth Disabled / Permissions Guidance
                if (!uiState.isBluetoothSupported) {
                    item {
                        BleNoticeCard(
                            title = "Bluetooth LE Unavailable",
                            message = "This device or emulator does not expose a Bluetooth Low Energy adapter. You can still test the full interface using Demo Mode in Settings.",
                            isError = false,
                        )
                    }
                } else {
                    if (!uiState.hasBluetoothPermissions) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = WarningSoftBg,
                                border = BorderStroke(1.dp, WarningBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = "Bluetooth Permissions Required",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimaryInk,
                                    )
                                    Text(
                                        text = "OptiMilk-AI needs nearby Bluetooth access to scan for and connect to your ESP32 colorimeter.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondarySlate,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                permissionLauncher.launch(BlePermissionHelper.requiredPermissions())
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldPrimary,
                                                contentColor = PureWhite,
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(48.dp).testTag("grant_ble_permissions_button"),
                                        ) {
                                            Text("Grant Permissions")
                                        }
                                        if (permissionsDeclined) {
                                            OutlinedButton(
                                                onClick = {
                                                    val intent = Intent(
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.fromParts("package", context.packageName, null),
                                                    )
                                                    context.startActivity(intent)
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.height(48.dp),
                                            ) {
                                                Text("App Settings")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!uiState.isBluetoothEnabled) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ErrorSoftBg,
                                border = BorderStroke(1.dp, ErrorBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = "Bluetooth Is Turned Off",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ErrorCrimson,
                                    )
                                    Text(
                                        text = "Enable Bluetooth on your phone to discover and communicate with the ESP32 colorimeter.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimaryInk,
                                    )
                                    Button(
                                        onClick = {
                                            if (BlePermissionHelper.hasConnectPermission(context)) {
                                                enableBluetoothLauncher.launch(
                                                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                                )
                                            } else {
                                                permissionLauncher.launch(BlePermissionHelper.requiredPermissions())
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary,
                                            contentColor = PureWhite,
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp).testTag("enable_bluetooth_button"),
                                    ) {
                                        Text("Turn On Bluetooth")
                                    }
                                }
                            }
                        }
                    }
                }

                // Error Banner
                uiState.bleErrorMessage?.let { errorMsg ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ErrorSoftBg,
                            border = BorderStroke(1.dp, ErrorBorder),
                            modifier = Modifier.fillMaxWidth().testTag("ble_error_banner"),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorCrimson,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedButton(
                                    onClick = onClearError,
                                    border = BorderStroke(1.dp, ErrorBorder),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("Dismiss", color = ErrorCrimson, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Scan Button Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Nearby BLE Devices",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryInk,
                            )
                            Text(
                                text = "${uiState.discoveredDevices.size} discovered",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondarySlate,
                            )
                        }

                        if (uiState.isScanning) {
                            OutlinedButton(
                                onClick = onStopScan,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, EmeraldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                                modifier = Modifier.height(48.dp).testTag("stop_scan_button"),
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop Scan")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!uiState.hasBluetoothPermissions) {
                                        permissionLauncher.launch(BlePermissionHelper.requiredPermissions())
                                    } else {
                                        onStartScan()
                                    }
                                },
                                enabled = uiState.isBluetoothSupported,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = PureWhite,
                                ),
                                modifier = Modifier.height(48.dp).testTag("start_scan_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BluetoothSearching,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan ESP32")
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EmeraldPrimary,
                            trackColor = MintLight,
                        )
                    }
                }

                if (uiState.discoveredDevices.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SurfaceMintTint,
                            border = BorderStroke(1.dp, BorderSage),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BluetoothSearching,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(32.dp),
                                )
                                Text(
                                    text = if (uiState.isScanning) {
                                        "Scanning for OptiMilk ESP32..."
                                    } else {
                                        "No BLE Devices Listed"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimaryInk,
                                )
                                Text(
                                    text = "Ensure your ESP32 colorimeter is powered on and advertising the Nordic UART Service (6E400001-...).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondarySlate,
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.discoveredDevices,
                        key = { it.address },
                    ) { device ->
                        DiscoveredDeviceRow(
                            device = device,
                            isCurrentlyConnected = (uiState.connectionState as? BleConnectionState.Connected)?.address == device.address,
                            isConnecting = (uiState.connectionState as? BleConnectionState.Connecting)?.address == device.address,
                            onConnect = { onConnectDevice(device) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBleSessionCard(
    connectionState: BleConnectionState,
    lastDeviceName: String?,
    lastDeviceAddress: String?,
    onDisconnect: () -> Unit,
    onReconnectLast: () -> Unit,
) {
    val isConnected = connectionState is BleConnectionState.Connected
    val isConnecting = connectionState is BleConnectionState.Connecting

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isConnected) MintLight else SurfaceMintTint,
        border = BorderStroke(1.dp, if (isConnected) MintBorder else BorderSage),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = if (isConnected) EmeraldPrimary else TextSecondarySlate,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (connectionState) {
                            is BleConnectionState.Connected -> "Connected: ${connectionState.deviceName}"
                            is BleConnectionState.Connecting -> "Connecting to ${connectionState.deviceName}..."
                            is BleConnectionState.Error -> "Connection Interrupted"
                            BleConnectionState.Disconnected -> "Not Connected"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryInk,
                    )
                    Text(
                        text = when (connectionState) {
                            is BleConnectionState.Connected ->
                                "${connectionState.address} • GATT Notifications Active"
                            is BleConnectionState.Connecting ->
                                "${connectionState.address} • Negotiating MTU & CCCD"
                            is BleConnectionState.Error ->
                                connectionState.message
                            BleConnectionState.Disconnected ->
                                if (lastDeviceAddress != null) {
                                    "Last device: ${lastDeviceName ?: lastDeviceAddress}"
                                } else {
                                    "Scan below and select your ESP32 colorimeter"
                                }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (connectionState is BleConnectionState.Error) ErrorCrimson else TextSecondarySlate,
                    )
                }
            }

            if (isConnecting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = EmeraldPrimary,
                    trackColor = PureWhite,
                )
            }

            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MintBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PureWhite,
                        contentColor = EmeraldDark,
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Disconnect ESP32")
                }
            } else if (!isConnecting && lastDeviceAddress != null) {
                OutlinedButton(
                    onClick = onReconnectLast,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PureWhite,
                        contentColor = EmeraldPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("reconnect_last_button"),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reconnect to ${lastDeviceName ?: lastDeviceAddress}")
                }
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceRow(
    device: DiscoveredBleDevice,
    isCurrentlyConnected: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, if (device.advertisesNus) MintBorder else BorderSage),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (device.advertisesNus) MintLight else SurfaceMintTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = if (device.advertisesNus) EmeraldPrimary else TextSecondarySlate,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (device.advertisesNus) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MintLight,
                        ) {
                            Text(
                                text = "UART",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                        color = TextSecondarySlate,
                    )
                    Icon(
                        imageVector = Icons.Filled.SignalCellularAlt,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                        color = TextSecondarySlate,
                    )
                }
            }

            Button(
                onClick = onConnect,
                enabled = !isCurrentlyConnected && !isConnecting,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentlyConnected) MintLight else EmeraldPrimary,
                    contentColor = if (isCurrentlyConnected) EmeraldDark else PureWhite,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(48.dp),
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PureWhite,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (isCurrentlyConnected) "Active" else "Connect",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BleNoticeCard(
    title: String,
    message: String,
    isError: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isError) ErrorSoftBg else WarningSoftBg,
        border = BorderStroke(1.dp, if (isError) ErrorBorder else WarningBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isError) ErrorCrimson else WarningAmber,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimaryInk,
            )
        }
    }
}

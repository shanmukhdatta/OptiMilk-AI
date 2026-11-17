package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.OptiMilkViewModel
import com.example.ui.screens.BluetoothScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BorderSage
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintLight
import com.example.ui.theme.OptiMilkTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextSecondarySlate

object OptiMilkRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_BLUETOOTH = "bluetooth"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
)

private val bottomNavItems = listOf(
    BottomNavItem(
        route = OptiMilkRoutes.ROUTE_HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        tag = "nav_home",
    ),
    BottomNavItem(
        route = OptiMilkRoutes.ROUTE_HISTORY,
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        tag = "nav_history",
    ),
    BottomNavItem(
        route = OptiMilkRoutes.ROUTE_SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        tag = "nav_settings",
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            OptiMilkTheme {
                OptiMilkApp()
            }
        }
    }
}

@Composable
fun OptiMilkApp(
    viewModel: OptiMilkViewModel = viewModel(
        factory = OptiMilkViewModel.Factory(LocalContext.current.applicationContext),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute != OptiMilkRoutes.ROUTE_BLUETOOTH

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissStatusBanner()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PureWhite,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                Box {
                    HorizontalDivider(color = BorderSage, thickness = 1.dp)
                    NavigationBar(
                        containerColor = PureWhite,
                        tonalElevation = 0.dp,
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldDark,
                                    indicatorColor = MintLight,
                                    unselectedIconColor = TextSecondarySlate,
                                    unselectedTextColor = TextSecondarySlate,
                                ),
                                modifier = Modifier.testTag(item.tag),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = OptiMilkRoutes.ROUTE_HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(OptiMilkRoutes.ROUTE_HOME) {
                HomeScreen(
                    uiState = uiState,
                    onOpenBluetoothScreen = {
                        navController.navigate(OptiMilkRoutes.ROUTE_BLUETOOTH)
                    },
                    onDisconnect = viewModel::disconnectDevice,
                    onMeasureSample = viewModel::measureSample,
                    onCalibrateBlank = viewModel::calibrateBlank,
                    onSampleNameChange = viewModel::updateEditableSampleName,
                    onSaveResult = viewModel::saveCurrentReading,
                )
            }

            composable(OptiMilkRoutes.ROUTE_HISTORY) {
                HistoryScreen(
                    uiState = uiState,
                    onFilterChange = viewModel::setHistoryFilter,
                    onSelectItem = viewModel::selectHistoryItem,
                    onDeleteItem = viewModel::deleteHistoryItem,
                )
            }

            composable(OptiMilkRoutes.ROUTE_SETTINGS) {
                SettingsScreen(
                    uiState = uiState,
                    onToggleDemoMode = viewModel::setDemoModeEnabled,
                    onUpdateSamplePrefix = viewModel::setDefaultSamplePrefix,
                    onClearDemoHistory = viewModel::clearDemoHistory,
                    onClearAllHistory = viewModel::clearAllHistory,
                )
            }

            composable(OptiMilkRoutes.ROUTE_BLUETOOTH) {
                BluetoothScreen(
                    uiState = uiState,
                    onRefreshCapabilities = viewModel::refreshBluetoothCapabilities,
                    onStartScan = viewModel::startBleScan,
                    onStopScan = viewModel::stopBleScan,
                    onConnectDevice = viewModel::connectToDevice,
                    onReconnectLast = viewModel::reconnectLastDevice,
                    onDisconnect = viewModel::disconnectDevice,
                    onClearError = viewModel::clearBleError,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}

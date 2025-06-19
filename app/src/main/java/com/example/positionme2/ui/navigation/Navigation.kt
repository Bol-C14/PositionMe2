package com.positionme.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.*
import com.example.positionme2.ui.main.device.DeviceScreen


/**
 * Defines the top-level app routes and navigation graph.
 */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Explore : Screen("explore", "Explore", Icons.Default.Place)
    object RecordReplay : Screen("record_replay", "Record", Icons.Default.FiberManualRecord)
    object Device : Screen("device", "Device", Icons.Default.Devices)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

/**
 * Root NavHost that handles authentication flow and main app graph.
 */
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("permissions") { PermissionRequestScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("calibration") { CompassCalibrationScreen(navController) }
        composable("main") { MainScreen() }
    }
}

/**
 * Main screen with bottom navigation tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Explore.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Explore.route) { ExploreScreen() }
            composable(Screen.RecordReplay.route) { RecordReplayScreen() }
            composable(Screen.Device.route) { DeviceScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

/**
 * Bottom navigation bar for main tabs.
 */
@Composable
fun BottomBar(navController: NavHostController) {
    val items = listOf(
        Screen.Explore,
        Screen.RecordReplay,
        Screen.Device,
        Screen.Settings
    )
    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// Placeholder composables for each screen
@Composable fun SplashScreen(navController: NavHostController) { /* TODO */ }
@Composable fun PermissionRequestScreen(navController: NavHostController) { /* TODO */ }
@Composable fun LoginScreen(navController: NavHostController) { /* TODO */ }
@Composable fun CompassCalibrationScreen(navController: NavHostController) { /* TODO */ }
@Composable fun ExploreScreen() { /* TODO */ }
@Composable fun RecordReplayScreen() { /* TODO */ }
@Composable fun DeviceScreen() { /* TODO */ }
@Composable fun SettingsScreen() { /* TODO */ }

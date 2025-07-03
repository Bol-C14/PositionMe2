package com.example.positionme2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.ui.compass.CompassCalibrationScreen
import com.example.positionme2.ui.main.RecordingStateManager
import com.example.positionme2.ui.main.device.DeviceScreen
import com.example.positionme2.ui.main.explore.ExploreScreen
import com.example.positionme2.ui.main.record.RecordTrajectoryScreen
import com.example.positionme2.ui.main.replay.ReplayTrajectoryScreen
import com.example.positionme2.ui.main.settings.SettingsScreen
import com.example.positionme2.ui.map.engine.GoogleMapEngine
import com.example.positionme2.ui.map.engine.adapter.FusedPositionProvider
import com.example.positionme2.ui.map.engine.adapter.GnssPositionProvider
import com.example.positionme2.ui.map.engine.adapter.PdrPositionProvider
import com.example.positionme2.ui.map.features.explore.ExploreFeatureManager
import com.example.positionme2.ui.map.features.record.RecordFeatureManager
import com.example.positionme2.ui.map.features.replay.ReplayFeatureManager
import com.example.positionme2.ui.map.marker.OptimizedMarkerRegistry
import com.example.positionme2.ui.permissions.PermissionScreen
import com.example.positionme2.ui.splash.SplashScreenWithViewModel
import com.example.positionme2.ui.splash.SplashViewModel
import com.example.positionme2.ui.theme.PositionMe2Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class Screen(val route: String, val icon: ImageVector, val title: String) {
    object Explore : Screen("explore", Icons.Default.Explore, "Explore")
    object Record : Screen("record", Icons.Default.RadioButtonChecked, "Record")
    object Replay : Screen("replay", Icons.Default.History, "Replay")
    object Settings : Screen("settings", Icons.Default.Settings, "Settings")
}

val items = listOf(
    Screen.Explore,
    Screen.Record,
    Screen.Replay,
    Screen.Settings,
)

@Composable
fun CompassScreen() {
    CompassCalibrationScreen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var recordingStateManager: RecordingStateManager
    @Inject
    lateinit var exploreFeatureManager: ExploreFeatureManager
    @Inject
    lateinit var recordFeatureManager: RecordFeatureManager
    @Inject
    lateinit var replayFeatureManager: ReplayFeatureManager
    @Inject
    lateinit var mapEngine: GoogleMapEngine
    @Inject
    lateinit var optimizedMarkerRegistry: OptimizedMarkerRegistry
    @Inject
    lateinit var recordingService: RecordingService
    @Inject
    lateinit var sensorFusionService: SensorFusionService
    @Inject
    lateinit var gnssPositionProvider: GnssPositionProvider
    @Inject
    lateinit var pdrPositionProvider: PdrPositionProvider
    @Inject
    lateinit var fusedPositionProvider: FusedPositionProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PositionMe2Theme {
                val navController = rememberNavController()
                Box {
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            val splashViewModel = hiltViewModel<SplashViewModel>()
                            // Delegate navigation to SplashScreenWithViewModel
                            SplashScreenWithViewModel(navController = navController, viewModel = splashViewModel)
                        }
                        composable("permissions") {
                            PermissionScreen(onPermissionsGranted = {
                                navController.navigate("login") {
                                    popUpTo("permissions") { inclusive = true }
                                }
                            })
                        }
                        composable("login") {
                            com.example.positionme2.ui.auth.AuthScreen(
                                onLoginSuccess = {
                                    navController.navigate("compass") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("compass") {
                            CompassCalibrationScreen(
                                onCalibrated = {
                                    navController.navigate("main") {
                                        popUpTo("compass") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("main") {
                            MainScreen(
                                recordingStateManager = recordingStateManager,
                                exploreFeatureManager = exploreFeatureManager,
                                recordFeatureManager = recordFeatureManager,
                                replayFeatureManager = replayFeatureManager,
                                mapEngine = mapEngine,
                                optimizedMarkerRegistry = optimizedMarkerRegistry,
                                recordingService = recordingService,
                                sensorFusionService = sensorFusionService
                            )
                        }
                    }
                    // Debug buttons overlay
                    Button(
                        onClick = { navController.navigate("login") },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Text("Skip to Login")
                    }
                    Button(
                        onClick = { navController.navigate("map") },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 64.dp, end = 16.dp)
                    ) {
                        Text("Skip to Map (Old)")
                    }
                    Button(
                        onClick = { navController.navigate("main") },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 112.dp, end = 16.dp)
                    ) {
                        Text("Skip to Main")
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    recordingStateManager: RecordingStateManager,
    exploreFeatureManager: ExploreFeatureManager,
    recordFeatureManager: RecordFeatureManager,
    replayFeatureManager: ReplayFeatureManager,
    mapEngine: GoogleMapEngine,
    optimizedMarkerRegistry: OptimizedMarkerRegistry,
    recordingService: RecordingService,
    sensorFusionService: SensorFusionService
) {
    val navController = rememberNavController()
    val isRecording by recordingStateManager.isRecording.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingDestination by remember { mutableStateOf<String?>(null) }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Stop Recording?") },
            text = { Text("You are currently recording. Leaving this screen will stop the recording. Do you want to continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        recordingStateManager.setRecordingState(false)
                        pendingDestination?.let { destination ->
                            navController.navigate(destination) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        pendingDestination = null
                    }
                ) {
                    Text("Stop & Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        pendingDestination = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val currentRoute = currentDestination?.route

                items.forEach { screen ->
                    BottomNavigationItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            // Check if currently on Record screen and recording is active
                            if (currentRoute == Screen.Record.route && isRecording && screen.route != Screen.Record.route) {
                                pendingDestination = screen.route
                                showConfirmDialog = true
                            } else {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Explore.route, Modifier.padding(innerPadding)) {
            composable(Screen.Explore.route) {
                ExploreScreen(
                    exploreFeatureManager = exploreFeatureManager,
                    mapEngine = mapEngine,
                    recordingService = recordingService,
                    sensorFusionService = sensorFusionService,
                    markerRegistry = optimizedMarkerRegistry
                )
            }
            composable(Screen.Record.route) {
                RecordTrajectoryScreen(
                    recordingStateManager = recordingStateManager,
                    recordFeatureManager = recordFeatureManager,
                    mapEngine = mapEngine,
                    markerRegistry = optimizedMarkerRegistry,
                    recordingService = recordingService,
                    sensorFusionService = sensorFusionService
                )
            }
            composable(Screen.Replay.route) {
                ReplayTrajectoryScreen(
                    replayFeatureManager = replayFeatureManager,
                    mapEngine = mapEngine,
                    markerRegistry = optimizedMarkerRegistry,
                    recordingService = recordingService,
                    sensorFusionService = sensorFusionService
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

package com.example.positionme2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.positionme2.data.repository.IndoorMapRepository
import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.ui.compass.CompassCalibrationScreen
import com.example.positionme2.ui.main.device.DeviceScreen
import com.example.positionme2.ui.map.engine.GoogleMapEngine
import com.example.positionme2.ui.map.features.explore.ExploreScreen
import com.example.positionme2.ui.map.features.record.RecordTrajectoryScreen
import com.example.positionme2.ui.map.features.replay.ReplayTrajectoryScreen
import com.example.positionme2.ui.theme.PositionMe2Theme
import com.example.positionme2.ui.splash.SplashScreenWithViewModel
import com.example.positionme2.ui.splash.SplashViewModel
import com.example.positionme2.ui.permissions.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@Composable
fun CompassScreen() {
    CompassCalibrationScreen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sensorFusionService: SensorFusionService
    @Inject
    lateinit var indoorMapRepository: IndoorMapRepository
    @Inject
    lateinit var recordingService: RecordingService

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
                                    navController.navigate("map") {
                                        popUpTo("compass") { inclusive = true }
                                    }
                            }
                            )
                        }
                        composable("device") { DeviceScreen() }
                        composable("map") {
                            val mapEngine = GoogleMapEngine(this@MainActivity, sensorFusionService, indoorMapRepository)
                            mapEngine.startTracking()
                            ExploreScreen(mapEngine = mapEngine, recordingService = recordingService)
                        }
                        composable("record") {
                            val mapEngine = GoogleMapEngine(this@MainActivity, sensorFusionService, indoorMapRepository)
                            mapEngine.startTracking()
                            RecordTrajectoryScreen(mapEngine = mapEngine)
                        }
                        composable("replay") {
                            val mapEngine = GoogleMapEngine(this@MainActivity, sensorFusionService, indoorMapRepository)
                            mapEngine.startTracking()
                            ReplayTrajectoryScreen(mapEngine = mapEngine)
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
                        Text("Skip to Map")
                    }
                }
            }
        }
    }
}

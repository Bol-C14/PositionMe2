package com.example.positionme2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.positionme2.ui.compass.CompassCalibrationScreen
import com.example.positionme2.ui.main.device.DeviceScreen
import com.example.positionme2.ui.theme.PositionMe2Theme
import com.example.positionme2.ui.splash.SplashScreenWithViewModel
import com.example.positionme2.ui.splash.SplashViewModel
import com.example.positionme2.ui.permissions.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint

@Composable
fun CompassScreen() {
    CompassCalibrationScreen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PositionMe2Theme {
                val navController = rememberNavController()
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
                                navController.navigate("device") {
                                    popUpTo("compass") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("device") { DeviceScreen() }
                }
            }
        }
    }
}

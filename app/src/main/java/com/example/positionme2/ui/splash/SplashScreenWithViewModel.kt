package com.example.positionme2.ui.splash

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the splash flow navigation decisions.
 */
sealed class SplashUiState {
    object Loading : SplashUiState()
    object NavigateToPermissions : SplashUiState()
    object NavigateToLogin : SplashUiState()
    object NavigateToCalibration : SplashUiState()
    object NavigateToMain : SplashUiState()
}

/**
 * ViewModel for SplashScreen. Checks permissions, login and calibration status.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "positionme_prefs", Context.MODE_PRIVATE
    )
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    companion object {
        private val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
            // Add other runtime permissions here as needed
        )
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CALIBRATED = "calibrated"
        private const val KEY_VERSION_CODE = "version_code"
    }

    init {
        viewModelScope.launch {
            // Detect app update or first launch
            val currentVersion = com.example.positionme2.BuildConfig.VERSION_CODE
            val savedVersion = prefs.getInt(KEY_VERSION_CODE, -1)
            if (savedVersion != currentVersion) {
                prefs.edit().clear().putInt(KEY_VERSION_CODE, currentVersion).apply()
            }

            // Perform navigation checks
            delay(2000) // Simulate loading
            if (arePermissionsGranted()) {
                if (isUserLoggedIn()) {
                    // Check if session is valid
                    if (isSessionValid()) {
                        _uiState.value = SplashUiState.NavigateToCalibration
                    } else {
                        clearUserSession()
                        _uiState.value = SplashUiState.NavigateToLogin
                    }
                } else {
                    _uiState.value = SplashUiState.NavigateToLogin
                }
            } else {
                _uiState.value = SplashUiState.NavigateToPermissions
            }
        }
    }

    private fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    private fun isSessionValid(): Boolean {
        // Add logic to validate session (e.g., check token expiration)
        val authToken = prefs.getString(KEY_AUTH_TOKEN, null)
        return !authToken.isNullOrEmpty()
    }

    private fun clearUserSession() {
        prefs.edit().remove("is_logged_in").remove(KEY_AUTH_TOKEN).apply()
    }

    private fun arePermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Avoid context leaks
        prefs.edit().clear().apply()
    }
}

/**
 * Splash screen composable. Observes ViewModel and navigates accordingly.
 */
@Composable
fun SplashScreenWithViewModel(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation based on UI state
    LaunchedEffect(uiState) {
        when (uiState) {
            is SplashUiState.NavigateToPermissions -> navController.navigate("permissions") {
                popUpTo("splash") { inclusive = true }
            }
            is SplashUiState.NavigateToLogin -> navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
            is SplashUiState.NavigateToCalibration -> navController.navigate("compass") {
                popUpTo("splash") { inclusive = true }
            }
            is SplashUiState.NavigateToMain -> navController.navigate("device") {
                popUpTo("splash") { inclusive = true }
            }
            else -> {}
        }
    }

    // Splash screen UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("PositionMe", style = MaterialTheme.typography.headlineLarge)
    }
}

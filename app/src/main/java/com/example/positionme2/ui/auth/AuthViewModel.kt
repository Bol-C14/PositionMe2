package com.example.positionme2.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.positionme2.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    fun login(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val request = LoginRequest(
                username = username,
                email = email,
                password = password
            )

            authService.login(request)
                .onSuccess { user ->
                    _user.value = user
                    _authState.value = AuthState.Success(user)
                    saveApiKey(user.api_key)
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Unknown error")
                }
        }
    }

    fun signup(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val request = SignupRequest(
                username = username,
                email = email,
                password = password
            )

            authService.signup(request)
                .onSuccess { user ->
                    _user.value = user
                    _authState.value = AuthState.Success(user)
                    saveApiKey(user.api_key)
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Unknown error")
                }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        _user.value = null
        _authState.value = AuthState.Idle
        userPreferences.clearApiKey()
    }

    private fun saveApiKey(apiKey: String) {
        userPreferences.saveApiKey(apiKey)
    }
}

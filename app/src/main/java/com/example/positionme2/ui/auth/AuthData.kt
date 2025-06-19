package com.example.positionme2.ui.auth

import java.time.Instant

// Request models
data class SignupRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val email: String,
    val password: String
)

// Response models
data class User(
    val username: String,
    val id: Int,
    val is_active: Boolean,
    val date_created: String, // ISO-8601 date format
    val api_key: String,
    val trajectories: List<Trajectory> = emptyList()
)

data class Trajectory(
    val id: Int,
    val name: String,
    val date_created: String, // ISO-8601 date format
    val file_name: String
)

// Auth state management
sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

// Validation Error from API
data class ValidationError(
    val detail: List<ValidationErrorDetail>
)

data class ValidationErrorDetail(
    val loc: List<Any>, // Can be string or int
    val msg: String,
    val type: String
)

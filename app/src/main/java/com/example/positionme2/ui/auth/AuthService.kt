package com.example.positionme2.ui.auth

import com.example.positionme2.data.api.ApiService
import com.example.positionme2.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {
    suspend fun signup(request: SignupRequest): Result<User> = withContext(Dispatchers.IO) {
        try {
            val result = apiService.signup(
                username = request.username,
                email = request.email,
                password = request.password
            )

            result.fold(
                onSuccess = { jsonResponse ->
                    Result.success(parseUserFromJson(jsonResponse))
                },
                onFailure = { e ->
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<User> = withContext(Dispatchers.IO) {
        try {
            val result = apiService.login(
                username = request.username,
                email = request.email,
                password = request.password
            )

            result.fold(
                onSuccess = { jsonResponse ->
                    Result.success(parseUserFromJson(jsonResponse))
                },
                onFailure = { e ->
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseUserFromJson(json: JSONObject): User {
        val trajectoriesJson = json.optJSONArray("trajectories") ?: JSONObject().optJSONArray("trajectories")
        val trajectories = mutableListOf<Trajectory>()

        for (i in 0 until (trajectoriesJson?.length() ?: 0)) {
            val trajectoryJson = trajectoriesJson?.getJSONObject(i)
            trajectoryJson?.let {
                val trajectory = Trajectory(
                    id = it.getInt("id"),
                    name = it.getString("name"),
                    date_created = it.getString("date_created"),
                    file_name = it.getString("file_name")
                )
                trajectories.add(trajectory)
            }
        }

        return User(
            username = json.getString("username"),
            id = json.getInt("id"),
            is_active = json.getBoolean("is_active"),
            date_created = json.getString("date_created"),
            api_key = json.getString("api_key"),
            trajectories = trajectories
        )
    }
}

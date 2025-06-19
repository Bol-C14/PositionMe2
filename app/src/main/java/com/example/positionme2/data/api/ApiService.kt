package com.example.positionme2.data.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.positionme2.BuildConfig
import com.example.positionme2.data.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This class handles all API communications with the OpenPositioning server.
 * It handles authentication, trajectory uploads/downloads, and other API operations.
 */
@Singleton
class ApiService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {
    // Network status checking
    private val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var isWifiConn = false
    private var isMobileConn = false

    companion object {
        private const val BASE_URL = "https://openpositioning.org/api"
        private const val MEDIA_TYPE_JSON = "application/json; charset=utf-8"
        private const val PROTOCOL_ACCEPT_TYPE = "application/json"

        // Endpoint URLs
        private const val SIGNUP_ENDPOINT = "$BASE_URL/users/signup"
        private const val LOGIN_ENDPOINT = "$BASE_URL/users/login"
    }

    /**
     * Constructs API URLs with the appropriate API keys
     */
    private fun constructAuthenticatedUrl(endpoint: String): String {
        val apiKey = userPreferences.getApiKey()
        // If there's a master key in BuildConfig, add it to the URL
        return try {
            val masterKey = BuildConfig.OPENPOSITIONING_MASTER_KEY
            "$endpoint/$apiKey?key=$masterKey"
        } catch (_: Exception) {
            // If master key not found, just use the API key
            "$endpoint/$apiKey"
        }
    }

    /**
     * Endpoint for trajectory upload
     */
    fun getTrajectoryUploadUrl(): String {
        return constructAuthenticatedUrl("$BASE_URL/live/trajectory/upload")
    }

    /**
     * Endpoint for trajectory download
     */
    fun getTrajectoryDownloadUrl(): String {
        return constructAuthenticatedUrl("$BASE_URL/live/trajectory/download")
    }

    /**
     * Endpoint for user trajectories info
     */
    fun getUserTrajectoriesUrl(): String {
        return constructAuthenticatedUrl("$BASE_URL/live/users/trajectories")
    }

    /**
     * Check if the device is connected to a network using modern API
     */
    fun checkNetworkStatus(): Boolean {
        val networkCapabilities = connMgr.getNetworkCapabilities(connMgr.activeNetwork)

        if (networkCapabilities != null) {
            isWifiConn = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            isMobileConn = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            return true
        } else {
            isWifiConn = false
            isMobileConn = false
            return false
        }
    }

    /**
     * Perform login request to the API
     */
    suspend fun login(username: String, email: String, password: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("username", username)
                put("email", email)
                put("password", password)
            }.toString()

            val url = URL(LOGIN_ENDPOINT)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", MEDIA_TYPE_JSON)
            connection.setRequestProperty("Accept", PROTOCOL_ACCEPT_TYPE)
            connection.doOutput = true

            connection.outputStream.use { os ->
                val input = jsonBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    val jsonResponse = JSONObject(response)

                    // Save the API key for future authenticated requests
                    val apiKey = jsonResponse.getString("api_key")
                    userPreferences.saveApiKey(apiKey)

                    Result.success(jsonResponse)
                }
            } else {
                val errorStream = connection.errorStream ?: connection.inputStream
                val errorBody = errorStream.bufferedReader().use { it.readText() }
                Result.failure(IOException("API error: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Perform signup request to the API
     */
    suspend fun signup(username: String, email: String, password: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("username", username)
                put("email", email)
                put("password", password)
            }.toString()

            val url = URL(SIGNUP_ENDPOINT)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", MEDIA_TYPE_JSON)
            connection.setRequestProperty("Accept", PROTOCOL_ACCEPT_TYPE)
            connection.doOutput = true

            connection.outputStream.use { os ->
                val input = jsonBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    val jsonResponse = JSONObject(response)

                    // Save the API key for future authenticated requests
                    val apiKey = jsonResponse.getString("api_key")
                    userPreferences.saveApiKey(apiKey)

                    Result.success(jsonResponse)
                }
            } else {
                val errorStream = connection.errorStream ?: connection.inputStream
                val errorBody = errorStream.bufferedReader().use { it.readText() }
                Result.failure(IOException("API error: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

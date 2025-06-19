package com.example.positionme2.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFERENCES_FILE = "user_preferences"
        private const val API_KEY = "api_key"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_FILE, Context.MODE_PRIVATE
    )

    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(API_KEY, apiKey).apply()
    }

    fun getApiKey(): String {
        return sharedPreferences.getString(API_KEY, "") ?: ""
    }

    fun clearApiKey() {
        sharedPreferences.edit().remove(API_KEY).apply()
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}

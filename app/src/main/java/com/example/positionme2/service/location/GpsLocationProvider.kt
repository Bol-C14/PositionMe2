package com.example.positionme2.service.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.positionme2.domain.location.LocationProvider
import com.example.positionme2.utils.CoordinateTransform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GPS-based implementation of LocationProvider domain interface
 */
@Singleton
class GpsLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider, LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _currentLocation = MutableLiveData<CoordinateTransform.GpsCoordinate>()
    val currentLocation: LiveData<CoordinateTransform.GpsCoordinate> get() = _currentLocation

    private val _locationAccuracy = MutableLiveData<Float>()
    val locationAccuracy: LiveData<Float> get() = _locationAccuracy

    private var isUpdatesStarted = false

    companion object {
        private const val TAG = "GpsLocationProvider"
        private const val MIN_TIME_BETWEEN_UPDATES = 1000L
        private const val MIN_DISTANCE_CHANGE = 0.5f
    }

    override suspend fun getCurrentLocation(): Result<CoordinateTransform.GpsCoordinate> {
        return try {
            getLastKnownLocation()?.let { location ->
                val gpsCoordinate = CoordinateTransform.fromAndroidLocation(location)
                Result.success(gpsCoordinate)
            } ?: Result.failure(Exception("No location available"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun waitForAccurateLocation(
        maxAccuracyMeters: Float,
        timeoutMs: Long
    ): Result<CoordinateTransform.GpsCoordinate> {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                var resumed = false

                if (!startLocationUpdates()) {
                    if (!resumed) {
                        resumed = true
                        continuation.resume(Result.failure(Exception("Cannot start location updates")))
                    }
                    return@suspendCancellableCoroutine
                }

                val locationObserver = androidx.lifecycle.Observer<CoordinateTransform.GpsCoordinate> { coordinate ->
                    val accuracy = _locationAccuracy.value
                    if (!resumed && accuracy != null && accuracy <= maxAccuracyMeters) {
                        resumed = true
                        continuation.resume(Result.success(coordinate))
                    }
                }

                _currentLocation.observeForever(locationObserver)

                continuation.invokeOnCancellation {
                    _currentLocation.removeObserver(locationObserver)
                }
            }
        } ?: Result.failure(Exception("Location timeout after ${timeoutMs}ms"))
    }

    override fun startLocationUpdates(): Boolean {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return false
        }

        if (isUpdatesStarted) {
            return true
        }

        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { locationManager.isProviderEnabled(it) }

            if (providers.isEmpty()) {
                Log.w(TAG, "No location providers available")
                return false
            }

            providers.forEach { provider ->
                locationManager.requestLocationUpdates(
                    provider,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    this
                )
            }

            isUpdatesStarted = true
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location updates", e)
            return false
        }
    }

    override fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
            isUpdatesStarted = false
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping location updates", e)
        }
    }

    override fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        return try {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.accuracy <= networkLocation.accuracy) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // LocationListener implementation
    override fun onLocationChanged(location: Location) {
        val gpsCoordinate = CoordinateTransform.fromAndroidLocation(location)

        if (CoordinateTransform.isValidWgs84(gpsCoordinate.latitude, gpsCoordinate.longitude)) {
            _currentLocation.postValue(gpsCoordinate)
            _locationAccuracy.postValue(location.accuracy)
            Log.d(TAG, "Location updated: ${gpsCoordinate.latitude}, ${gpsCoordinate.longitude}, accuracy: ${location.accuracy}m")
        }
    }

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Provider disabled: $provider")
    }
}

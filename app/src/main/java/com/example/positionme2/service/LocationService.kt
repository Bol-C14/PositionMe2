package com.example.positionme2.service

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
import com.example.positionme2.utils.CoordinateTransform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing GPS location and providing initial position for PDR
 */
@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _currentLocation = MutableLiveData<CoordinateTransform.GpsCoordinate>()
    val currentLocation: LiveData<CoordinateTransform.GpsCoordinate> get() = _currentLocation

    private val _locationAccuracy = MutableLiveData<Float>()
    val locationAccuracy: LiveData<Float> get() = _locationAccuracy

    private val _isGpsEnabled = MutableLiveData<Boolean>()
    val isGpsEnabled: LiveData<Boolean> get() = _isGpsEnabled

    private var referencePoint: CoordinateTransform.GpsCoordinate? = null
    private var isLocationUpdatesStarted = false

    companion object {
        private const val MIN_TIME_BETWEEN_UPDATES = 1000L // 1 second
        private const val MIN_DISTANCE_CHANGE = 0.5f // 0.5 meters
        private const val GPS_ACCURACY_THRESHOLD = 10.0f // 10 meters
        private const val TAG = "LocationService"
    }

    /**
     * Start GPS location updates
     */
    fun startLocationUpdates(): Boolean {
        if (!hasLocationPermission()) {
            return false
        }

        val isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        _isGpsEnabled.postValue(isGpsProviderEnabled || isNetworkProviderEnabled)

        if (!isGpsProviderEnabled && !isNetworkProviderEnabled) {
            return false
        }

        try {
            // Request updates from both GPS and Network providers
            if (isGpsProviderEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    this
                )
            }

            if (isNetworkProviderEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    this
                )
            }

            // Get last known location immediately
            getLastKnownLocation()?.let { location ->
                updateLocation(location)
            }

            isLocationUpdatesStarted = true
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    /**
     * Stop GPS location updates
     */
    fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
            isLocationUpdatesStarted = false
        } catch (e: SecurityException) {
            // Handle the exception
        }
    }

    /**
     * Get the best available last known location
     */
    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        val gpsLocation = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) {
            null
        }

        val networkLocation = try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            null
        }

        // Return the most accurate location
        return when {
            gpsLocation != null && networkLocation != null -> {
                if (gpsLocation.accuracy <= networkLocation.accuracy) gpsLocation else networkLocation
            }
            gpsLocation != null -> gpsLocation
            networkLocation != null -> networkLocation
            else -> null
        }
    }

    /**
     * Set reference point for ENU coordinate system
     */
    fun setReferencePoint(gpsCoordinate: CoordinateTransform.GpsCoordinate) {
        referencePoint = gpsCoordinate
    }

    /**
     * Get reference point (usually the first GPS fix)
     */
    fun getReferencePoint(): CoordinateTransform.GpsCoordinate? = referencePoint

    /**
     * Convert current GPS position to ENU coordinates
     */
    fun getCurrentEnuPosition(): CoordinateTransform.EnuCoordinate? {
        val current = _currentLocation.value
        val reference = referencePoint

        return if (current != null && reference != null) {
            CoordinateTransform.gpsToEnu(current, reference)
        } else null
    }

    /**
     * Check if app has location permissions
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateLocation(location: Location) {
        // Ensure WGS84 compliance by converting Android Location
        val gpsCoordinate = CoordinateTransform.fromAndroidLocation(location)

        // Validate coordinates are valid WGS84
        if (!CoordinateTransform.isValidWgs84(gpsCoordinate.latitude, gpsCoordinate.longitude)) {
            Log.w(TAG, "Invalid WGS84 coordinates received: ${gpsCoordinate.latitude}, ${gpsCoordinate.longitude}")
            return
        }

        _currentLocation.postValue(gpsCoordinate)
        _locationAccuracy.postValue(location.accuracy)

        Log.d(TAG, "WGS84 location updated: lat=${gpsCoordinate.latitude}, lng=${gpsCoordinate.longitude}, alt=${gpsCoordinate.altitude}, accuracy=${location.accuracy}m")

        // Set reference point on first good GPS fix
        if (referencePoint == null && location.accuracy <= GPS_ACCURACY_THRESHOLD) {
            setReferencePoint(gpsCoordinate)
            Log.i(TAG, "WGS84 reference point set: ${gpsCoordinate.latitude}, ${gpsCoordinate.longitude}")
        }
    }

    // LocationListener implementation
    override fun onLocationChanged(location: Location) {
        updateLocation(location)
    }

    override fun onProviderEnabled(provider: String) {
        _isGpsEnabled.postValue(true)
    }

    override fun onProviderDisabled(provider: String) {
        val isAnyProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        _isGpsEnabled.postValue(isAnyProviderEnabled)
    }
}

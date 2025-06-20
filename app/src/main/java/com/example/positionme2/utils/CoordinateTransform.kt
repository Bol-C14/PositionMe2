package com.example.positionme2.utils

import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

/**
 * Utility class for coordinate transformations between different reference systems
 *
 * Provides conversion methods between:
 * - Geodetic coordinates (latitude, longitude, altitude)
 * - ECEF (Earth-Centered, Earth-Fixed) coordinates
 * - ENU (East-North-Up) local coordinates
 */
object CoordinateTransform {
    // WGS84 ellipsoid constants
    private const val WGS84_A = 6378137.0         // semi-major axis in meters
    private const val WGS84_F = 1.0 / 298.257223563 // flattening
    private const val WGS84_B = WGS84_A * (1.0 - WGS84_F) // semi-minor axis
    private const val WGS84_E_SQ = 2 * WGS84_F - WGS84_F * WGS84_F // eccentricity squared

    /**
     * Convert geodetic coordinates (latitude, longitude, altitude) to
     * ECEF (Earth-Centered, Earth-Fixed) coordinates
     *
     * @param lat Latitude in degrees
     * @param lon Longitude in degrees
     * @param alt Altitude in meters above ellipsoid
     * @return Double array with [x, y, z] ECEF coordinates in meters
     */
    fun geodeticToEcef(lat: Double, lon: Double, alt: Double): DoubleArray {
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)

        // Radius of curvature in the prime vertical
        val n = WGS84_A / sqrt(1 - WGS84_E_SQ * sinLat * sinLat)

        // ECEF coordinates
        val x = (n + alt) * cosLat * cosLon
        val y = (n + alt) * cosLat * sinLon
        val z = (n * (1 - WGS84_E_SQ) + alt) * sinLat

        return doubleArrayOf(x, y, z)
    }

    /**
     * Convert ECEF (Earth-Centered, Earth-Fixed) coordinates to
     * geodetic coordinates (latitude, longitude, altitude)
     *
     * @param x ECEF X coordinate in meters
     * @param y ECEF Y coordinate in meters
     * @param z ECEF Z coordinate in meters
     * @return Double array with [latitude, longitude, altitude] in [degrees, degrees, meters]
     */
    fun ecefToGeodetic(x: Double, y: Double, z: Double): DoubleArray {
        val p = sqrt(x * x + y * y)
        val theta = atan2(z * WGS84_A, p * WGS84_B)

        val sinTheta = sin(theta)
        val cosTheta = cos(theta)

        // Longitude is straightforward
        val lon = atan2(y, x)

        // Latitude requires iteration for accuracy, but this approximation is usually good enough
        val lat = atan2(
            z + WGS84_E_SQ * WGS84_B * sinTheta * sinTheta * sinTheta,
            p - WGS84_E_SQ * WGS84_A * cosTheta * cosTheta * cosTheta
        )

        val sinLat = sin(lat)
        val n = WGS84_A / sqrt(1 - WGS84_E_SQ * sinLat * sinLat)

        // Altitude
        val alt = p / cos(lat) - n

        return doubleArrayOf(
            Math.toDegrees(lat),
            Math.toDegrees(lon),
            alt
        )
    }

    /**
     * Convert geodetic coordinates to local ENU (East-North-Up) coordinates
     * relative to a reference point
     *
     * @param lat Latitude in degrees
     * @param lon Longitude in degrees
     * @param alt Altitude in meters
     * @param refLat Reference latitude in degrees
     * @param refLon Reference longitude in degrees
     * @param refAlt Reference altitude in meters
     * @return Double array with [east, north, up] coordinates in meters
     */
    fun geodeticToEnu(
        lat: Double, lon: Double, alt: Double,
        refLat: Double, refLon: Double, refAlt: Double
    ): DoubleArray {
        // Convert to ECEF
        val ecef = geodeticToEcef(lat, lon, alt)
        val refEcef = geodeticToEcef(refLat, refLon, refAlt)

        // Delta in ECEF
        val dx = ecef[0] - refEcef[0]
        val dy = ecef[1] - refEcef[1]
        val dz = ecef[2] - refEcef[2]

        // Convert delta to ENU
        val refLatRad = Math.toRadians(refLat)
        val refLonRad = Math.toRadians(refLon)

        val sinLat = sin(refLatRad)
        val cosLat = cos(refLatRad)
        val sinLon = sin(refLonRad)
        val cosLon = cos(refLonRad)

        // Calculate ENU components
        val east = -sinLon * dx + cosLon * dy
        val north = -sinLat * cosLon * dx - sinLat * sinLon * dy + cosLat * dz
        val up = cosLat * cosLon * dx + cosLat * sinLon * dy + sinLat * dz

        return doubleArrayOf(east, north, up)
    }

    /**
     * Convert local ENU (East-North-Up) coordinates to geodetic coordinates
     *
     * @param east East coordinate in meters
     * @param north North coordinate in meters
     * @param up Up coordinate in meters
     * @param refLat Reference latitude in degrees
     * @param refLon Reference longitude in degrees
     * @param refAlt Reference altitude in meters
     * @return LatLng object with converted coordinates
     */
    fun enuToGeodetic(
        east: Double, north: Double, up: Double,
        refLat: Double, refLon: Double, refAlt: Double
    ): LatLng {
        // Get reference point in ECEF
        val refEcef = geodeticToEcef(refLat, refLon, refAlt)

        // Convert ENU to ECEF
        val refLatRad = Math.toRadians(refLat)
        val refLonRad = Math.toRadians(refLon)

        val sinLat = sin(refLatRad)
        val cosLat = cos(refLatRad)
        val sinLon = sin(refLonRad)
        val cosLon = cos(refLonRad)

        // Calculate ECEF coordinates from ENU
        val dx = -sinLon * east - sinLat * cosLon * north + cosLat * cosLon * up
        val dy = cosLon * east - sinLat * sinLon * north + cosLat * sinLon * up
        val dz = cosLat * north + sinLat * up

        // Add to reference ECEF
        val x = refEcef[0] + dx
        val y = refEcef[1] + dy
        val z = refEcef[2] + dz

        // Convert back to geodetic
        val geodetic = ecefToGeodetic(x, y, z)

        return LatLng(geodetic[0], geodetic[1])
    }

    /**
     * Calculate distance between two points in meters
     *
     * @param lat1 First point latitude in degrees
     * @param lon1 First point longitude in degrees
     * @param lat2 Second point latitude in degrees
     * @param lon2 Second point longitude in degrees
     * @return Distance in meters
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)

        // Haversine formula
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Calculate distance between two LatLng points in meters
     */
    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        return calculateDistance(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude
        )
    }

    /**
     * Calculate bearing between two points in degrees
     *
     * @param lat1 First point latitude in degrees
     * @param lon1 First point longitude in degrees
     * @param lat2 Second point latitude in degrees
     * @param lon2 Second point longitude in degrees
     * @return Bearing in degrees from North (0-360)
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) -
                sin(lat1Rad) * cos(lat2Rad) * cos(deltaLon)

        var bearing = Math.toDegrees(atan2(y, x))

        // Normalize to 0-360
        bearing = (bearing + 360) % 360

        return bearing
    }
}

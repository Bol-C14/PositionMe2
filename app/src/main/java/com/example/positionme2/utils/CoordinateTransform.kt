package com.example.positionme2.utils

import kotlin.math.*

/**
 * Utility class for coordinate transformations ensuring WGS84 compliance across all systems
 * All GPS coordinates are assumed to be in WGS84 datum
 */
object CoordinateTransform {

    // WGS84 ellipsoid parameters
    private const val WGS84_A = 6378137.0              // Semi-major axis (meters)
    private const val WGS84_F = 1.0 / 298.257223563    // Flattening
    private const val WGS84_B = WGS84_A * (1.0 - WGS84_F)  // Semi-minor axis
    private const val WGS84_E2 = 2.0 * WGS84_F - WGS84_F * WGS84_F  // First eccentricity squared

    /**
     * GPS coordinate in WGS84 datum
     */
    data class GpsCoordinate(
        val latitude: Double,   // WGS84 latitude in decimal degrees
        val longitude: Double,  // WGS84 longitude in decimal degrees
        val altitude: Double = 0.0  // Height above WGS84 ellipsoid in meters
    ) {
        init {
            require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90 degrees" }
            require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180 degrees" }
        }
    }

    /**
     * Local ENU (East-North-Up) coordinate relative to a WGS84 reference point
     */
    data class EnuCoordinate(
        val east: Double,   // East distance in meters
        val north: Double,  // North distance in meters
        val up: Double = 0.0    // Up distance in meters
    )

    /**
     * ECEF (Earth-Centered, Earth-Fixed) coordinate in WGS84
     */
    data class EcefCoordinate(
        val x: Double,  // X coordinate in meters
        val y: Double,  // Y coordinate in meters
        val z: Double   // Z coordinate in meters
    )

    /**
     * Convert WGS84 GPS coordinates to local ENU coordinates relative to a reference point
     * This is the primary method for PDR coordinate conversion
     */
    fun gpsToEnu(
        gpsPoint: GpsCoordinate,
        referencePoint: GpsCoordinate
    ): EnuCoordinate {
        // Convert both points to ECEF first for accurate transformation
        val pointEcef = gpsToEcef(gpsPoint)
        val refEcef = gpsToEcef(referencePoint)

        // Convert ECEF difference to ENU
        return ecefToEnu(pointEcef, refEcef, referencePoint)
    }

    /**
     * Convert local ENU coordinates back to WGS84 GPS coordinates
     */
    fun enuToGps(
        enuPoint: EnuCoordinate,
        referencePoint: GpsCoordinate
    ): GpsCoordinate {
        // Convert reference to ECEF
        val refEcef = gpsToEcef(referencePoint)

        // Convert ENU to ECEF offset
        val pointEcef = enuToEcef(enuPoint, refEcef, referencePoint)

        // Convert back to WGS84 GPS
        return ecefToGps(pointEcef)
    }

    /**
     * Convert WGS84 GPS to ECEF coordinates
     */
    private fun gpsToEcef(gps: GpsCoordinate): EcefCoordinate {
        val latRad = Math.toRadians(gps.latitude)
        val lonRad = Math.toRadians(gps.longitude)

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)

        // Radius of curvature in the prime vertical
        val N = WGS84_A / sqrt(1.0 - WGS84_E2 * sinLat * sinLat)

        val x = (N + gps.altitude) * cosLat * cosLon
        val y = (N + gps.altitude) * cosLat * sinLon
        val z = (N * (1.0 - WGS84_E2) + gps.altitude) * sinLat

        return EcefCoordinate(x, y, z)
    }

    /**
     * Convert ECEF coordinates to WGS84 GPS
     */
    private fun ecefToGps(ecef: EcefCoordinate): GpsCoordinate {
        val x = ecef.x
        val y = ecef.y
        val z = ecef.z

        val p = sqrt(x * x + y * y)
        val theta = atan2(z * WGS84_A, p * WGS84_B)

        val sinTheta = sin(theta)
        val cosTheta = cos(theta)

        val longitude = atan2(y, x)
        val latitude = atan2(
            z + WGS84_E2 * WGS84_B / (1.0 - WGS84_E2) * sinTheta * sinTheta * sinTheta,
            p - WGS84_E2 * WGS84_A * cosTheta * cosTheta * cosTheta
        )

        val sinLat = sin(latitude)
        val N = WGS84_A / sqrt(1.0 - WGS84_E2 * sinLat * sinLat)
        val altitude = p / cos(latitude) - N

        return GpsCoordinate(
            latitude = Math.toDegrees(latitude),
            longitude = Math.toDegrees(longitude),
            altitude = altitude
        )
    }

    /**
     * Convert ECEF difference to ENU relative to reference point
     */
    private fun ecefToEnu(
        pointEcef: EcefCoordinate,
        refEcef: EcefCoordinate,
        refGps: GpsCoordinate
    ): EnuCoordinate {
        val dx = pointEcef.x - refEcef.x
        val dy = pointEcef.y - refEcef.y
        val dz = pointEcef.z - refEcef.z

        val latRad = Math.toRadians(refGps.latitude)
        val lonRad = Math.toRadians(refGps.longitude)

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)

        val east = -sinLon * dx + cosLon * dy
        val north = -sinLat * cosLon * dx - sinLat * sinLon * dy + cosLat * dz
        val up = cosLat * cosLon * dx + cosLat * sinLon * dy + sinLat * dz

        return EnuCoordinate(east, north, up)
    }

    /**
     * Convert ENU to ECEF offset from reference point
     */
    private fun enuToEcef(
        enu: EnuCoordinate,
        refEcef: EcefCoordinate,
        refGps: GpsCoordinate
    ): EcefCoordinate {
        val latRad = Math.toRadians(refGps.latitude)
        val lonRad = Math.toRadians(refGps.longitude)

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)

        val dx = -sinLon * enu.east - sinLat * cosLon * enu.north + cosLat * cosLon * enu.up
        val dy = cosLon * enu.east - sinLat * sinLon * enu.north + cosLat * sinLon * enu.up
        val dz = cosLat * enu.north + sinLat * enu.up

        return EcefCoordinate(
            x = refEcef.x + dx,
            y = refEcef.y + dy,
            z = refEcef.z + dz
        )
    }

    /**
     * Calculate accurate distance between two WGS84 GPS points using Vincenty's formula
     */
    fun calculateDistance(point1: GpsCoordinate, point2: GpsCoordinate): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val deltaLon = Math.toRadians(point2.longitude - point1.longitude)

        val a = WGS84_A
        val b = WGS84_B
        val f = WGS84_F

        val L = deltaLon
        val U1 = atan((1 - f) * tan(lat1))
        val U2 = atan((1 - f) * tan(lat2))
        val sinU1 = sin(U1)
        val cosU1 = cos(U1)
        val sinU2 = sin(U2)
        val cosU2 = cos(U2)

        var lambda = L
        var lambdaP: Double
        var iterLimit = 100
        var cosSqAlpha: Double
        var sinSigma: Double
        var cos2SigmaM: Double
        var cosLambda: Double
        var sinLambda: Double
        var sigma: Double
        var cosSigma: Double

        do {
            sinLambda = sin(lambda)
            cosLambda = cos(lambda)
            sinSigma = sqrt(
                (cosU2 * sinLambda) * (cosU2 * sinLambda) +
                (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
            )

            if (sinSigma == 0.0) return 0.0  // Co-incident points

            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
            sigma = atan2(sinSigma, cosSigma)
            val sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
            cosSqAlpha = 1 - sinAlpha * sinAlpha
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha

            if (cos2SigmaM.isNaN()) cos2SigmaM = 0.0  // Equatorial line

            val C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha))
            lambdaP = lambda
            lambda = L + (1 - C) * f * sinAlpha * (
                sigma + C * sinSigma * (
                    cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)
                )
            )
        } while (abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0)

        if (iterLimit == 0) {
            // Formula failed to converge, fall back to haversine
            return calculateDistanceHaversine(point1, point2)
        }

        val uSq = cosSqAlpha * (a * a - b * b) / (b * b)
        val A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
        val B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))
        val deltaSigma = B * sinSigma * (
            cos2SigmaM + B / 4 * (
                cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) -
                B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)
            )
        )

        return b * A * (sigma - deltaSigma)
    }

    /**
     * Fallback distance calculation using Haversine formula
     */
    private fun calculateDistanceHaversine(point1: GpsCoordinate, point2: GpsCoordinate): Double {
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLon = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return WGS84_A * c
    }

    /**
     * Validate if coordinates are valid WGS84
     */
    fun isValidWgs84(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    /**
     * Convert Android Location to our GpsCoordinate (ensuring WGS84)
     */
    fun fromAndroidLocation(location: android.location.Location): GpsCoordinate {
        // Android Location is always in WGS84
        return GpsCoordinate(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else 0.0
        )
    }

    /**
     * Convert to Google Maps LatLng (WGS84 compatible)
     */
    fun toLatLng(gps: GpsCoordinate): com.google.android.gms.maps.model.LatLng {
        return com.google.android.gms.maps.model.LatLng(gps.latitude, gps.longitude)
    }

    /**
     * Convert from Google Maps LatLng to our GpsCoordinate
     */
    fun fromLatLng(latLng: com.google.android.gms.maps.model.LatLng, altitude: Double = 0.0): GpsCoordinate {
        return GpsCoordinate(latLng.latitude, latLng.longitude, altitude)
    }
}

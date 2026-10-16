package com.college.eventmanagement.util;

/**
 * Authoritative Geofencing & Geographic Distance Utility for CEMS.
 * Calculates Haversine spherical distance between geographic coordinates
 * and enforces the 300-meter attendance geofence radius.
 */
public final class GeoUtil {

    /**
     * Authoritative campus attendance geofencing radius in meters.
     */
    public static final double ATTENDANCE_GEOFENCE_RADIUS_METERS = 300.0;

    /**
     * Mean spherical Earth radius in meters (WGS84 mean radius ~6371km).
     */
    public static final double EARTH_RADIUS_METERS = 6371000.0;

    private GeoUtil() {}

    /**
     * Validates whether latitude and longitude are non-null, finite numbers
     * within standard spherical bounds (latitude [-90, 90], longitude [-180, 180]).
     */
    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        if (Double.isNaN(latitude) || Double.isInfinite(latitude)
                || Double.isNaN(longitude) || Double.isInfinite(longitude)) {
            return false;
        }
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    /**
     * Calculates the great-circle distance between two geographic points in meters
     * using the Haversine formula with antimeridian (international date line) normalization.
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance in meters
     */
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) {
            throw new IllegalArgumentException("Invalid geographic coordinates provided for distance calculation.");
        }

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);

        // Normalize delta lambda to [-PI, PI] to handle antimeridian crossing correctly
        double deltaLambda = Math.toRadians(lon2 - lon1);
        while (deltaLambda > Math.PI) deltaLambda -= 2 * Math.PI;
        while (deltaLambda < -Math.PI) deltaLambda += 2 * Math.PI;

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);

        // Guard against numerical inaccuracies outside [0, 1]
        a = Math.min(1.0, Math.max(0.0, a));

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Determines whether a participant's location is within the authoritative 300-meter
     * attendance geofence radius of a venue location.
     */
    public static boolean isWithinGeofence(double venueLat, double venueLon, double participantLat, double participantLon) {
        double distance = calculateDistanceMeters(venueLat, venueLon, participantLat, participantLon);
        return distance <= ATTENDANCE_GEOFENCE_RADIUS_METERS;
    }
}

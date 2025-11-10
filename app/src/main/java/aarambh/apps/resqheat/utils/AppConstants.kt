package aarambh.apps.resqheat.utils

/**
 * Application-wide constants
 */
object AppConstants {
    // Notification settings
    const val NOTIFICATION_RADIUS_KM = 50.0
    
    // Map settings
    const val DEFAULT_MAP_ZOOM = 14f
    const val DEFAULT_MAP_LAT = 20.5937  // India center
    const val DEFAULT_MAP_LNG = 78.9629
    const val INITIAL_MAP_ZOOM = 4f
    
    // Priority weights for heatmap
    const val HIGH_PRIORITY_WEIGHT = 3.0
    const val MEDIUM_PRIORITY_WEIGHT = 2.0
    const val LOW_PRIORITY_WEIGHT = 1.0
    
    // Validation constants
    const val MIN_TITLE_LENGTH = 3
    const val MAX_TITLE_LENGTH = 100
    const val MAX_NOTES_LENGTH = 500
    const val MAX_ADDRESS_LENGTH = 200
    const val MAX_NAME_LENGTH = 100
    const val MAX_PHONE_LENGTH = 20
    const val MIN_PHONE_LENGTH = 10
    
    // Coordinate validation
    const val MIN_LATITUDE = -90.0
    const val MAX_LATITUDE = 90.0
    const val MIN_LONGITUDE = -180.0
    const val MAX_LONGITUDE = 180.0
    const val INVALID_COORDINATE_THRESHOLD = 0.0001  // Consider 0.0 as invalid if less than this
    
    // Earth radius for distance calculations (km)
    const val EARTH_RADIUS_KM = 6371.0
}


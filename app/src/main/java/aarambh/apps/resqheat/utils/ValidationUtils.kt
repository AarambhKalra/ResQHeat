package aarambh.apps.resqheat.utils

import java.util.regex.Pattern

/**
 * Utility functions for input validation
 */
object ValidationUtils {
    
    // Phone number patterns (supports various formats)
    private val PHONE_PATTERN = Pattern.compile(
        "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$"
    )
    
    // Email pattern
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
    
    /**
     * Validates a phone number
     * @param phone Phone number to validate
     * @return Validation result with error message if invalid
     */
    fun validatePhone(phone: String): ValidationResult {
        if (phone.isBlank()) {
            return ValidationResult(false, "Phone number is required")
        }
        
        val cleanPhone = phone.replace("\\s".toRegex(), "")
        if (cleanPhone.length < AppConstants.MIN_PHONE_LENGTH) {
            return ValidationResult(
                false, 
                "Phone number must be at least ${AppConstants.MIN_PHONE_LENGTH} digits"
            )
        }
        
        if (cleanPhone.length > AppConstants.MAX_PHONE_LENGTH) {
            return ValidationResult(
                false,
                "Phone number must be at most ${AppConstants.MAX_PHONE_LENGTH} characters"
            )
        }
        
        // Basic validation - check if it contains mostly digits and common phone characters
        val digitCount = cleanPhone.count { it.isDigit() }
        if (digitCount < AppConstants.MIN_PHONE_LENGTH) {
            return ValidationResult(false, "Phone number must contain at least ${AppConstants.MIN_PHONE_LENGTH} digits")
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validates an email address
     * @param email Email to validate
     * @return Validation result with error message if invalid
     */
    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, "Email is required")
        }
        
        if (email.length > 254) { // RFC 5321 limit
            return ValidationResult(false, "Email address is too long")
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult(false, "Invalid email format")
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validates a title
     * @param title Title to validate
     * @return Validation result with error message if invalid
     */
    fun validateTitle(title: String): ValidationResult {
        if (title.isBlank()) {
            return ValidationResult(false, "Title is required")
        }
        
        val trimmed = title.trim()
        if (trimmed.length < AppConstants.MIN_TITLE_LENGTH) {
            return ValidationResult(
                false,
                "Title must be at least ${AppConstants.MIN_TITLE_LENGTH} characters"
            )
        }
        
        if (trimmed.length > AppConstants.MAX_TITLE_LENGTH) {
            return ValidationResult(
                false,
                "Title must be at most ${AppConstants.MAX_TITLE_LENGTH} characters"
            )
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validates notes/text content
     * @param notes Notes to validate
     * @param required Whether the field is required
     * @return Validation result with error message if invalid
     */
    fun validateNotes(notes: String, required: Boolean = false): ValidationResult {
        if (required && notes.isBlank()) {
            return ValidationResult(false, "Notes are required")
        }
        
        if (notes.length > AppConstants.MAX_NOTES_LENGTH) {
            return ValidationResult(
                false,
                "Notes must be at most ${AppConstants.MAX_NOTES_LENGTH} characters"
            )
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validates a name
     * @param name Name to validate
     * @return Validation result with error message if invalid
     */
    fun validateName(name: String): ValidationResult {
        if (name.isBlank()) {
            return ValidationResult(false, "Name is required")
        }
        
        val trimmed = name.trim()
        if (trimmed.length < 2) {
            return ValidationResult(false, "Name must be at least 2 characters")
        }
        
        if (trimmed.length > AppConstants.MAX_NAME_LENGTH) {
            return ValidationResult(
                false,
                "Name must be at most ${AppConstants.MAX_NAME_LENGTH} characters"
            )
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validates an address
     * @param address Address to validate
     * @param required Whether the field is required
     * @return Validation result with error message if invalid
     */
    fun validateAddress(address: String, required: Boolean = false): ValidationResult {
        if (required && address.isBlank()) {
            return ValidationResult(false, "Address is required")
        }
        
        if (address.length > AppConstants.MAX_ADDRESS_LENGTH) {
            return ValidationResult(
                false,
                "Address must be at most ${AppConstants.MAX_ADDRESS_LENGTH} characters"
            )
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validates latitude coordinate
     * @param lat Latitude to validate
     * @return Validation result with error message if invalid
     */
    fun validateLatitude(lat: Double): ValidationResult {
        if (lat < AppConstants.MIN_LATITUDE || lat > AppConstants.MAX_LATITUDE) {
            return ValidationResult(false, "Latitude must be between ${AppConstants.MIN_LATITUDE} and ${AppConstants.MAX_LATITUDE}")
        }
        
        // Check if coordinate is effectively zero (invalid)
        if (kotlin.math.abs(lat) < AppConstants.INVALID_COORDINATE_THRESHOLD) {
            return ValidationResult(false, "Invalid latitude coordinate")
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validates longitude coordinate
     * @param lng Longitude to validate
     * @return Validation result with error message if invalid
     */
    fun validateLongitude(lng: Double): ValidationResult {
        if (lng < AppConstants.MIN_LONGITUDE || lng > AppConstants.MAX_LONGITUDE) {
            return ValidationResult(false, "Longitude must be between ${AppConstants.MIN_LONGITUDE} and ${AppConstants.MAX_LONGITUDE}")
        }
        
        // Check if coordinate is effectively zero (invalid)
        if (kotlin.math.abs(lng) < AppConstants.INVALID_COORDINATE_THRESHOLD) {
            return ValidationResult(false, "Invalid longitude coordinate")
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validates both latitude and longitude coordinates
     * @param lat Latitude to validate
     * @param lng Longitude to validate
     * @return Validation result with error message if invalid
     */
    fun validateCoordinates(lat: Double, lng: Double): ValidationResult {
        val latResult = validateLatitude(lat)
        if (!latResult.isValid) {
            return latResult
        }
        
        val lngResult = validateLongitude(lng)
        if (!lngResult.isValid) {
            return lngResult
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validates estimated days covered
     * @param days Days to validate
     * @return Validation result with error message if invalid
     */
    fun validateEstimatedDays(days: Int?): ValidationResult {
        if (days == null) {
            return ValidationResult(true, null) // Optional field
        }
        
        if (days < 0) {
            return ValidationResult(false, "Estimated days cannot be negative")
        }
        
        if (days > 365) {
            return ValidationResult(false, "Estimated days cannot exceed 365")
        }
        
        return ValidationResult(true, null)
    }
}

/**
 * Result of validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)


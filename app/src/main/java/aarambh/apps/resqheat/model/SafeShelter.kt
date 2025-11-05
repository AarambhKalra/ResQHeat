package aarambh.apps.resqheat.model

data class SafeShelter(
    val id: String = "",
    val name: String = "",
    val address: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val capacity: Int = 0,
    val currentOccupancy: Int = 0,
    val availableSpots: Int = 0,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val facilities: List<String> = emptyList(), // e.g., ["Food", "Medical", "Water"]
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val availabilityText: String
        get() = "$availableSpots / $capacity"
}


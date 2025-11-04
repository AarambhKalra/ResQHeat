package aarambh.apps.resqheat.model

enum class RequestType {
    RESCUE,
    RESOURCE
}

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}

enum class RequestStatus {
    NOT_SERVED,
    BEING_SERVED,
    SERVED
}

data class Request(
    val id: String = "",
    val type: RequestType = RequestType.RESOURCE,
    val resourceType: String? = null,
    val title: String = "",
    val notes: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val priority: Priority = Priority.MEDIUM,
    val status: RequestStatus = RequestStatus.NOT_SERVED,
    val createdByUid: String = "",
    val claimedBy: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val estimatedDaysCovered: Int? = null
)



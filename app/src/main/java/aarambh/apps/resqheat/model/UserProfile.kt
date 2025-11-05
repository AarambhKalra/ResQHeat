package aarambh.apps.resqheat.model

enum class UserRole {
    NGO_ORG,
    VICTIM
}

data class UserProfile(
    val uid: String = "",
    val role: UserRole = UserRole.VICTIM,
    val displayName: String? = null,
    val victimName: String? = null,
    val victimPhone: String? = null,
    val ngoOrgName: String? = null,
    val ngoOrgPhone: String? = null,
    val address: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)



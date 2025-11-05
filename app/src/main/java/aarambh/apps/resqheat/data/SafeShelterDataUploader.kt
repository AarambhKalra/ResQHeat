package aarambh.apps.resqheat.data

import aarambh.apps.resqheat.model.SafeShelter
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper class to upload 3 example safe shelter data entries to Firestore.
 * 
 * Usage (from MainActivity):
 * 
 * Option 1: Add to MainActivity.onCreate() temporarily:
 * ```kotlin
 * import aarambh.apps.resqheat.data.SafeShelterDataUploader
 * 
 * lifecycleScope.launch {
 *     val uploader = SafeShelterDataUploader()
 *     uploader.uploadExampleShelters()
 * }
 * ```
 * 
 * Option 2: Add a one-time LaunchedEffect in MainActivity.setContent:
 * ```kotlin
 * androidx.compose.runtime.LaunchedEffect(Unit) {
 *     // Uncomment the line below to upload 3 example shelters
 *     // SafeShelterDataUploader().uploadExampleShelters()
 * }
 * ```
 * 
 * After uploading, remove the upload code to avoid duplicate entries.
 */
class SafeShelterDataUploader(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collectionName = "safeShelters"

    /**
     * Uploads example safe shelter data to Firestore.
     * This can be called from the app or removed after initial setup.
     */
    suspend fun uploadExampleShelters() {
        val exampleShelters = getExampleShelters()
        
        try {
            Log.d("SafeShelterDataUploader", "Starting upload of ${exampleShelters.size} shelters to collection: $collectionName")
            
            exampleShelters.forEachIndexed { index, shelter ->
                try {
                    val docRef = firestore.collection(collectionName).document()
                    val nowMillis = System.currentTimeMillis()
                    // Create a map without the 'id' field (id is the document ID, not a field)
                    val shelterData = mapOf(
                        "name" to shelter.name,
                        "address" to (shelter.address ?: ""),
                        "lat" to shelter.lat,
                        "lng" to shelter.lng,
                        "capacity" to shelter.capacity,
                        "currentOccupancy" to shelter.currentOccupancy,
                        "availableSpots" to shelter.availableSpots,
                        "contactPhone" to (shelter.contactPhone ?: ""),
                        "contactEmail" to (shelter.contactEmail ?: ""),
                        "facilities" to shelter.facilities,
                        "isActive" to shelter.isActive,
                        "createdAt" to (if (shelter.createdAt == 0L) nowMillis else shelter.createdAt),
                        "updatedAt" to nowMillis
                    )
                    
                    Log.d("SafeShelterDataUploader", "Uploading shelter ${index + 1}/${exampleShelters.size}: ${shelter.name}")
                    Log.d("SafeShelterDataUploader", "Shelter data: $shelterData")
                    docRef.set(shelterData).await()
                    Log.d("SafeShelterDataUploader", "✓ Successfully uploaded: ${shelter.name} (ID: ${docRef.id})")
                } catch (e: Exception) {
                    Log.e("SafeShelterDataUploader", "Failed to upload shelter: ${shelter.name}", e)
                    e.printStackTrace()
                    throw e
                }
            }
            Log.d("SafeShelterDataUploader", "✓ Successfully uploaded all ${exampleShelters.size} shelters")
        } catch (e: Exception) {
            Log.e("SafeShelterDataUploader", "Failed to upload shelters", e)
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Returns 3 example safe shelter data entries.
     * Modify these coordinates and details as needed for your region.
     */
    private fun getExampleShelters(): List<SafeShelter> {
        val now = System.currentTimeMillis()
        
        return listOf(
            SafeShelter(
                name = "Community Center - Downtown",
                address = "123 Main Street, Downtown Area",
                lat = 28.6139, // Example: New Delhi coordinates - replace with actual locations
                lng = 77.2090,
                capacity = 200,
                currentOccupancy = 45,
                availableSpots = 155,
                contactPhone = "+91-11-12345678",
                contactEmail = "downtown-shelter@example.com",
                facilities = listOf("Food", "Medical", "Water", "Sanitation", "Beds"),
                isActive = true,
                createdAt = now,
                updatedAt = now
            ),
            SafeShelter(
                name = "Emergency Shelter - North Zone",
                address = "456 Park Avenue, North Zone",
                lat = 28.7041,
                lng = 77.1025,
                capacity = 150,
                currentOccupancy = 30,
                availableSpots = 120,
                contactPhone = "+91-11-23456789",
                contactEmail = "north-shelter@example.com",
                facilities = listOf("Food", "Water", "Medical", "Beds"),
                isActive = true,
                createdAt = now,
                updatedAt = now
            ),
            SafeShelter(
                name = "Temporary Relief Camp - South Zone",
                address = "789 High Street, South Zone",
                lat = 28.5245,
                lng = 77.1855,
                capacity = 100,
                currentOccupancy = 25,
                availableSpots = 75,
                contactPhone = "+91-11-34567890",
                contactEmail = "south-shelter@example.com",
                facilities = listOf("Food", "Water", "Beds"),
                isActive = true,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}


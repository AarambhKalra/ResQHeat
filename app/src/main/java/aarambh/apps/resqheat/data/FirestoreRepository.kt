package aarambh.apps.resqheat.data

import aarambh.apps.resqheat.model.Request
import aarambh.apps.resqheat.model.RequestStatus
import aarambh.apps.resqheat.model.SafeShelter
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val collectionName: String = "requests"

    suspend fun createRequest(request: Request): String {
        val docRef = if (request.id.isBlank()) {
            firestore.collection(collectionName).document()
        } else {
            firestore.collection(collectionName).document(request.id)
        }

        val nowMillis = System.currentTimeMillis()
        val requestToSave = request.copy(
            id = docRef.id,
            createdAt = if (request.createdAt == 0L) nowMillis else request.createdAt,
            updatedAt = nowMillis
        )

        try {
            docRef.set(requestToSave).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "createRequest failed", e)
            throw e
        }
        return docRef.id
    }

    fun listenToAllRequests(callback: (List<Request>) -> Unit, onError: (Exception) -> Unit = {}): ListenerRegistration {
        return firestore.collection(collectionName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirestoreRepository", "listenToAllRequests error", e)
                    onError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    callback(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot.documents.mapNotNull { it.toObject<Request>() }
                callback(items)
            }
    }

    suspend fun claimRequest(requestId: String, ngoId: String, ngoName: String?, ngoPhone: String?, eta: String? = null) {
        val docRef = firestore.collection(collectionName).document(requestId)
        val updates = mutableMapOf<String, Any>(
            "claimedBy" to ngoId,
            "status" to RequestStatus.BEING_SERVED.name,
            "updatedAt" to System.currentTimeMillis()
        )
        if (ngoName != null) {
            updates["claimedByNgoName"] = ngoName
        }
        if (ngoPhone != null) {
            updates["claimedByNgoPhone"] = ngoPhone
        }
        if (eta != null) {
            updates["eta"] = eta
        }
        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "claimRequest failed", e)
            throw e
        }
    }

    suspend fun completeRequest(requestId: String, estimatedDaysCovered: Int? = null) {
        val docRef = firestore.collection(collectionName).document(requestId)
        val updates = mutableMapOf<String, Any>(
            "status" to RequestStatus.SERVED.name,
            "updatedAt" to System.currentTimeMillis()
        )
        if (estimatedDaysCovered != null) {
            updates["estimatedDaysCovered"] = estimatedDaysCovered
        }
        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "completeRequest failed", e)
            throw e
        }
    }

    // Safe Shelters methods
    private val sheltersCollectionName: String = "safeShelters"

    fun listenToAllShelters(callback: (List<SafeShelter>) -> Unit, onError: (Exception) -> Unit = {}): ListenerRegistration {
        return firestore.collection(sheltersCollectionName)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirestoreRepository", "listenToAllShelters error", e)
                    onError(e)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    callback(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        val shelter = doc.toObject<SafeShelter>()
                        if (shelter == null) {
                            Log.w("FirestoreRepository", "Failed to parse shelter document: ${doc.id}")
                            Log.w("FirestoreRepository", "Document data: ${doc.data}")
                            null
                        } else {
                            Log.d("FirestoreRepository", "Parsed shelter: ${shelter.name}, lat=${shelter.lat}, lng=${shelter.lng}, active=${shelter.isActive}")
                            shelter.copy(id = doc.id)
                        }
                    } catch (e: Exception) {
                        Log.e("FirestoreRepository", "Error parsing shelter document: ${doc.id}", e)
                        null
                    }
                }
                Log.d("FirestoreRepository", "Returning ${items.size} shelters from listener")
                callback(items)
            }
    }

    suspend fun getAllShelters(): List<SafeShelter> {
        return try {
            val snapshot = firestore.collection(sheltersCollectionName)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val shelter = doc.toObject<SafeShelter>()
                shelter?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "getAllShelters failed", e)
            emptyList()
        }
    }
}



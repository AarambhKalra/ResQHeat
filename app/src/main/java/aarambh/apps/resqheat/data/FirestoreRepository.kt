package aarambh.apps.resqheat.data

import aarambh.apps.resqheat.model.Request
import aarambh.apps.resqheat.model.RequestStatus
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

    suspend fun claimRequest(requestId: String, ngoId: String) {
        val docRef = firestore.collection(collectionName).document(requestId)
        val updates = mapOf(
            "claimedBy" to ngoId,
            "status" to RequestStatus.BEING_SERVED.name,
            "updatedAt" to System.currentTimeMillis()
        )
        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "claimRequest failed", e)
            throw e
        }
    }

    suspend fun completeRequest(requestId: String) {
        val docRef = firestore.collection(collectionName).document(requestId)
        val updates = mapOf(
            "status" to RequestStatus.SERVED.name,
            "updatedAt" to System.currentTimeMillis()
        )
        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "completeRequest failed", e)
            throw e
        }
    }
}



package aarambh.apps.resqheat.data

import aarambh.apps.resqheat.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val collectionName: String = "users"

    suspend fun getUserProfile(uid: String): UserProfile? {
        val snap = firestore.collection(collectionName).document(uid).get().await()
        return snap.toObject<UserProfile>()
    }

    suspend fun setUserProfile(profile: UserProfile) {
        val now = System.currentTimeMillis()
        val doc = firestore.collection(collectionName).document(profile.uid)
        val toSave = profile.copy(
            createdAt = if (profile.createdAt == 0L) now else profile.createdAt,
            updatedAt = now
        )
        doc.set(toSave).await()
    }
}



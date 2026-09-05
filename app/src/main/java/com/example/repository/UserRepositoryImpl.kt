package com.example.repository

import android.util.Log
import com.example.util.getDocumentServerFirst
import com.example.util.getQuerySnapshotServerFirst
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface UserRepository {
    suspend fun createUserProfile(uid: String, email: String, name: String? = null, plenxoId: String? = null): Boolean

    suspend fun syncUserDataOnAuth(
        uid: String,
        email: String,
        displayName: String? = null,
        photoUrl: String? = null,
        fcmToken: String? = null,
        status: String = "online"
    ): Boolean

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Boolean
    suspend fun updateUserStatus(uid: String, status: String, lastSeen: Long = System.currentTimeMillis()): Boolean
    suspend fun updateFcmToken(uid: String, token: String): Boolean
    fun observeUserData(uid: String): Flow<Map<String, Any>?>
    suspend fun getUserData(uid: String): Map<String, Any>?

    /**
     * Strictly searches the Firestore 'users' collection by 'plenxoId' field ONLY.
     * Completely disables/removes email searching capability.
     */
    suspend fun searchUsersByPlenxoId(plenxoIdQuery: String): List<Map<String, Any>>

    /**
     * Repository function to search user by exact Plenxo ID using whereEqualTo("plenxoId", plenxoId).
     */
    suspend fun searchUserByPlenxoId(plenxoId: String): List<Map<String, Any>>

    /**
     * Resolves a Plenxo ID to its linked user email address for authentication purposes.
     */
    suspend fun getEmailByPlenxoId(plenxoId: String): String?

    /**
     * Fetches user document by Plenxo ID.
     */
    suspend fun getUserByPlenxoId(plenxoId: String): Map<String, Any>?
}

class UserRepositoryImpl : UserRepository {

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    override suspend fun createUserProfile(uid: String, email: String, name: String?, plenxoId: String?): Boolean {
        if (uid.isBlank()) return false

        // Check if user document already exists before attempting creation
        val userDocRef = firestore.collection("users").document(uid)
        val existingSnap = try { getDocumentServerFirst(userDocRef, timeoutMs = 6000L) } catch (e: Exception) { null }

        if (existingSnap != null && existingSnap.exists()) {
            val existingDisplayName = existingSnap.getString("displayName")?.takeIf { it.isNotBlank() && it != "User" }
                ?: existingSnap.getString("name")?.takeIf { it.isNotBlank() && it != "User" }
            if (!existingDisplayName.isNullOrBlank()) {
                Log.d("UserRepositoryImpl", "User $uid already exists with profile ($existingDisplayName). Preserving existing profile.")
                return true
            }
        }

        val now = System.currentTimeMillis()
        val resolvedName = name?.takeIf { it.isNotBlank() && it != "User" } 
            ?: existingSnap?.getString("displayName")?.takeIf { it.isNotBlank() && it != "User" }
            ?: existingSnap?.getString("name")?.takeIf { it.isNotBlank() && it != "User" }
            ?: email.substringBefore("@").ifBlank { "User" }

        val finalPxId = plenxoId?.takeIf { it.startsWith("PX-") } 
            ?: existingSnap?.getString("plenxoId")?.takeIf { it.startsWith("PX-") }
            ?: com.example.model.resolveOrCreatePlenxoId(uid, firestore)
        val numericCode = finalPxId.removePrefix("PX-")

        val userData = mutableMapOf<String, Any?>(
            "uid" to uid,
            "id" to uid,
            "email" to email,
            "displayName" to resolvedName,
            "plenxoId" to finalPxId,
            "userCode" to numericCode,
            "status" to "online",
            "lastSeen" to now,
            "createdAt" to now,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            userDocRef.set(userData, SetOptions.merge()).await()
            Log.d("UserRepositoryImpl", "User profile created successfully for $uid with PX ID: $finalPxId")
            true
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed creating user profile for $uid: ${e.message}", e)
            false
        }
    }

    override suspend fun syncUserDataOnAuth(
        uid: String,
        email: String,
        displayName: String?,
        photoUrl: String?,
        fcmToken: String?,
        status: String
    ): Boolean {
        if (uid.isBlank()) return false

        val userDocRef = firestore.collection("users").document(uid)
        val userSnap = try { getDocumentServerFirst(userDocRef, timeoutMs = 6000L) } catch (e: Exception) { null }

        if (userSnap != null && userSnap.exists()) {
            Log.d("UserRepositoryImpl", "Returning user $uid already exists. Skipping profile overwrite during auth sync.")
            try {
                val updates = mutableMapOf<String, Any>(
                    "status" to status,
                    "lastSeen" to System.currentTimeMillis()
                )
                if (!fcmToken.isNullOrBlank()) {
                    updates["fcmToken"] = fcmToken
                }
                userDocRef.update(updates).await()
            } catch (e: Exception) {
                Log.w("UserRepositoryImpl", "Non-profile update warning: ${e.message}")
            }
            return true
        } else {
            return createUserProfile(uid, email, displayName, null)
        }
    }

    override suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Boolean {
        if (uid.isBlank()) return false
        val mutableUpdates = updates.toMutableMap()
        mutableUpdates["updatedAt"] = FieldValue.serverTimestamp()

        return try {
            firestore.collection("users").document(uid)
                .set(mutableUpdates, SetOptions.merge())
                .await()

            Log.d("UserRepositoryImpl", "Updated profile for user $uid")
            true
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed to update user profile for $uid: ${e.message}", e)
            false
        }
    }

    override suspend fun updateUserStatus(uid: String, status: String, lastSeen: Long): Boolean {
        if (uid.isBlank()) return false
        val updates = mapOf(
            "status" to status,
            "lastSeen" to lastSeen,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return updateUserProfile(uid, updates)
    }

    override suspend fun updateFcmToken(uid: String, token: String): Boolean {
        if (uid.isBlank() || token.isBlank()) return false
        val updates = mapOf(
            "fcmToken" to token,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return updateUserProfile(uid, updates)
    }

    override fun observeUserData(uid: String): Flow<Map<String, Any>?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection("users").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UserRepositoryImpl", "Error observing user data: ${error.message}")
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.data)
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun getUserData(uid: String): Map<String, Any>? {
        if (uid.isBlank()) return null
        return try {
            val docRef = firestore.collection("users").document(uid)
            val snapshot = getDocumentServerFirst(docRef)
            if (snapshot.exists()) snapshot.data else null
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed to fetch user data for $uid: ${e.message}")
            null
        }
    }

    /**
     * STRICT PLENXO ID SEARCH ONLY:
     * Queries Firestore 'users' collection strictly using whereEqualTo("plenxoId", cleanedQuery) with resilient server-first read.
     */
    override suspend fun searchUsersByPlenxoId(plenxoIdQuery: String): List<Map<String, Any>> {
        val cleanInput = plenxoIdQuery.trim().removePrefix("@").lowercase()
        if (cleanInput.isBlank()) return emptyList()

        return try {
            val numericPart = cleanInput.removePrefix("px-")
            val formatted = "PX-$numericPart"

            Log.d("UserRepositoryImpl", "Querying users collection by plenxoId: $cleanInput ($formatted)")
            val resultsList = mutableListOf<Map<String, Any>>()

            var snapshot = try {
                getQuerySnapshotServerFirst(firestore.collection("users").whereEqualTo("plenxoId", cleanInput))
            } catch (e: Exception) {
                null
            }

            if (snapshot == null || snapshot.isEmpty) {
                snapshot = try {
                    getQuerySnapshotServerFirst(firestore.collection("users").whereEqualTo("plenxoId", formatted))
                } catch (e: Exception) {
                    null
                }
            }
            if (snapshot == null || snapshot.isEmpty) {
                snapshot = try {
                    getQuerySnapshotServerFirst(firestore.collection("users").whereEqualTo("plenxoId", formatted.lowercase()))
                } catch (e: Exception) {
                    null
                }
            }

            snapshot?.documents?.forEach { doc ->
                val data = doc.data?.toMutableMap() ?: return@forEach
                data["docId"] = doc.id
                data["uid"] = (data["uid"] as? String) ?: doc.id
                resultsList.add(data)
            }

            // Normalize all result maps strictly using plenxoId
            resultsList.map { rawMap ->
                val norm = rawMap.toMutableMap()
                val uid = (norm["uid"] as? String) ?: (norm["docId"] as? String) ?: ""
                norm["uid"] = uid
                norm["docId"] = uid
                val pId = (norm["plenxoId"] as? String) ?: (norm["userCode"] as? String) ?: formatted
                norm["plenxoId"] = pId
                val dName = (norm["displayName"] as? String) ?: (norm["name"] as? String) ?: (norm["fullName"] as? String) ?: "Plenxo User"
                norm["displayName"] = dName
                norm["name"] = dName
                val pic = (norm["profilePicUrl"] as? String) ?: (norm["profile_pic_url"] as? String) ?: (norm["photoUrl"] as? String) ?: ""
                norm["profilePicUrl"] = pic
                val bio = (norm["bio"] as? String) ?: (norm["statusMessage"] as? String) ?: ""
                norm["bio"] = bio
                norm
            }
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error searching users strictly by plenxoId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun searchUserByPlenxoId(plenxoId: String): List<Map<String, Any>> {
        val cleanId = plenxoId.trim().lowercase()
        if (cleanId.isBlank()) return emptyList()
        return try {
            val query = firestore.collection("users").whereEqualTo("plenxoId", cleanId)
            val querySnapshot = getQuerySnapshotServerFirst(query)
            querySnapshot.documents.mapNotNull { doc ->
                doc.data?.toMutableMap()?.apply {
                    put("docId", doc.id)
                    put("uid", get("uid") as? String ?: doc.id)
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error searching user by plenxoId $cleanId: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getEmailByPlenxoId(plenxoId: String): String? {
        val userMap = getUserByPlenxoId(plenxoId)
        return userMap?.get("email") as? String
    }

    override suspend fun getUserByPlenxoId(plenxoId: String): Map<String, Any>? {
        val cleaned = plenxoId.trim().lowercase()
        if (cleaned.isBlank()) return null

        return try {
            val query1 = firestore.collection("users").whereEqualTo("plenxoId", cleaned).limit(1)
            var snapshot = try {
                getQuerySnapshotServerFirst(query1)
            } catch (e: Exception) {
                null
            }

            if (snapshot == null || snapshot.isEmpty) {
                val formatted = "PX-${cleaned.removePrefix("px-")}"
                val query2 = firestore.collection("users").whereEqualTo("plenxoId", formatted).limit(1)
                snapshot = try {
                    getQuerySnapshotServerFirst(query2)
                } catch (e: Exception) {
                    null
                }
            }

            if (snapshot != null && !snapshot.isEmpty) {
                snapshot.documents[0].data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed to fetch user by plenxoId '$cleaned': ${e.message}", e)
            null
        }
    }
}

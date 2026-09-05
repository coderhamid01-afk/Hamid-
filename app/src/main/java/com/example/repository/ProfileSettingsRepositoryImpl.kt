@file:Suppress("DEPRECATION")
package com.example.repository

import android.util.Log
import com.example.model.UserProfileDomainModel
import com.example.util.getDocumentServerFirst
import com.example.util.getQuerySnapshotServerFirst
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

class ProfileSettingsRepositoryImpl : ProfileSettingsRepository {

    private val firebaseAuth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private fun parseSnapshotToDomainModel(snapshot: DocumentSnapshot, targetId: String): UserProfileDomainModel {
        val data = snapshot.data ?: emptyMap()
        val currentEmail = firebaseAuth.currentUser?.email
        val resolvedDisplayName = (data["displayName"] as? String)?.takeIf { it.isNotBlank() && it != "User" }
            ?: (data["name"] as? String)?.takeIf { it.isNotBlank() && it != "User" }
            ?: (data["display_name"] as? String)?.takeIf { it.isNotBlank() && it != "User" }
            ?: (data["current_name"] as? String)?.takeIf { it.isNotBlank() && it != "User" }
            ?: (data["fullName"] as? String)?.takeIf { it.isNotBlank() && it != "User" }
            ?: (data["full_name"] as? String)?.takeIf { it.isNotBlank() && it != "User" }
            ?: (data["username"] as? String)?.takeIf { it.isNotBlank() && it != "User" }
            ?: firebaseAuth.currentUser?.displayName?.takeIf { it.isNotBlank() && it != "User" }
            ?: (data["displayName"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["name"] as? String)?.takeIf { it.isNotBlank() }
            ?: "User"

        val resolvedBio = (data["bio"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["statusMessage"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["bioStatus"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["bio_status"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["status_message"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["current_bio"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["about"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["status"] as? String)?.takeIf { it.isNotBlank() }
            ?: ""

        val resolvedPicUrl = (data["profilePicUrl"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["profilePic"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["avatarUrl"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["avatar_url"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["photoUrl"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["profileUrl"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["current_profile_pic_url"] as? String)?.takeIf { it.isNotBlank() }
            ?: firebaseAuth.currentUser?.photoUrl?.toString()
            ?: ""

        val resolvedEmail = (data["email"] as? String)?.takeIf { it.isNotBlank() }
            ?: currentEmail
            ?: ""

        val resolvedPlenxoId = (data["plenxoId"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["plenxo_id"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["px_id"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["userCode"] as? String)?.takeIf { it.isNotBlank() }
            ?: ""

        return UserProfileDomainModel(
            id = snapshot.id,
            userId = targetId,
            displayName = resolvedDisplayName,
            name = resolvedDisplayName,
            email = resolvedEmail,
            statusMessage = resolvedBio,
            bio = resolvedBio,
            bioStatus = resolvedBio,
            profilePicUrl = resolvedPicUrl,
            profileUrl = resolvedPicUrl,
            userCode = resolvedPlenxoId,
            plenxoId = resolvedPlenxoId,
            selectedRingId = (data["selectedRingId"] as? String) ?: "",
            profileRingId = (data["profileRingId"] as? String) ?: ""
        )
    }

    override fun getProfileFlow(userId: String): Flow<UserProfileDomainModel?> = callbackFlow {
        val fbUid = firebaseAuth.currentUser?.uid ?: ""
        val sessionToken = try { com.example.util.SessionManager.getLoginState(com.example.PlenxoApplication.instance).token ?: "" } catch (e: Exception) { "" }
        val sessionEmail = try { com.example.util.SessionManager.getLoginState(com.example.PlenxoApplication.instance).email?.replace(".", "_") ?: "" } catch (e: Exception) { "" }
        val fallbackUid = fbUid.ifEmpty { sessionToken.ifEmpty { sessionEmail } }
        val targetId = if (userId.isNotBlank()) userId else fallbackUid

        if (targetId.isEmpty()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val docRef = firestore.collection("users").document(targetId)

        // Resilient server-first fetch with automatic cache fallback
        launch {
            try {
                val serverSnap = getDocumentServerFirst(docRef)
                if (serverSnap.exists()) {
                    val profile = parseSnapshotToDomainModel(serverSnap, targetId)
                    if (profile.displayName.isNotBlank() && profile.displayName != "User") {
                        try {
                            com.example.util.SessionManager.saveUserProfileLocally(
                                com.example.PlenxoApplication.instance,
                                plenxoId = profile.plenxoId,
                                displayName = profile.displayName,
                                bio = profile.bio,
                                profilePicUrl = profile.profilePicUrl
                            )
                        } catch (_: Exception) {}
                    }
                    trySend(profile)
                }
            } catch (e: Exception) {
                Log.w("ProfileSettingsRepo", "Initial resilient fetch error for $targetId: ${e.message}")
            }
        }

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                val currentEmail = firebaseAuth.currentUser?.email
                if (!currentEmail.isNullOrBlank()) {
                    launch {
                        try {
                            val query = firestore.collection("users").whereEqualTo("email", currentEmail).limit(1)
                            val querySnap = getQuerySnapshotServerFirst(query)
                            if (!querySnap.isEmpty) {
                                val d = querySnap.documents[0]
                                val profile = parseSnapshotToDomainModel(d, targetId)
                                if (profile.displayName.isNotBlank() && profile.displayName != "User") {
                                    try {
                                        com.example.util.SessionManager.saveUserProfileLocally(
                                            com.example.PlenxoApplication.instance,
                                            plenxoId = profile.plenxoId,
                                            displayName = profile.displayName,
                                            bio = profile.bio,
                                            profilePicUrl = profile.profilePicUrl
                                        )
                                    } catch (_: Exception) {}
                                }
                                trySend(profile)
                            } else {
                                trySend(null)
                            }
                        } catch (e: Exception) {
                            trySend(null)
                        }
                    }
                } else {
                    trySend(null)
                }
                return@addSnapshotListener
            }

            val profile = parseSnapshotToDomainModel(snapshot, targetId)
            if (profile.displayName.isNotBlank() && profile.displayName != "User") {
                try {
                    com.example.util.SessionManager.saveUserProfileLocally(
                        com.example.PlenxoApplication.instance,
                        plenxoId = profile.plenxoId,
                        displayName = profile.displayName,
                        bio = profile.bio,
                        profilePicUrl = profile.profilePicUrl
                    )
                } catch (_: Exception) {}
            }
            trySend(profile)
        }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun updateProfile(
        userId: String,
        name: String,
        bio: String,
        profileUrl: String
    ) {
        val currentUid = firebaseAuth.currentUser?.uid ?: ""
        if (currentUid.isEmpty()) throw Exception("Not authenticated in Firebase Auth")
        val targetId = if (userId.isBlank()) currentUid else userId

        try {
            com.example.util.ProfileHistoryUtils.saveProfileWithHistory(
                uid = targetId,
                newName = name,
                newBio = bio,
                newProfileUrl = profileUrl,
                firestore = firestore,
                auth = firebaseAuth
            )

            val updates = mutableMapOf<String, Any>()
            if (name.isNotEmpty()) {
                updates["displayName"] = name
            }
            if (bio.isNotEmpty()) {
                updates["bio"] = bio
                updates["statusMessage"] = bio
            }
            if (profileUrl.isNotEmpty()) {
                updates["profilePicUrl"] = profileUrl
                updates["photoUrl"] = profileUrl
            }
            updates["updatedAt"] = System.currentTimeMillis()
            firebaseAuth.currentUser?.email?.let { updates["email"] = it }

            firestore.collection("users").document(targetId)
                .set(updates, SetOptions.merge())
                .await()

            val profileUpdateBuilder = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            if (name.isNotEmpty()) profileUpdateBuilder.setDisplayName(name)
            if (profileUrl.isNotEmpty() && (profileUrl.startsWith("http://") || profileUrl.startsWith("https://"))) {
                profileUpdateBuilder.setPhotoUri(android.net.Uri.parse(profileUrl))
            }
            firebaseAuth.currentUser?.updateProfile(profileUpdateBuilder.build())?.await()

            Log.d("ProfileSettingsRepo", "Profile updated successfully in Firestore users for $targetId")
        } catch (e: Exception) {
            Log.e("ProfileSettingsRepo", "Failed to update profile in Firestore: ${e.message}")
            throw e
        }
    }

    override suspend fun updateRing(userId: String, ringId: String) {
        val currentUid = firebaseAuth.currentUser?.uid ?: ""
        if (currentUid.isEmpty()) throw Exception("Not authenticated in Firebase Auth")
        val targetId = if (userId.isBlank()) currentUid else userId

        try {
            val updates = mapOf("selectedRingId" to ringId)
            firestore.collection("users").document(targetId)
                .set(updates, SetOptions.merge())
                .await()
            Log.d("ProfileSettingsRepo", "Selected ring updated in Firestore for $targetId")
        } catch (e: Exception) {
            Log.e("ProfileSettingsRepo", "Failed to update ring in Firestore: ${e.message}")
            throw e
        }
    }

    override suspend fun updateProfileRing(userId: String, ringId: String) {
        val currentUid = firebaseAuth.currentUser?.uid ?: ""
        if (currentUid.isEmpty()) throw Exception("Not authenticated in Firebase Auth")
        val targetId = if (userId.isBlank()) currentUid else userId

        try {
            val updates = mapOf(
                "profileRing" to ringId,
                "profileRingId" to ringId,
                "selectedRingId" to ringId,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(targetId)
                .set(updates, SetOptions.merge())
                .await()
            Log.d("ProfileSettingsRepo", "Profile ring updated in Firestore users for $targetId")
        } catch (e: Exception) {
            Log.e("ProfileSettingsRepo", "Failed to update profile ring in Firestore: ${e.message}")
            throw e
        }
    }

    override suspend fun uploadProfileImage(
        context: android.content.Context,
        uri: android.net.Uri
    ): String = withContext(kotlinx.coroutines.Dispatchers.IO) {
        Log.d("ProfileSettingsRepo", "Starting Catbox.moe profile picture upload...")
        val uploadedUrl = com.example.network.CatboxStorageManager.uploadImage(context, uri)
        Log.d("ProfileSettingsRepo", "Catbox upload completed successfully. CDN URL: $uploadedUrl")

        // Synchronize with Firestore 'users' collection
        val currentUid = firebaseAuth.currentUser?.uid ?: ""
        if (currentUid.isNotEmpty()) {
            Log.d("ProfileSettingsRepo", "Syncing profilePicUrl to Firestore 'users' collection for userId: $currentUid")
            firestore.collection("users").document(currentUid)
                .set(mapOf("profilePicUrl" to uploadedUrl, "avatar_url" to uploadedUrl, "photoUrl" to uploadedUrl), SetOptions.merge())
                .await()

            val req = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setPhotoUri(android.net.Uri.parse(uploadedUrl))
                .build()
            firebaseAuth.currentUser?.updateProfile(req)?.await()
            Log.d("ProfileSettingsRepo", "Firestore users document and FirebaseAuth updated successfully with new Catbox CDN URL.")
        } else {
            Log.w("ProfileSettingsRepo", "No active user session detected. Skipping database sync.")
        }

        return@withContext uploadedUrl
    }
}

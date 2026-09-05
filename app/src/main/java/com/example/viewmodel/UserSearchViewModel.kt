package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.UserRepository
import com.example.repository.UserRepositoryImpl
import com.example.util.getQuerySnapshotServerFirst
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserSearchViewModel @JvmOverloads constructor(
    application: Application,
    private val userRepository: UserRepository = UserRepositoryImpl()
) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val searchResults: StateFlow<List<Map<String, Any>>> = _searchResults.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query.take(20)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _hasSearched.value = false
        _searchError.value = null
    }

    fun executeSearch() {
        val rawInput = _searchQuery.value.trim().removePrefix("@").removePrefix("#").trim()
        if (rawInput.isBlank()) {
            _searchError.value = "Please enter Plenxo ID"
            return
        }

        val numericPart = rawInput.removePrefix("PX-").removePrefix("px-").removePrefix("Px-").trim()
        val formattedPxId = if (numericPart.isNotBlank()) "PX-$numericPart" else rawInput.uppercase()

        _isSearching.value = true
        _hasSearched.value = true
        _searchError.value = null

        viewModelScope.launch {
            try {
                val currentAuthUid = auth.currentUser?.uid ?: ""

                // Self search prevention
                val currentUserData = userRepository.getUserData(currentAuthUid)
                val myPlenxoId = ((currentUserData?.get("plenxoId") as? String) ?: "")
                    .trim()
                    .lowercase()

                val rawLower = rawInput.lowercase()
                val formattedLower = formattedPxId.lowercase()

                if (rawLower == myPlenxoId ||
                    formattedLower == myPlenxoId ||
                    (myPlenxoId.isNotBlank() && numericPart.isNotBlank() && numericPart == myPlenxoId.removePrefix("px-"))) {
                    _searchError.value = "You cannot add yourself"
                    _searchResults.value = emptyList()
                    _isSearching.value = false
                    return@launch
                }

                val list = mutableListOf<Map<String, Any>>()

                // Standardized Firestore query with resilient server-first read on plenxoId
                val searchKeys = mutableSetOf<String>()
                searchKeys.add(formattedPxId)
                searchKeys.add(formattedPxId.uppercase())
                searchKeys.add(formattedPxId.lowercase())
                searchKeys.add(rawInput)
                if (numericPart.isNotBlank()) {
                    searchKeys.add(numericPart)
                }

                for (key in searchKeys) {
                    val query = firestore.collection("users").whereEqualTo("plenxoId", key)
                    val snapshot = try {
                        getQuerySnapshotServerFirst(query, timeoutMs = 5000L)
                    } catch (e: Exception) {
                        Log.w("UserSearchViewModel", "Search query failed for key $key: ${e.message}")
                        null
                    } ?: continue

                    if (!snapshot.isEmpty) {
                        snapshot.documents.forEach { doc ->
                            val data = doc.data?.toMutableMap() ?: mutableMapOf()
                            val uid = doc.id
                            data["docId"] = uid
                            data["uid"] = (data["uid"] as? String) ?: uid

                            val dName = doc.getString("displayName")?.takeIf { it.isNotBlank() && it != "User" }
                                ?: doc.getString("name")?.takeIf { it.isNotBlank() && it != "User" }
                                ?: doc.getString("display_name")?.takeIf { it.isNotBlank() && it != "User" }
                                ?: doc.getString("fullName")?.takeIf { it.isNotBlank() && it != "User" }
                                ?: doc.getString("displayName")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("name")?.takeIf { it.isNotBlank() }
                                ?: "Plenxo User"
                            data["displayName"] = dName
                            data["name"] = dName

                            val pId = doc.getString("plenxoId")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("userCode")?.takeIf { it.isNotBlank() }
                                ?: formattedPxId
                            data["plenxoId"] = pId

                            val bio = doc.getString("bio")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("statusMessage")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("bioStatus")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("bio_status")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("current_bio")?.takeIf { it.isNotBlank() }
                                ?: ""
                            data["bio"] = bio
                            data["statusMessage"] = bio

                            val pic = doc.getString("profilePicUrl")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("avatar_url")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("photoUrl")?.takeIf { it.isNotBlank() }
                                ?: ""
                            data["profilePicUrl"] = pic

                            list.add(data)
                        }
                        break
                    }
                }

                if (list.isEmpty()) {
                    val repoResults = userRepository.searchUsersByPlenxoId(formattedPxId)
                    if (repoResults.isNotEmpty()) {
                        repoResults.forEach { list.add(it) }
                    } else if (numericPart.isNotBlank()) {
                        val numericResults = userRepository.searchUsersByPlenxoId(numericPart)
                        numericResults.forEach { list.add(it) }
                    }
                }

                val filteredResults = list
                    .distinctBy { (it["uid"] as? String) ?: (it["id"] as? String) ?: it.hashCode().toString() }
                    .filter { doc ->
                        val targetUid = (doc["uid"] as? String) ?: (doc["id"] as? String) ?: ""
                        targetUid.isNotBlank() && targetUid != currentAuthUid
                    }

                _searchResults.value = filteredResults
            } catch (e: Exception) {
                Log.e("UserSearchViewModel", "Search error: ${e.message}", e)
                _searchError.value = "Search failed: ${e.localizedMessage}"
            } finally {
                _isSearching.value = false
            }
        }
    }
}

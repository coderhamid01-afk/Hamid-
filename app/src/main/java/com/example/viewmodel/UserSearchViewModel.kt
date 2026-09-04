package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.UserRepository
import com.example.repository.UserRepositoryImpl
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
        _searchQuery.value = query.filter { it.isDigit() }.take(6)
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _hasSearched.value = false
        _searchError.value = null
    }

    fun executeSearch() {
        val rawInput = _searchQuery.value.trim().removePrefix("@").removePrefix("#")
        val digitsOnly = rawInput.filter { it.isDigit() }

        if (digitsOnly.isBlank()) {
            _searchError.value = "Please enter numeric User ID"
            return
        }

        val formattedPxId = "PX-$digitsOnly"
        val uppercaseSearch = formattedPxId.uppercase()

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
                    .removePrefix("@")
                    .removePrefix("#")

                if (formattedPxId.equals(myPlenxoId, ignoreCase = true) ||
                    digitsOnly == myPlenxoId.removePrefix("PX-").removePrefix("px-")) {
                    _searchError.value = "You cannot add yourself"
                    _searchResults.value = emptyList()
                    _isSearching.value = false
                    return@launch
                }

                val list = mutableListOf<Map<String, Any>>()

                // Firestore query: whereEqualTo("plenxoId", searchQuery.trim().uppercase())
                var snapshot = firestore.collection("users")
                    .whereEqualTo("plenxoId", uppercaseSearch)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    snapshot = firestore.collection("users")
                        .whereEqualTo("plenxoId", formattedPxId)
                        .get()
                        .await()
                }

                if (snapshot.isEmpty) {
                    snapshot = firestore.collection("users")
                        .whereEqualTo("plenxoId", digitsOnly)
                        .get()
                        .await()
                }

                if (snapshot.isEmpty) {
                    val repoResults = userRepository.searchUsersByPlenxoId(formattedPxId)
                    repoResults.forEach { list.add(it) }
                } else {
                    snapshot.documents.forEach { doc ->
                        val data = doc.data?.toMutableMap() ?: return@forEach
                        data["docId"] = doc.id
                        data["uid"] = (data["uid"] as? String) ?: doc.id
                        list.add(data)
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

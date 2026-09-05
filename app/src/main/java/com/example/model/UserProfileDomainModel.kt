package com.example.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class UserProfileDomainModel(
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "", // Document ID compatibility
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("displayName") @set:PropertyName("displayName") var displayName: String = "",
    @get:PropertyName("bio") @set:PropertyName("bio") var bio: String = "",
    @get:PropertyName("statusMessage") @set:PropertyName("statusMessage") var statusMessage: String = "",
    @get:PropertyName("bioStatus") @set:PropertyName("bioStatus") var bioStatus: String = "",
    @get:PropertyName("profileUrl") @set:PropertyName("profileUrl") var profileUrl: String = "",
    @get:PropertyName("profilePicUrl") @set:PropertyName("profilePicUrl") var profilePicUrl: String = "",
    @get:PropertyName("userCode") @set:PropertyName("userCode") var userCode: String = "",
    @get:PropertyName("plenxoId") @set:PropertyName("plenxoId") var plenxoId: String = "",
    @get:PropertyName("selectedRingId") @set:PropertyName("selectedRingId") var selectedRingId: String = "NONE",
    @get:PropertyName("profileRingId") @set:PropertyName("profileRingId") var profileRingId: String = "none",
    @get:PropertyName("profileRing") @set:PropertyName("profileRing") var profileRing: String? = null,
    @get:PropertyName("selectedFontId") @set:PropertyName("selectedFontId") var selectedFontId: String = "DEFAULT",
    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber") var phoneNumber: String = "",
    @get:PropertyName("leagueData") @set:PropertyName("leagueData") var leagueData: LeagueData = LeagueData()
) {
    val resolvedDisplayName: String
        get() = displayName.takeIf { it.isNotBlank() && it != "User" }
            ?: name.takeIf { it.isNotBlank() && it != "User" }
            ?: displayName.ifBlank { name }.ifBlank { "User" }

    val resolvedBio: String
        get() = bio.takeIf { it.isNotBlank() }
            ?: statusMessage.takeIf { it.isNotBlank() }
            ?: bioStatus.takeIf { it.isNotBlank() }
            ?: ""
}


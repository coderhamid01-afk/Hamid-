package com.example.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Immutable
data class User(
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String = "",
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    @get:PropertyName("displayName") @set:PropertyName("displayName") var displayName: String = "User",
    @get:PropertyName("bio") @set:PropertyName("bio") var bio: String = "",
    @get:PropertyName("profilePicUrl") @set:PropertyName("profilePicUrl") var profilePicUrl: String = "",
    @get:PropertyName("themePreference") @set:PropertyName("themePreference") var themePreference: String = "Blue",
    @get:PropertyName("userCode") @set:PropertyName("userCode") var userCode: String = "",
    @get:PropertyName("plenxoId") @set:PropertyName("plenxoId") var plenxoId: String = "",
    @get:PropertyName("dob") @set:PropertyName("dob") var dob: String = "",
    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber") var phoneNumber: String = "",
    @get:PropertyName("lastLoginTimestamp") @set:PropertyName("lastLoginTimestamp") var lastLoginTimestamp: Long = 0L,
    @get:PropertyName("totalAccumulatedSeconds") @set:PropertyName("totalAccumulatedSeconds") var totalAccumulatedSeconds: Long = 0L,
    @get:PropertyName("selectedRingId") @set:PropertyName("selectedRingId") var selectedRingId: String = "",
    @get:PropertyName("profileRingId") @set:PropertyName("profileRingId") var profileRingId: String = "none",
    @get:PropertyName("profileRing") @set:PropertyName("profileRing") var profileRing: String? = null,
    @get:PropertyName("termsAccepted") @set:PropertyName("termsAccepted") var termsAccepted: Boolean = false,
    @get:PropertyName("termsAcceptedAt") @set:PropertyName("termsAcceptedAt") var termsAcceptedAt: String = "",
    @get:PropertyName("leagueData") @set:PropertyName("leagueData") var leagueData: LeagueData = LeagueData()
) {
    @PropertyName("name")
    fun setName(n: String) {
        if (displayName.isBlank() || displayName == "User") {
            displayName = n
        }
    }

    @PropertyName("statusMessage")
    fun setStatusMessage(s: String) {
        if (bio.isBlank()) {
            bio = s
        }
    }
}

@Keep
@Serializable
@Immutable
data class ChatRoom(
    val chatId: String = "",
    val participantUids: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long? = null,
    val unreadCounts: Map<String, Int> = emptyMap()
)

@Keep
@Serializable
data class Invitation(
    val invitationId: String = "",
    val requestId: String = "",
    val senderUid: String = "",
    val senderId: String = "",
    val senderUserCode: String = "",
    val senderName: String = "",
    val receiverUid: String = "",
    val receiverId: String = "",
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
)

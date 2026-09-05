package com.example.ui.search

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.repository.UserRepository
import com.example.repository.UserRepositoryImpl
import com.example.ui.components.UserActionState
import com.example.ui.components.UserListItemCard
import com.example.viewmodel.ChatRequestViewModel
import com.example.viewmodel.PlenxoViewModel
import kotlinx.coroutines.launch

import com.example.viewmodel.UserSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onBack: () -> Unit,
    userRepository: UserRepository = remember { UserRepositoryImpl() },
    chatRequestViewModel: ChatRequestViewModel = viewModel(),
    userSearchViewModel: UserSearchViewModel = viewModel(),
    plenxoViewModel: PlenxoViewModel? = null
) {
    val context = LocalContext.current

    val searchQuery by userSearchViewModel.searchQuery.collectAsState()
    val isSearching by userSearchViewModel.isSearching.collectAsState()
    val searchResults by userSearchViewModel.searchResults.collectAsState()
    val hasSearched by userSearchViewModel.hasSearched.collectAsState()
    val searchError by userSearchViewModel.searchError.collectAsState()

    var localPendingUids by remember { mutableStateOf<Set<String>>(emptySet()) }

    val sentRequests by chatRequestViewModel.sentRequests.collectAsState()
    val contactStatuses by chatRequestViewModel.contactStatuses.collectAsState()
    val toastMsg by chatRequestViewModel.toastMessage.collectAsState()

    LaunchedEffect(searchError) {
        searchError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // Sync localPendingUids with real-time server state
    LaunchedEffect(sentRequests, contactStatuses) {
        localPendingUids = localPendingUids.filter { uid ->
            val status = contactStatuses[uid]
            val isServerPending = sentRequests.any { req -> req.receiverId == uid && req.status == "PENDING" }
            status == "PENDING" || isServerPending
        }.toSet()
    }

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatRequestViewModel.clearToast()
        }
    }

    val darkBg = Color(0xFF0D1117)
    val cardBg = Color(0xFF161B22)
    val strokeBorder = Color(0xFF30363D)
    val accentBlue = Color(0xFF58A6FF)
    val textWhite = Color(0xFFF0F6FC)
    val textMuted = Color(0xFF8B949E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Search Users",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("user_search_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = accentBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        },
        containerColor = darkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // STRICT PLENXO ID SEARCH BAR WITH FIXED PX- PREFIX & DIGITS-ONLY INPUT
            val executeSearch = {
                userSearchViewModel.executeSearch()
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { input ->
                    userSearchViewModel.updateSearchQuery(input)
                },
                placeholder = {
                    Text(
                        "Search by Plenxo ID (e.g. PX-849201, 849201)",
                        color = textMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = textMuted)
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    userSearchViewModel.clearSearch()
                                },
                                modifier = Modifier.testTag("clear_search_input_button")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear input",
                                    tint = textMuted
                                )
                            }
                            IconButton(
                                onClick = { executeSearch() },
                                modifier = Modifier.testTag("execute_search_button")
                            ) {
                                Text("GO", color = accentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { executeSearch() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedBorderColor = accentBlue,
                    unfocusedBorderColor = strokeBorder,
                    focusedTextColor = textWhite,
                    unfocusedTextColor = textWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_plenxo_id_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter user's Plenxo ID (e.g. PX-123456, 123456, or @PX-123456)",
                color = textMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentBlue)
                }
            } else if (hasSearched && searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No user found with Plenxo ID '${searchQuery.trim()}'",
                        color = textMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { (it["uid"] as? String) ?: (it["id"] as? String) ?: (it["docId"] as? String) ?: it.hashCode().toString() }
                    ) { userMap ->
                        val targetUid = (userMap["uid"] as? String) ?: (userMap["id"] as? String) ?: (userMap["docId"] as? String) ?: ""
                        val name = (userMap["displayName"] as? String) ?: (userMap["fullName"] as? String) ?: "Plenxo User"
                        val rawPlenxoId = (userMap["plenxoId"] as? String) ?: (userMap["userCode"] as? String) ?: ""
                        val cleanPxId = rawPlenxoId.trim().removePrefix("@").removePrefix("#")
                        val formattedPxId = if (cleanPxId.startsWith("PX-", ignoreCase = true)) {
                            "PX-${cleanPxId.removePrefix("PX-").removePrefix("px-")}"
                        } else if (cleanPxId.isNotBlank()) {
                            "PX-$cleanPxId"
                        } else {
                            ""
                        }
                        val profilePic = (userMap["profilePicUrl"] as? String) ?: (userMap["photoUrl"] as? String) ?: ""
                        val profileRingId = (userMap["profileRingId"] as? String) ?: (userMap["selectedRingId"] as? String) ?: "none"
                        val bio = (userMap["bio"] as? String) ?: (userMap["statusMessage"] as? String) ?: ""

                        val contactStatus = contactStatuses[targetUid]
                        val isAccepted = contactStatus == "ACCEPTED"
                        val isPending = !isAccepted && (contactStatus == "PENDING" || localPendingUids.contains(targetUid) || sentRequests.any { req ->
                            req.receiverId == targetUid && req.status == "PENDING"
                        })

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_result_card_$targetUid"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, strokeBorder)
                        ) {
                            UserListItemCard(
                                displayName = name,
                                plenxoId = formattedPxId,
                                profilePicUrl = profilePic,
                                profileRingId = profileRingId,
                                bio = bio.takeIf { it.isNotBlank() },
                                actionState = when {
                                    isAccepted -> UserActionState.Chevron
                                    isPending -> UserActionState.Pending
                                    else -> UserActionState.Add(
                                        onClick = {
                                            if (targetUid.isNotBlank()) {
                                                localPendingUids = localPendingUids + targetUid
                                                chatRequestViewModel.sendChatRequest(userMap)
                                            }
                                        }
                                    )
                                },
                                onClick = {
                                    if (targetUid.isNotBlank() && plenxoViewModel != null) {
                                        plenxoViewModel.openUserProfile(targetUid)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

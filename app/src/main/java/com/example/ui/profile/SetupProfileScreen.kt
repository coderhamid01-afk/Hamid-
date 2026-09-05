package com.example.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import coil.compose.AsyncImage
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(
    authViewModel: AuthViewModel,
    onNext: () -> Unit,
    primaryColor: Color = Color(0xFF059669)
) {
    val profilePicUrl by authViewModel.profilePicUrl.collectAsState()
    val name by authViewModel.name.collectAsState()
    val bio by authViewModel.bio.collectAsState()
    val dob by authViewModel.dob.collectAsState()
    val calculatedAge by authViewModel.calculatedAge.collectAsState()
    val gender by authViewModel.gender.collectAsState()

    val isLoading by authViewModel.isProfileSetupLoading.collectAsState()
    val error by authViewModel.profileSetupError.collectAsState()
    val success by authViewModel.profileSetupSuccess.collectAsState()

    val context = LocalContext.current

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            authViewModel.profilePicUrl.value = uri.toString()
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.isProfileSetupLoading.value = false
        authViewModel.profileSetupSuccess.value = false
    }

    LaunchedEffect(success) {
        if (success) {
            onNext()
            authViewModel.profileSetupSuccess.value = false
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F17),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Identity Configuration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFF1F5F9)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.8f)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F172A),
                            Color(0xFF0B0F17),
                            Color(0xFF080C14)
                        )
                    )
                )
        ) {
            // Subtle ambient aura
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                            radius = 550f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Intro
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Identity Configuration",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "Establish your verified public display credentials and avatar for cryptographic routing.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }

                // Profile Picture Avatar Selector
                Box(
                    modifier = Modifier.padding(vertical = 6.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(114.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.5.dp, primaryColor, CircleShape)
                            .clickable { galleryLauncher.launch("image/*") }
                            .shadow(12.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePicUrl.isNotEmpty()) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Add Photo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                        }
                    }

                    // Camera Icon Overlay
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .border(2.dp, Color(0xFF0B0F17), CircleShape)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Upload Photo",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Error Card
                AnimatedVisibility(visible = error != null) {
                    error?.let {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Input Fields Glass Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Display Name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { authViewModel.name.value = it },
                            label = { Text("Display Name", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("John Doe", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("name_input"),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.4f)
                            )
                        )

                        // Personal Bio
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { authViewModel.bio.value = it },
                            label = { Text("Personal Bio", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("Hey there! Let's chat securely.", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bio_input"),
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = false,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.4f)
                            )
                        )

                        // DoB and Auto-Calculated Age
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = dob,
                                onValueChange = { authViewModel.updateDob(it) },
                                label = { Text("DoB (DD-MM-YYYY)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                placeholder = { Text("15-08-1995", color = Color(0xFF64748B)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("dob_input"),
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                                    unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.4f)
                                )
                            )

                            // Calculated Age Readout Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "CALCULATED AGE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = calculatedAge.ifEmpty { "--" },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (calculatedAge.isNotEmpty()) primaryColor else Color(0xFF64748B),
                                        modifier = Modifier.testTag("age_display")
                                    )
                                }
                            }
                        }

                        // Gender Segment Selection
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Select Gender",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Male", "Female", "Other").forEach { choice ->
                                    val isSelected = (gender == choice)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) primaryColor.copy(alpha = 0.2f) else Color(0xFF0F172A).copy(alpha = 0.6f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) primaryColor else Color(0xFF334155)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { authViewModel.gender.value = choice }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = choice,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) primaryColor else Color(0xFFCBD5E1)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = { authViewModel.completeProfileSetup() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("setup_profile_done_button")
                        .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = primaryColor.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text("Commit Identity Configuration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
